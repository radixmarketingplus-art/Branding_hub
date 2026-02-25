package com.example.rmplus;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.Date;
import com.example.rmplus.NotificationHelper;


public class ApproveRejectActivity extends AppCompatActivity {

    TextView nameTxt,emailTxt,mobileTxt,planTxt,dateTxt;
    ImageView proofImage;
    Button approveBtn,rejectBtn,downloadBtn;

    DatabaseReference requestRef,userRef;
    String uid;
    String proofPath="";
    Spinner spinnerPlan;
    String userRequestedPlan = "";
    long currentExpiry = 0;
    Button btnUpdateExpiry;
    View planGrantContainer;
    TextView txtCurrentExpiry;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_approve_reject);

        nameTxt=findViewById(R.id.nameTxt);
        emailTxt=findViewById(R.id.emailTxt);
        mobileTxt=findViewById(R.id.mobileTxt);
        planTxt=findViewById(R.id.planTxt);
        dateTxt=findViewById(R.id.dateTxt);
        proofImage=findViewById(R.id.proofImage);
        approveBtn=findViewById(R.id.approveBtn);
        rejectBtn=findViewById(R.id.rejectBtn);
        downloadBtn=findViewById(R.id.downloadBtn);
        spinnerPlan=findViewById(R.id.spinnerPlan);
        btnUpdateExpiry=findViewById(R.id.btnUpdateExpiry); // Will add this to XML too
        planGrantContainer=findViewById(R.id.planGrantContainer); // Wrap spinner in this
        txtCurrentExpiry=findViewById(R.id.txtCurrentExpiry); // To show expiry if approved

        uid=getIntent().getStringExtra("uid");

        requestRef= FirebaseDatabase.getInstance()
                .getReference("subscription_requests")
                .child(uid);

        userRef= FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid);

        loadData();

        approveBtn.setOnClickListener(v->approve());
        rejectBtn.setOnClickListener(v->reject());
        downloadBtn.setOnClickListener(v->saveToGallery());
        
        if (btnUpdateExpiry != null) {
            btnUpdateExpiry.setOnClickListener(v -> pickNewExpiry());
        }
    }

    // ---------------------------

    void loadData(){

        requestRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    public void onDataChange(DataSnapshot s){

                        userRequestedPlan = s.child("plan").getValue(String.class);
                        planTxt.setText(getString(R.string.label_plan_requested, userRequestedPlan));

                        String status = s.child("status").getValue(String.class);
                        
                        // ✅ Canonical English keys — always saved to Firebase (locale-independent)
                        String[] plansEn = {"Silver", "Gold", "Diamond", "Custom"};

                        // Localized display names — shown in spinner UI
                        String[] plans = {
                                getString(R.string.plan_silver),
                                getString(R.string.plan_gold),
                                getString(R.string.plan_diamond),
                                getString(R.string.plan_custom)
                        };
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(ApproveRejectActivity.this, android.R.layout.simple_spinner_item, plans);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerPlan.setAdapter(adapter);

                        // Auto-select based on user request (match against English keys)
                        if (userRequestedPlan != null) {
                            for (int i = 0; i < plansEn.length; i++) {
                                if (plansEn[i].toLowerCase().contains(userRequestedPlan.toLowerCase())
                                        || userRequestedPlan.toLowerCase().contains(plansEn[i].toLowerCase())) {
                                    spinnerPlan.setSelection(i);
                                    break;
                                }
                            }
                        }

                        if ("approved".equalsIgnoreCase(status)) {
                            approveBtn.setVisibility(View.GONE);
                            rejectBtn.setVisibility(View.GONE);
                            if (planGrantContainer != null) planGrantContainer.setVisibility(View.GONE);
                            
                            // Load and show expiry
                            loadExpiryFromUser();
                        } else if ("pending".equalsIgnoreCase(status)) {
                            approveBtn.setVisibility(View.VISIBLE);
                            rejectBtn.setVisibility(View.VISIBLE);
                            if (planGrantContainer != null) planGrantContainer.setVisibility(View.VISIBLE);
                        }

                        Long t=s.child("time")
                                .getValue(Long.class);

                        if(t!=null){
                            String dateStr = new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date(t));
        dateTxt.setText(getString(R.string.label_requested_on, dateStr));
                        }

                        proofPath = s.child("proofPath")
                                .getValue(String.class);

                        if (proofPath != null && !proofPath.isEmpty()) {

                            loadImageFromUrl(proofPath, proofImage);

                            proofImage.setOnClickListener(v -> {

                                Intent i = new Intent(
                                        ApproveRejectActivity.this,
                                        ImagePreviewActivity.class
                                );

                                i.putExtra("img", proofPath);
                                startActivity(i);
                            });

                        } else {
                            proofImage.setVisibility(View.GONE);
                            downloadBtn.setEnabled(false);
                        }
                    }
                    public void onCancelled(DatabaseError e){}
                });
    }

    void loadExpiryFromUser() {
        userRef.child("subscriptionExpiry").get().addOnSuccessListener(s -> {
            if (s.exists()) {
                currentExpiry = (long) s.getValue();
                String dateStr = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.US) // ✅ Locale.US: ASCII digits for display
                        .format(new Date(currentExpiry));
                txtCurrentExpiry.setText(getString(R.string.label_expires_on, dateStr));
                txtCurrentExpiry.setVisibility(View.VISIBLE);
                btnUpdateExpiry.setVisibility(View.VISIBLE);
            }
        });
    }

    void pickNewExpiry() {
        android.icu.util.Calendar c = android.icu.util.Calendar.getInstance();
        if (currentExpiry > 0) c.setTimeInMillis(currentExpiry);

        new android.app.DatePickerDialog(this, (view, year, month, day) -> {
            android.icu.util.Calendar s = android.icu.util.Calendar.getInstance();
            s.set(year, month, day, 23, 59, 59);
            long newExpiry = s.getTimeInMillis();
            updateExpiryInDatabase(newExpiry);
        }, c.get(android.icu.util.Calendar.YEAR), c.get(android.icu.util.Calendar.MONTH), c.get(android.icu.util.Calendar.DAY_OF_MONTH)).show();
    }

    void updateExpiryInDatabase(long newExpiry) {
        userRef.child("subscriptionExpiry").setValue(newExpiry).addOnSuccessListener(aVoid -> {
            currentExpiry = newExpiry;
            loadExpiryFromUser();
            Toast.makeText(this, R.string.msg_expiry_updated, Toast.LENGTH_SHORT).show();
        });
    }

    // ---------------------------

    private void loadImageFromUrl(String url, ImageView imageView) {

        imageView.setImageResource(
                android.R.drawable.ic_menu_gallery);

        new Thread(() -> {
            try {

                java.net.URL u = new java.net.URL(url);
                java.net.HttpURLConnection conn =
                        (java.net.HttpURLConnection) u.openConnection();

                conn.setDoInput(true);
                conn.connect();

                java.io.InputStream input = conn.getInputStream();

                Bitmap bitmap =
                        BitmapFactory.decodeStream(input);

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

    void approve(){

        // ✅ Use English canonical plan keys — locale-independent
        String[] plansEn = {"Silver", "Gold", "Diamond", "Custom"};
        String grantedPlan = plansEn[spinnerPlan.getSelectedItemPosition()];

        long now = System.currentTimeMillis();
        long expiry = now + (7L * 24 * 60 * 60 * 1000); // Default 7 days

        // ✅ Compare against English canonical keys (not localized strings)
        if ("Silver".equals(grantedPlan))       expiry = now + (30L  * 24 * 60 * 60 * 1000);
        else if ("Gold".equals(grantedPlan))    expiry = now + (180L * 24 * 60 * 60 * 1000);
        else if ("Diamond".equals(grantedPlan)) expiry = now + (365L * 24 * 60 * 60 * 1000);

        requestRef.child("status").setValue("approved");
        requestRef.child("plan").setValue(grantedPlan);
        requestRef.child("expiryDate").setValue(expiry);

        userRef.child("subscribed").setValue(true);
        userRef.child("subscriptionStatus").setValue("approved");
        userRef.child("plan").setValue(grantedPlan);
        userRef.child("subscriptionExpiry").setValue(expiry);
        userRef.child("subscriptionStartDate").setValue(now);

        Toast.makeText(this,
                R.string.msg_approved,
                Toast.LENGTH_SHORT).show();

        NotificationHelper.send(
                ApproveRejectActivity.this,
                uid,
                "Subscription Approved",
                "Your plan has been approved!");

        finish();
    }

    // ---------------------------

    void reject(){

        requestRef.child("status")
                .setValue("rejected");

        userRef.child("subscribed")
                .setValue(false);

        userRef.child("subscriptionStatus")
                .setValue("rejected");

        Toast.makeText(this,
                R.string.msg_rejected,
                Toast.LENGTH_SHORT).show();

        NotificationHelper.send(
                ApproveRejectActivity.this,
                uid,
                "Subscription Rejected",
                "Your plan request was rejected.");

        finish();
    }

    // ---------------------------

    void saveToGallery(){

        if (proofPath == null || proofPath.isEmpty()) {
            Toast.makeText(this,
                    R.string.msg_no_image,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {

                java.net.URL url = new java.net.URL(proofPath);
                java.net.HttpURLConnection conn =
                        (java.net.HttpURLConnection) url.openConnection();

                conn.connect();

                Bitmap bitmap =
                        BitmapFactory.decodeStream(conn.getInputStream());

                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME,
                        "proof_" + uid + ".jpg");
                values.put(MediaStore.Images.Media.MIME_TYPE,
                        "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH,
                        "Pictures/RMPlus");

                Uri uri = getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values);

                OutputStream out =
                        getContentResolver().openOutputStream(uri);

                bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        100,
                        out
                );

                out.close();

                runOnUiThread(() ->
                        Toast.makeText(
                                this,
                                R.string.msg_saved_to_gallery,
                                Toast.LENGTH_LONG
                        ).show());

            } catch (Exception e){

                runOnUiThread(() ->
                        Toast.makeText(
                                this,
                                R.string.msg_save_failed,
                                Toast.LENGTH_SHORT
                        ).show());
            }
        }).start();
    }
}
