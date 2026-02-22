package com.example.rmplus;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.HashMap;

public class SubscriptionActivity extends AppCompatActivity {

    TextView statusTxt;
    Spinner planSpinner;
    ImageView qrImage, proofPreview;
    Button uploadBtn, submitBtn;

    FirebaseAuth auth;
    DatabaseReference userRef, requestRef;

    Uri proofUri;
    String localProofPath="";

    String[] plans={"1 Month","3 Month","6 Month","1 Year"};

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_subscription);

        statusTxt=findViewById(R.id.statusTxt);
        planSpinner=findViewById(R.id.planSpinner);
        qrImage=findViewById(R.id.qrImage);
        proofPreview=findViewById(R.id.proofPreview);
        uploadBtn=findViewById(R.id.uploadBtn);
        submitBtn=findViewById(R.id.submitBtn);

        auth=FirebaseAuth.getInstance();

        userRef= FirebaseDatabase.getInstance()
                .getReference("users")
                .child(auth.getUid());

        requestRef= FirebaseDatabase.getInstance()
                .getReference("subscription_requests")
                .child(auth.getUid());

        planSpinner.setAdapter(
                new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_dropdown_item,
                        plans));

        int[] qrImages = {
                R.drawable.qr_1month,
                R.drawable.qr_3month,
                R.drawable.qr_6month,
                R.drawable.qr_1year
        };

        planSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        qrImage.setImageResource(qrImages[position]);
                    }

                    public void onNothingSelected(AdapterView<?> parent) {}
                });


        checkSubscriptionStatus();

        uploadBtn.setOnClickListener(v->pickImage());
        submitBtn.setOnClickListener(v->submitRequest());
    }

    // ---------------------------

    void checkSubscriptionStatus(){

        userRef.addValueEventListener(
                new ValueEventListener() {
                    public void onDataChange(DataSnapshot s){

                        Boolean sub =
                                s.child("subscribed")
                                        .getValue(Boolean.class);

                        String status =
                                s.child("subscriptionStatus")
                                        .getValue(String.class);

                        if(sub!=null && sub){

                            statusTxt.setText("Subscribed");
                            uploadBtn.setVisibility(View.GONE);
                            submitBtn.setVisibility(View.GONE);
                            planSpinner.setEnabled(false);

                        }
                        else if(status!=null &&
                                status.equals("rejected")){

                            statusTxt.setText("Rejected - Try Again");
                            uploadBtn.setEnabled(true);
                            submitBtn.setEnabled(true);
                            planSpinner.setEnabled(true);

                        }
                        else{
                            statusTxt.setText("Not Subscribed");
                        }
                    }
                    public void onCancelled(DatabaseError e){}
                });
    }

    // ---------------------------

    void pickImage(){
        Intent i=new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        startActivityForResult(i,101);
    }

    @Override
    protected void onActivityResult(int r, int c, Intent data) {
        super.onActivityResult(r, c, data);

        if (r == 101 && c == RESULT_OK && data != null) {

            proofUri = data.getData();

            if (proofUri != null) {
                proofPreview.setImageURI(proofUri);
                proofPreview.setVisibility(View.VISIBLE);  // ⭐ IMPORTANT
                localProofPath = proofUri.toString();
            }
        }
    }


    // ---------------------------

    void submitRequest(){

        if(localProofPath.isEmpty()){
            Toast.makeText(this,
                    "Upload proof",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String plan =
                planSpinner.getSelectedItem().toString();

        userRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    public void onDataChange(DataSnapshot u){

                        HashMap<String,Object> map =
                                new HashMap<>();

                        map.put("uid",auth.getUid());
                        map.put("name",
                                u.child("name")
                                        .getValue(String.class));
                        map.put("email",
                                u.child("email")
                                        .getValue(String.class));
                        map.put("mobile",
                                u.child("mobile")
                                        .getValue(String.class));
                        map.put("plan",plan);
                        map.put("proofPath",localProofPath);
                        map.put("status","pending");
                        map.put("time",
                                System.currentTimeMillis());

                        requestRef.setValue(map);

                        userRef.child("subscriptionStatus")
                                .setValue("pending");

                        NotificationHelper.send(
                                SubscriptionActivity.this,
                                auth.getUid(),
                                "Subscription Request",
                                "Your subscription request has been sent");


                        statusTxt.setText("Request Pending");

                        Toast.makeText(
                                SubscriptionActivity.this,
                                "Request Sent",
                                Toast.LENGTH_SHORT).show();
                    }
                    public void onCancelled(DatabaseError e){}
                });
    }
}
