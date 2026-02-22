package com.example.rmplus;

import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.FirebaseDatabase;

public class AdminDecisionActivity extends AppCompatActivity {

    Button accept,reject;
    String id;

    protected void onCreate(Bundle b){
        super.onCreate(b);
        setContentView(R.layout.activity_admin_decision);

        id=getIntent().getStringExtra("id");
        accept=findViewById(R.id.btnAccept);
        reject=findViewById(R.id.btnReject);

        accept.setOnClickListener(v->
                FirebaseDatabase.getInstance()
                        .getReference("customer_requests")
                        .child(id).child("status")
                        .setValue("accepted"));

        reject.setOnClickListener(v->
                FirebaseDatabase.getInstance()
                        .getReference("customer_requests")
                        .child(id).child("status")
                        .setValue("rejected"));
    }
}
