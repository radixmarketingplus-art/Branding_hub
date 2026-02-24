package com.example.rmplus;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rmplus.models.AdvertisementRequest;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

import com.example.rmplus.AdvertisementItem;


public class AdRequestDetailActivity extends AppCompatActivity {

    TextView txtUserName, txtEmail, txtMobile;
    TextView txtTitle, txtDesc, txtStatus, txtTime;

    ImageView imgTemplate, imgProof;

    Button btnApprove, btnReject;
    Spinner spinnerDuration;
    View expiryPickerContainer;
    long expiryTime = 0;

    String requestId;
    boolean isAdmin;

    AdvertisementRequest r;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_ad_request_detail);

        txtUserName = findViewById(R.id.txtUserName);
        txtEmail = findViewById(R.id.txtEmail);
        txtMobile = findViewById(R.id.txtMobile);

        txtTitle = findViewById(R.id.txtTitle);
        txtDesc = findViewById(R.id.txtDesc);
        txtStatus = findViewById(R.id.txtStatus);
        txtTime = findViewById(R.id.txtTime);

        imgTemplate = findViewById(R.id.imgTemplate);
        imgProof = findViewById(R.id.imgProof);

        btnApprove = findViewById(R.id.btnApprove);
        btnReject = findViewById(R.id.btnReject);
        spinnerDuration = findViewById(R.id.spinnerDuration);
        expiryPickerContainer = findViewById(R.id.expiryPickerContainer);

        requestId = getIntent().getStringExtra("id");
        isAdmin = getIntent().getBooleanExtra("isAdmin", false);

        loadData();
    }

    void loadData() {

        FirebaseDatabase.getInstance()
                .getReference("advertisement_requests") // 🔥 IMPORTANT
                .child(requestId)
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {

                            @Override
                            public void onDataChange(DataSnapshot s) {

                                r = s.getValue(
                                        AdvertisementRequest.class);

                                if (r == null) return;

                                txtTitle.setText("Ad Link : " + r.adLink);
                                txtDesc.setText("Advertisement Request");
                                txtStatus.setText("Status : " + r.status);

                                String time =
                                        new SimpleDateFormat(
                                                "dd MMM yyyy, hh:mm a",
                                                Locale.getDefault())
                                                .format(new Date(r.time));

                                txtTime.setText("Time : " + time);

                                // USER INFO (Admin only)
                                if (isAdmin) {
                                    txtUserName.setText("Name : " + r.userName);
                                    txtEmail.setText("Email : " + r.email);
                                    txtMobile.setText("Mobile : " + r.mobile);
                                } else {
                                    txtUserName.setVisibility(View.GONE);
                                    txtEmail.setVisibility(View.GONE);
                                    txtMobile.setVisibility(View.GONE);
                                }

                                // TEMPLATE IMAGE
                                if (r.templatePath != null &&
                                        !r.templatePath.isEmpty()) {

                                    imgTemplate.setVisibility(View.VISIBLE);
                                    loadImageFromUrl(r.templatePath, imgTemplate);

                                    imgTemplate.setOnClickListener(v -> {
                                        Intent i = new Intent(
                                                AdRequestDetailActivity.this,
                                                ImagePreviewActivity.class);
                                        i.putExtra("img", r.templatePath);
                                        startActivity(i);
                                    });
                                }

                                // PAYMENT PROOF IMAGE
                                if (r.proofPath != null &&
                                        !r.proofPath.isEmpty()) {

                                    imgProof.setVisibility(View.VISIBLE);
                                    loadImageFromUrl(r.proofPath, imgProof);

                                    imgProof.setOnClickListener(v -> {
                                        Intent i = new Intent(
                                                AdRequestDetailActivity.this,
                                                ImagePreviewActivity.class);
                                        i.putExtra("img", r.proofPath);
                                        startActivity(i);
                                    });
                                }

                                // ADMIN ACTIONS
                                if (isAdmin &&
                                        r.status != null &&
                                        r.status.equalsIgnoreCase("pending")) {

                                    btnApprove.setVisibility(View.VISIBLE);
                                    btnReject.setVisibility(View.VISIBLE);
                                    expiryPickerContainer.setVisibility(View.VISIBLE);

                                    // Setup Spinner
                                    String[] durations = {"Select Duration", "7 Days", "1 Month", "1 Year"};
                                    android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                                            AdRequestDetailActivity.this,
                                            android.R.layout.simple_spinner_item,
                                            durations
                                    );
                                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                    spinnerDuration.setAdapter(adapter);
                                }

                                btnApprove.setOnClickListener(v -> {
                                    int pos = spinnerDuration.getSelectedItemPosition();
                                    if (pos == 0) {
                                        Toast.makeText(AdRequestDetailActivity.this, "Please select duration first", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    long now = System.currentTimeMillis();
                                    if (pos == 1) expiryTime = now + (7L * 24 * 60 * 60 * 1000);
                                    else if (pos == 2) expiryTime = now + (30L * 24 * 60 * 60 * 1000);
                                    else if (pos == 3) expiryTime = now + (365L * 24 * 60 * 60 * 1000);

                                    changeStatus("accepted");
                                });

                                btnReject.setOnClickListener(v ->
                                        changeStatus("rejected"));
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {}
                        });
    }

    private void loadImageFromUrl(String url, ImageView imageView) {

        new Thread(() -> {
            try {

                java.net.URL u = new java.net.URL(url);
                java.net.HttpURLConnection conn =
                        (java.net.HttpURLConnection) u.openConnection();

                conn.setDoInput(true);
                conn.connect();

                java.io.InputStream input = conn.getInputStream();

                android.graphics.Bitmap bitmap =
                        android.graphics.BitmapFactory.decodeStream(input);

                imageView.post(() -> imageView.setImageBitmap(bitmap));

            } catch (Exception e) {
                e.printStackTrace();

                imageView.post(() ->
                        imageView.setImageResource(
                                android.R.drawable.ic_menu_report_image
                        )
                );
            }
        }).start();
    }

//    void changeStatus(String s) {
//
//        FirebaseDatabase.getInstance()
//                .getReference("advertisement_requests") // 🔥 IMPORTANT
//                .child(requestId)
//                .child("status")
//                .setValue(s);
//
//        NotificationHelper.send(
//                this,
//                r.uid,
//                "Advertisement " + s,
//                r.adLink
//        );
//
//        Toast.makeText(this,
//                "Updated",
//                Toast.LENGTH_SHORT).show();
//
//        finish();
//    }

    void changeStatus(String s) {

        DatabaseReference ref =
                FirebaseDatabase.getInstance()
                        .getReference("advertisement_requests")
                        .child(requestId);

        ref.child("status").setValue(s);

        // ==========================================
        // ✅ IF ACCEPTED → ADD TO HOME PAGE
        // ==========================================
        if ("accepted".equals(s)) {

            saveToHomeAdvertisement();
        }

        NotificationHelper.send(
                this,
                r.uid,
                "Advertisement " + s,
                r.adLink
        );

        Toast.makeText(this,
                "Updated",
                Toast.LENGTH_SHORT).show();

        finish();
    }

    void saveToHomeAdvertisement() {

        if (r.templatePath == null || r.templatePath.isEmpty()) return;
        if (r.adLink == null || r.adLink.isEmpty()) return;

        // --- STRUCTURED FIREBASE SAVE ---
        String section = "Advertisement";
        DatabaseReference dbRef = FirebaseDatabase.getInstance()
                .getReference("templates")
                .child(section);

        String templateId = dbRef.push().getKey();
        if (templateId == null) templateId = String.valueOf(System.currentTimeMillis());

        AdvertisementItem adItem = new AdvertisementItem(
                r.templatePath,
                r.adLink,
                expiryTime,
                r.userName != null ? r.userName : "User",
                r.time
        );

        // 1. Firebase
        dbRef.child(templateId).setValue(adItem);

        // 2. SharedPreferences
        SharedPreferences sp =
                getSharedPreferences("HOME_DATA", MODE_PRIVATE);

        Gson gson = new Gson();

        Type t =
                new TypeToken<ArrayList<AdvertisementItem>>(){}.getType();

        ArrayList<AdvertisementItem> list =
                gson.fromJson(
                        sp.getString("Advertisement", "[]"),
                        t
                );

        if (list == null)
            list = new ArrayList<>();

        // Add newest first
        list.add(0, adItem);

        sp.edit()
                .putString("Advertisement",
                        gson.toJson(list))
                .apply();
    }

}
