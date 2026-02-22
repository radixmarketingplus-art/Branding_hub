package com.example.rmplus;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.example.rmplus.adapters.RequestAdapter;
import com.example.rmplus.models.CustomerRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class RequestListFragment extends Fragment {

    private static final String ARG_STATUS = "status";
    private static final String ARG_ADMIN = "admin";

    String status;
    boolean isAdmin;

    // REQUIRED EMPTY CONSTRUCTOR
    public RequestListFragment() {}

    // Factory method
    public static RequestListFragment newInstance(
            String status,
            boolean isAdmin) {

        RequestListFragment f =
                new RequestListFragment();

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

        View v =
                inflater.inflate(
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
                        .getReference("customer_requests");

        ref.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(
                    @NonNull DataSnapshot snapshot) {

                ArrayList<CustomerRequest> list =
                        new ArrayList<>();

                for (DataSnapshot d : snapshot.getChildren()) {

                    CustomerRequest r =
                            d.getValue(CustomerRequest.class);

                    if (r == null) continue;

                    // User filter
                    if (!isAdmin &&
                            !r.uid.equals(
                                    FirebaseAuth
                                            .getInstance()
                                            .getUid())) {
                        continue;
                    }

                    // Status filter
                    if (r.status != null &&
                            r.status.equals(status)) {
                        list.add(r);
                    }
                }

                rv.setAdapter(
                        new RequestAdapter(
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
