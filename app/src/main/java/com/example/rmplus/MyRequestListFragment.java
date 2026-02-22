package com.example.rmplus;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.example.rmplus.adapters.CommonRequestAdapter;
import com.example.rmplus.models.CommonRequest;
import com.example.rmplus.models.CustomerRequest;
import com.example.rmplus.models.AdvertisementRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class MyRequestListFragment extends Fragment {

    private static final String ARG_STATUS = "status";

    String status;

    public MyRequestListFragment(){}

    public static MyRequestListFragment newInstance(String status){

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
            Bundle savedInstanceState){

        View v = inflater.inflate(
                R.layout.fragment_requests,
                container,
                false);

        RecyclerView rv = v.findViewById(R.id.recycler);
        rv.setLayoutManager(
                new LinearLayoutManager(getContext()));

        status = getArguments().getString(ARG_STATUS);

        ArrayList<CommonRequest> list = new ArrayList<>();

        String uid = FirebaseAuth.getInstance().getUid();

        // ---------------- CONTACT REQUESTS ----------------

        DatabaseReference contactRef =
                FirebaseDatabase.getInstance()
                        .getReference("customer_requests");

        contactRef.addValueEventListener(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot) {

                        list.clear();

                        for (DataSnapshot d : snapshot.getChildren()) {

                            CustomerRequest r =
                                    d.getValue(CustomerRequest.class);

                            if (r == null) continue;

                            if (!r.uid.equals(uid)) continue;

                            if (r.status != null &&
                                    r.status.equals(status)) {

                                CommonRequest cr = new CommonRequest();

                                cr.requestId = r.requestId;
                                cr.uid = r.uid;
                                cr.title = r.title;
                                cr.status = r.status;
                                cr.time = r.time;
                                cr.requestType = "contact";

                                list.add(cr);
                            }
                        }

                        loadAdvertisementRequests(list, rv);
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error) {}
                });

        return v;
    }

    // ---------------- LOAD ADS ----------------

    void loadAdvertisementRequests(
            ArrayList<CommonRequest> list,
            RecyclerView rv){

        String uid = FirebaseAuth.getInstance().getUid();

        DatabaseReference adRef =
                FirebaseDatabase.getInstance()
                        .getReference("advertisement_requests");

        adRef.addValueEventListener(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot) {

                        for (DataSnapshot d : snapshot.getChildren()) {

                            AdvertisementRequest r =
                                    d.getValue(AdvertisementRequest.class);

                            if (r == null) continue;

                            if (!r.uid.equals(uid)) continue;

                            if (r.status != null &&
                                    r.status.equals(status)) {

                                CommonRequest cr = new CommonRequest();

                                cr.requestId = r.requestId;
                                cr.uid = r.uid;
                                cr.title = r.adLink;
                                cr.status = r.status;
                                cr.time = r.time;
                                cr.requestType = "advertisement";

                                list.add(cr);
                            }
                        }

                        rv.setAdapter(
                                new CommonRequestAdapter(
                                        list,
                                        getContext()
                                )
                        );
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error) {}
                });
    }
}
