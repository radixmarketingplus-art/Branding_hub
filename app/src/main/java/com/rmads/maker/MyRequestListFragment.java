package com.rmads.maker;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rmads.maker.adapters.CommonRequestAdapter;
import com.rmads.maker.models.CommonRequest;
import com.rmads.maker.models.CustomerRequest;
import com.rmads.maker.models.AdvertisementRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class MyRequestListFragment extends Fragment {

    private static final String ARG_STATUS = "status";

    String status;
    RecyclerView rv;
    View txtEmpty;

    // ✅ Keep both lists separately to avoid race condition
    ArrayList<CommonRequest> contactList = new ArrayList<>();
    ArrayList<CommonRequest> adList = new ArrayList<>();

    public MyRequestListFragment() {}

    public static MyRequestListFragment newInstance(String status) {
        MyRequestListFragment f = new MyRequestListFragment();
        Bundle b = new Bundle();
        b.putString(ARG_STATUS, status);
        f.setArguments(b);
        return f;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_requests, container, false);

        rv = v.findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        // Empty state view (optional — only shown if fragment_requests has it)
        txtEmpty = v.findViewById(R.id.txtEmpty);

        status = getArguments() != null ? getArguments().getString(ARG_STATUS, "") : "";

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return v;

        // ──────────────────────────────────────────
        // 1. CONTACT / SUPPORT REQUESTS
        // ──────────────────────────────────────────
        DatabaseReference contactRef =
                FirebaseDatabase.getInstance().getReference("customer_requests");

        contactRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                contactList.clear(); // ✅ clear only contact list

                for (DataSnapshot d : snapshot.getChildren()) {
                    CustomerRequest r = d.getValue(CustomerRequest.class);
                    if (r == null || r.uid == null) continue;
                    if (!r.uid.equals(uid)) continue;
                    if (r.status == null || !r.status.equalsIgnoreCase(status)) continue;

                    CommonRequest cr = new CommonRequest();
                    cr.requestId = r.requestId;
                    cr.uid = r.uid;
                    cr.title = r.title;
                    cr.status = r.status;
                    cr.time = r.time;
                    cr.requestType = "contact";
                    contactList.add(cr);
                }
                refreshAdapter(); // ✅ merge & show
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // ──────────────────────────────────────────
        // 2. ADVERTISEMENT REQUESTS
        // ──────────────────────────────────────────
        DatabaseReference adRef =
                FirebaseDatabase.getInstance().getReference("advertisement_requests");

        adRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                adList.clear(); // ✅ clear only ad list

                for (DataSnapshot d : snapshot.getChildren()) {
                    AdvertisementRequest r = d.getValue(AdvertisementRequest.class);
                    if (r == null || r.uid == null) continue;
                    if (!r.uid.equals(uid)) continue;
                    if (r.status == null || !r.status.equalsIgnoreCase(status)) continue;

                    CommonRequest cr = new CommonRequest();
                    cr.requestId = r.requestId;
                    cr.uid = r.uid;
                    cr.title = r.adLink != null && !r.adLink.isEmpty()
                            ? r.adLink
                            : getString(R.string.msg_advertisement);
                    cr.status = r.status;
                    cr.time = r.time;
                    cr.requestType = "advertisement";
                    adList.add(cr);
                }
                refreshAdapter(); // ✅ merge & show
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        return v;
    }

    // ✅ Merge both lists → sort by time → set adapter
    private void refreshAdapter() {
        if (rv == null) return;

        ArrayList<CommonRequest> merged = new ArrayList<>();
        merged.addAll(contactList);
        merged.addAll(adList);

        // Sort by time descending (newest first)
        merged.sort((a, b) -> Long.compare(b.time, a.time));

        // Show/hide empty state if view exists
        if (txtEmpty != null) {
            txtEmpty.setVisibility(merged.isEmpty() ? View.VISIBLE : View.GONE);
        }

        rv.setAdapter(new CommonRequestAdapter(merged, getContext()));
    }
}
