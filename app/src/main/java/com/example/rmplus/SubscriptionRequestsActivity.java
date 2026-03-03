package com.example.rmplus;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rmplus.adapters.SubscriptionRequestAdapter;
import com.example.rmplus.models.SubscriptionRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class SubscriptionRequestsActivity extends AppCompatActivity {

    RecyclerView requestRecycler;
    TextView pendingBtn, approvedBtn, rejectedBtn;

    ArrayList<SubscriptionRequest> list = new ArrayList<>();
    SubscriptionRequestAdapter adapter;

    DatabaseReference requestRef;
    View tabUnderline;
    String currentStatus = "pending";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_subscription_requests);

        requestRecycler = findViewById(R.id.requestRecycler);
        pendingBtn = findViewById(R.id.pendingBtn);
        approvedBtn = findViewById(R.id.approvedBtn);
        rejectedBtn = findViewById(R.id.rejectedBtn);
        tabUnderline = findViewById(R.id.tabUnderline);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        requestRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SubscriptionRequestAdapter(list, this);
        requestRecycler.setAdapter(adapter);

        // default: Pending selected
        pendingBtn.post(() -> {
            moveUnderline(pendingBtn);
            updateTabText(pendingBtn);
        });

        requestRef = FirebaseDatabase.getInstance()
                .getReference("subscription_requests");

        loadRequests();

        pendingBtn.setOnClickListener(v -> {
            currentStatus = "pending";
            moveUnderline(pendingBtn);
            updateTabText(pendingBtn);
            loadRequests();
        });

        approvedBtn.setOnClickListener(v -> {
            currentStatus = "approved";
            moveUnderline(approvedBtn);
            updateTabText(approvedBtn);
            loadRequests();
        });

        rejectedBtn.setOnClickListener(v -> {
            currentStatus = "rejected";
            moveUnderline(rejectedBtn);
            updateTabText(rejectedBtn);
            loadRequests();
        });
    }

    void loadRequests() {

        requestRef.addValueEventListener(
                new ValueEventListener() {
                    public void onDataChange(DataSnapshot snap) {

                        list.clear();

                        for (DataSnapshot d : snap.getChildren()) {

                            String status = d.child("status").getValue(String.class);

                            if (status != null && status.equalsIgnoreCase(currentStatus)) {

                                SubscriptionRequest r = d.getValue(SubscriptionRequest.class);
                                if (r != null) {
                                    r.uid = d.getKey();
                                    list.add(r);
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }

                    public void onCancelled(DatabaseError e) {
                    }
                });
    }

    void moveUnderline(View tab) {
        tab.post(() -> {
            int left = tab.getLeft();
            int width = tab.getWidth();

            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) tabUnderline.getLayoutParams();

            params.width = width;
            params.leftMargin = left;

            tabUnderline.setLayoutParams(params);
        });
    }

    void updateTabText(TextView active) {

        int inactive =
                getColorFromAttr(
                        com.google.android.material.R.attr.colorOnSurface);

        pendingBtn.setTextColor(inactive);
        approvedBtn.setTextColor(inactive);
        rejectedBtn.setTextColor(inactive);

        active.setTextColor(
                android.graphics.Color.parseColor("#4A6CF7"));
    }

    private int getColorFromAttr(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

}
