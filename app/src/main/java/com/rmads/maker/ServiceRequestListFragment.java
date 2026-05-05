package com.rmads.maker;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.rmads.maker.models.ClientInfoRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class ServiceRequestListFragment extends Fragment {

    private static final String ARG_STATUS = "status";
    private static final String ARG_ADMIN = "admin";

    String status;
    boolean isAdmin;

    public ServiceRequestListFragment() {}

    public static ServiceRequestListFragment newInstance(
            String status, boolean isAdmin) {

        ServiceRequestListFragment f =
                new ServiceRequestListFragment();

        Bundle b = new Bundle();
        b.putString(ARG_STATUS, status);
        b.putBoolean(ARG_ADMIN, isAdmin);
        f.setArguments(b);

        return f;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        View v = inflater.inflate(
                R.layout.fragment_requests,
                container,
                false);

        RecyclerView rv = v.findViewById(R.id.recycler);
        rv.setLayoutManager(
                new LinearLayoutManager(getContext()));

        status = getArguments().getString(ARG_STATUS);
        isAdmin = getArguments().getBoolean(ARG_ADMIN);

        DatabaseReference ref =
                FirebaseDatabase.getInstance()
                        .getReference("client_info_requests");

        ref.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(
                    @NonNull DataSnapshot snapshot) {

                ArrayList<ClientInfoRequest> list =
                        new ArrayList<>();

                String currentUid =
                        FirebaseAuth.getInstance().getUid();

                for (DataSnapshot d : snapshot.getChildren()) {

                    ClientInfoRequest r =
                            d.getValue(ClientInfoRequest.class);

                    if (r == null) continue;

                    // USER FILTER
                    if (!isAdmin) {
                        if (currentUid == null ||
                                r.uid == null ||
                                !r.uid.equals(currentUid)) {
                            continue;
                        }
                    }

                    // STATUS FILTER
                    if (r.status != null &&
                            r.status.equalsIgnoreCase(status)) {

                        list.add(r);
                    }
                }

                // Sort: newest first
                list.sort((a, b) -> Long.compare(b.time, a.time));

                View emptyView = v.findViewById(R.id.txtEmpty);
                if (emptyView != null) {
                    emptyView.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                }

                rv.setAdapter(
                        new AdminClientInfoAdapter(
                                getContext(),
                                list,
                                isAdmin
                        )
                );
            }

            @Override
            public void onCancelled(
                    @NonNull DatabaseError error) {}
        });

        return v;
    }
}
