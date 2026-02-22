package com.example.rmplus;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.*;

import java.util.ArrayList;
import android.widget.TextView;

public class SubscriptionRequestsActivity extends AppCompatActivity {

    ListView requestList;
    TextView pendingBtn, approvedBtn, rejectedBtn;

    ArrayList<String> displayList = new ArrayList<>();
    ArrayList<String> uidList = new ArrayList<>();

    DatabaseReference requestRef;
    View tabUnderline;
    String currentStatus = "pending";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_subscription_requests);

        requestList = findViewById(R.id.requestList);
        pendingBtn = findViewById(R.id.pendingBtn);
        approvedBtn = findViewById(R.id.approvedBtn);
        rejectedBtn = findViewById(R.id.rejectedBtn);

        tabUnderline = findViewById(R.id.tabUnderline);

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

        requestList.setOnItemClickListener((a,v,pos,id)->{
            Intent i = new Intent(this,
                    ApproveRejectActivity.class);
            i.putExtra("uid", uidList.get(pos));
            startActivity(i);
        });
    }

    void loadRequests(){

        requestRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    public void onDataChange(DataSnapshot snap){

                        displayList.clear();
                        uidList.clear();

                        for(DataSnapshot d : snap.getChildren()){

                            String status =
                                    d.child("status")
                                            .getValue(String.class);

                            if(status != null &&
                                    status.equals(currentStatus)){

                                String name =
                                        d.child("name")
                                                .getValue(String.class);

                                String plan =
                                        d.child("plan")
                                                .getValue(String.class);

                                displayList.add(
                                        name + " - " + plan);

                                uidList.add(d.getKey());
                            }
                        }

                        requestList.setAdapter(
                                new ArrayAdapter<>(
                                        SubscriptionRequestsActivity.this,
                                        android.R.layout.simple_list_item_1,
                                        displayList));
                    }

                    public void onCancelled(DatabaseError e){}
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
