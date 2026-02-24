package com.example.rmplus;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rmplus.models.CustomerRequest;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RequestDetailActivity extends AppCompatActivity {

    TextView txtUserName, txtEmail, txtMobile;
    TextView txtTitle, txtType, txtDesc, txtStatus, txtTime;
    ImageView imgProof;

    Button btnApprove, btnReject, btnChat;

    String requestId;
    boolean isAdmin;

    CustomerRequest r;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_request_detail);

        txtUserName = findViewById(R.id.txtUserName);
        txtEmail = findViewById(R.id.txtEmail);
        txtMobile = findViewById(R.id.txtMobile);

        txtTitle = findViewById(R.id.txtTitle);
        txtType = findViewById(R.id.txtType);
        txtDesc = findViewById(R.id.txtDesc);
        txtStatus = findViewById(R.id.txtStatus);
        txtTime = findViewById(R.id.txtTime);

        imgProof = findViewById(R.id.imgProof);

        btnApprove = findViewById(R.id.btnApprove);
        btnReject = findViewById(R.id.btnReject);
        btnChat = findViewById(R.id.btnChat);

        requestId = getIntent().getStringExtra("id");
        isAdmin = getIntent().getBooleanExtra("isAdmin", false);

        loadData();
    }

    void loadData() {

        FirebaseDatabase.getInstance()
                .getReference("customer_requests")
                .child(requestId)
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot s) {

                                r = s.getValue(CustomerRequest.class);

                                txtTitle.setText(r.title);
                                txtType.setText("Type : " + r.type);
                                txtDesc.setText("Description : " + r.description);
                                txtStatus.setText("Status : " + r.status);

                                String time =
                                        new SimpleDateFormat(
                                                "dd MMM yyyy, hh:mm a",
                                                Locale.getDefault())
                                                .format(new Date(r.time));

                                txtTime.setText("Time : " + time);

                                if (isAdmin) {
                                    txtUserName.setText("Name : " + r.userName);
                                    txtEmail.setText("Email : " + r.email);
                                    txtMobile.setText("Mobile : " + r.mobile);
                                } else {
                                    txtUserName.setVisibility(View.GONE);
                                    txtEmail.setVisibility(View.GONE);
                                    txtMobile.setVisibility(View.GONE);
                                }

                                if (r.attachmentUrl != null &&
                                        !r.attachmentUrl.isEmpty()) {

                                    imgProof.setVisibility(View.VISIBLE);
                                    loadImageFromUrl(r.attachmentUrl, imgProof);

                                    imgProof.setOnClickListener(v -> {
                                        Intent i = new Intent(
                                                RequestDetailActivity.this,
                                                ImagePreviewActivity.class);
                                        i.putExtra("img", r.attachmentUrl);
                                        startActivity(i);
                                    });
                                }

                                if (isAdmin && r.status.equals("pending")) {
                                    btnApprove.setVisibility(View.VISIBLE);
                                    btnReject.setVisibility(View.VISIBLE);
                                }

                                if (r.status.equals("accepted")) {
                                    btnChat.setVisibility(View.VISIBLE);
                                }

                                btnApprove.setOnClickListener(v ->
                                        changeStatus("accepted"));

                                btnReject.setOnClickListener(v ->
                                        changeStatus("rejected"));

                                btnChat.setOnClickListener(v -> {
                                    Intent i = new Intent(
                                            RequestDetailActivity.this,
                                            RequestChatActivity.class);
                                    i.putExtra("id", requestId);
                                    startActivity(i);
                                });
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

                imageView.post(() ->
                        imageView.setImageBitmap(bitmap));

            } catch (Exception e) {
                e.printStackTrace();

                imageView.post(() ->
                        imageView.setImageResource(
                                android.R.drawable.ic_menu_report_image
                        ));
            }
        }).start();
    }

    void changeStatus(String s) {

        FirebaseDatabase.getInstance()
                .getReference("customer_requests")
                .child(requestId)
                .child("status")
                .setValue(s);

        NotificationHelper.send(
                this,
                r.uid,
                "Request " + s,
                r.title
        );

        Toast.makeText(this,
                "Updated",
                Toast.LENGTH_SHORT).show();

        finish();
    }
}
