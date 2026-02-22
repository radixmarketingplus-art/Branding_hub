package com.example.rmplus;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.example.rmplus.adapters.AdRequestAdapter;
import com.example.rmplus.models.AdvertisementRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class AdminAdRequestListFragment extends Fragment {

    private static final String ARG_STATUS = "status";
    private static final String ARG_ADMIN = "admin";

    String status;
    boolean isAdmin;

    public AdminAdRequestListFragment() {}

    public static AdminAdRequestListFragment newInstance(
            String status, boolean isAdmin) {

        AdminAdRequestListFragment f =
                new AdminAdRequestListFragment();

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

        // 🔥 IMPORTANT CHANGE HERE
        DatabaseReference ref =
                FirebaseDatabase.getInstance()
                        .getReference("advertisement_requests");

        ref.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(
                    @NonNull DataSnapshot snapshot) {

                ArrayList<AdvertisementRequest> list =
                        new ArrayList<>();

                for (DataSnapshot d : snapshot.getChildren()) {

                    AdvertisementRequest r =
                            d.getValue(
                                    AdvertisementRequest.class);

                    if (r == null) continue;

                    // USER FILTER
                    if (!isAdmin &&
                            !r.uid.equals(
                                    FirebaseAuth
                                            .getInstance()
                                            .getUid())) {
                        continue;
                    }

                    // STATUS FILTER
                    if (r.status != null &&
                            r.status.equals(status)) {
                        list.add(r);
                    }
                }

                rv.setAdapter(
                        new AdRequestAdapter(
                                list,
                                isAdmin,
                                getContext()
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
