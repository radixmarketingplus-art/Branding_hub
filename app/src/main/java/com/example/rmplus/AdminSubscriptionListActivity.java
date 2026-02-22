package com.example.rmplus;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class AdminSubscriptionListActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<String> userIds = new ArrayList<>();
    ArrayList<String> displayList = new ArrayList<>();
    ArrayAdapter<String> adapter;
    DatabaseReference reqRef;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_admin_subscription_list);

        listView = findViewById(R.id.listView);
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                displayList);
        listView.setAdapter(adapter);

        reqRef = FirebaseDatabase.getInstance()
                .getReference("subscription_requests");

        loadRequests();

        listView.setOnItemClickListener((p,v,pos,id)->{
            Intent i = new Intent(this,
                    ApproveRejectActivity.class);
            i.putExtra("uid", userIds.get(pos));
            startActivity(i);
        });
    }

    void loadRequests(){
        reqRef.addValueEventListener(
                new ValueEventListener() {
                    public void onDataChange(DataSnapshot s){
                        userIds.clear();
                        displayList.clear();

                        for(DataSnapshot d : s.getChildren()){
                            String plan =
                                    d.child("plan").getValue(String.class);
                            String status =
                                    d.child("status").getValue(String.class);

                            userIds.add(d.getKey());
                            displayList.add("UID: "+d.getKey()
                                    +"\nPlan: "+plan
                                    +"\nStatus: "+status);
                        }
                        adapter.notifyDataSetChanged();
                    }
                    public void onCancelled(DatabaseError e){}
                });
    }
}

