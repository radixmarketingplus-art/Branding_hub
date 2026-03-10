package com.example.rmplus;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import android.graphics.Paint;
import android.provider.MediaStore;
import java.io.InputStream;
import java.util.HashMap;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.rmplus.models.SubscriptionPlan;
import com.example.rmplus.adapters.UserSubscriptionPlanAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Date;

public class SubscriptionActivity extends AppCompatActivity {

    TextView statusTxt, amountTxt, originalAmountTxt, savingsDetailTxt, planTxt, expiryTxt;
    View sectionPlanChooser, sectionQrPayment;
    RecyclerView plansRecycler;
    UserSubscriptionPlanAdapter planAdapter;
    ImageView qrImage, proofPreview;
    Button uploadBtn, submitBtn;
    ProgressBar progressBar;
    View proofCard, expiryContainer;
    TextView upiIdTxt;

    FirebaseAuth auth;
    DatabaseReference userRef, requestRef;

    Uri proofUri; // Final URI

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    ArrayList<SubscriptionPlan> dynamicPlans = new ArrayList<>();
    DatabaseReference plansRef;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_subscription);

        statusTxt=findViewById(R.id.statusTxt);
        amountTxt=findViewById(R.id.amountTxt);
        originalAmountTxt=findViewById(R.id.originalAmountTxt);
        savingsDetailTxt=findViewById(R.id.savingsDetailTxt);
        plansRecycler=findViewById(R.id.plansRecycler);
        qrImage=findViewById(R.id.qrImage);
        proofPreview=findViewById(R.id.proofPreview);
        uploadBtn=findViewById(R.id.uploadBtn);
        submitBtn=findViewById(R.id.submitBtn);
        progressBar=findViewById(R.id.progressBar);
        proofCard=findViewById(R.id.proofCard);
        planTxt=findViewById(R.id.planTxt);
        expiryTxt=findViewById(R.id.expiryTxt);
        sectionPlanChooser=findViewById(R.id.sectionPlanChooser);
        sectionQrPayment=findViewById(R.id.sectionQrPayment);
        upiIdTxt = findViewById(R.id.upiIdTxt);
        expiryContainer = findViewById(R.id.expiryContainer);
        // Default initial text
        upiIdTxt.setText(getString(R.string.label_upi_id, getString(R.string.upi_id_value)));

        auth=FirebaseAuth.getInstance();

        userRef= FirebaseDatabase.getInstance()
                .getReference("users")
                .child(auth.getUid());

        requestRef= FirebaseDatabase.getInstance()
                .getReference("subscription_requests")
                .child(auth.getUid());

        plansRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        plansRef = FirebaseDatabase.getInstance()
                .getReference("dynamic_subscriptions");

        loadDynamicPlans();

        checkSubscriptionStatus();

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());

        uploadBtn.setOnClickListener(v->pickImage());
        submitBtn.setOnClickListener(v->submitRequest());

        if (proofCard != null) {
            proofCard.setOnClickListener(v -> {
                if (proofUri != null) {
                    Intent intent = new Intent(this, ImagePreviewActivity.class);
                    intent.putExtra("img", proofUri.toString());
                    startActivity(intent);
                }
            });
        }

        setupActivityResultLaunchers();
    }

    void loadDynamicPlans() {
        plansRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                dynamicPlans.clear();
                ArrayList<String> planNames = new ArrayList<>();

                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

                for (DataSnapshot d : snapshot.getChildren()) {
                    SubscriptionPlan p = d.getValue(SubscriptionPlan.class);
                    if (p != null) {
                        if (p.isSpecificDay) {
                            if (p.specificDate != null && p.specificDate.equals(today)) {
                                dynamicPlans.add(p);
                                planNames.add(p.duration);
                            }
                        } else {
                            dynamicPlans.add(p);
                            planNames.add(p.duration);
                        }
                    }
                }

                if (dynamicPlans.isEmpty()) {
                    amountTxt.setText(R.string.msg_no_active_plans);
                    qrImage.setImageResource(R.drawable.ic_gallery_modern);
                } else {
                    planAdapter = new UserSubscriptionPlanAdapter(dynamicPlans, plan -> {
                        updatePlanDetails(plan);
                    });
                    plansRecycler.setAdapter(planAdapter);

                    // Load initial details
                    updatePlanDetails(dynamicPlans.get(0));
                }
            }

            private void updatePlanDetails(SubscriptionPlan selected) {
                if (selected == null || isFinishing() || isDestroyed()) return;
                
                // Professional Amount Display
                try {
                    double amount = Double.parseDouble(selected.amount);
                    double discount = (selected.discountPrice != null && !selected.discountPrice.isEmpty()) ? Double.parseDouble(selected.discountPrice) : 0;

                    if (discount > 0) {
                        double original = amount + discount;
                        int percent = (int) ((discount / original) * 100);

                        originalAmountTxt.setText("₹" + (int)original);
                        originalAmountTxt.setPaintFlags(originalAmountTxt.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                        originalAmountTxt.setVisibility(View.VISIBLE);

                        savingsDetailTxt.setText(getString(R.string.label_you_save_percent, (int)discount, percent));
                        savingsDetailTxt.setVisibility(View.VISIBLE);

                        String detail = String.format(Locale.US, "₹%d", (int)amount);
                        amountTxt.setText(detail);
                    } else {
                        originalAmountTxt.setVisibility(View.GONE);
                        savingsDetailTxt.setVisibility(View.GONE);
                        amountTxt.setText("₹" + (int)amount);
                    }
                } catch (Exception e) {
                    amountTxt.setText("₹" + selected.amount);
                }

                // UPI ID update
                String upi = (selected.upiId != null && !selected.upiId.isEmpty()) ? selected.upiId : getString(R.string.upi_id_value);
                upiIdTxt.setText(getString(R.string.label_upi_id, upi));

                // Scanner Image Update
                android.util.Log.d("SubActivity", "Updating QR for plan: " + selected.duration + " URL: " + selected.scannerUrl);
                
                if (selected.scannerUrl != null && !selected.scannerUrl.isEmpty()) {
                    com.bumptech.glide.Glide.with(SubscriptionActivity.this)
                            .load(selected.scannerUrl)
                            .placeholder(R.drawable.ic_gallery_modern)
                            .error(R.drawable.ic_gallery_modern)
                            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(qrImage);
                } else {
                    qrImage.setImageResource(R.drawable.ic_gallery_modern);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupActivityResultLaunchers() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        handleImageResult(result.getData());
                    }
                }
        );
    }

    private void handleImageResult(Intent data) {
        proofUri = data.getData();
        if (proofUri == null) return;

        // Validate format
        String mimeType = getContentResolver().getType(proofUri);
        if (mimeType == null || (!mimeType.equals("image/jpeg") && !mimeType.equals("image/jpg") && !mimeType.equals("image/png"))) {
            Toast.makeText(this, R.string.msg_invalid_img_format, Toast.LENGTH_SHORT).show();
            proofUri = null;
            return;
        }

        // ✅ Show full preview locally
        proofPreview.setImageURI(proofUri);
        if (proofCard != null) proofCard.setVisibility(View.VISIBLE);
        else proofPreview.setVisibility(View.VISIBLE);

        Toast.makeText(this, R.string.msg_proof_selected, Toast.LENGTH_SHORT).show();
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
                            statusTxt.setText(R.string.status_subscribed);
                            uploadBtn.setVisibility(View.GONE);
                            submitBtn.setVisibility(View.GONE);
                            if (plansRecycler != null) plansRecycler.setVisibility(View.GONE);
                            if (proofCard != null) proofCard.setVisibility(View.GONE);
                            if (sectionPlanChooser != null) sectionPlanChooser.setVisibility(View.GONE);
                            if (sectionQrPayment != null) sectionQrPayment.setVisibility(View.GONE);

                            // Load active plan info
                            String activePlan = s.child("plan").getValue(String.class);
                            Long expiry = s.child("subscriptionExpiry").getValue(Long.class);

                            if (activePlan != null) {
                                String planDisplay = getLocalizedPlanName(activePlan);
                                planTxt.setText(getString(R.string.label_active_plan, planDisplay));
                                planTxt.setVisibility(View.VISIBLE);
                            }
                            if (expiry != null) {
                                String dateStr = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.US)
                                        .format(new java.util.Date(expiry));
                                expiryTxt.setText(getString(R.string.label_expires_tag, dateStr));
                                expiryContainer.setVisibility(View.VISIBLE);
                            } else {
                                expiryContainer.setVisibility(View.GONE);
                            }
                        }
                        else if(status!=null && status.equals("pending")){
                            statusTxt.setText(R.string.status_pending);
                            uploadBtn.setEnabled(false);
                            submitBtn.setEnabled(false);
                            submitBtn.setText(R.string.status_pending);
                            planTxt.setVisibility(View.GONE);
                            expiryContainer.setVisibility(View.GONE);
                            
                            // Hide QR/Plan chooser while pending to avoid confusion
                            if (sectionPlanChooser != null) sectionPlanChooser.setVisibility(View.GONE);
                            if (sectionQrPayment != null) sectionQrPayment.setVisibility(View.GONE);
                        }
                        else if(status!=null && status.equals("rejected")){
                            statusTxt.setText(R.string.status_rejected_retry);
                            uploadBtn.setEnabled(true);
                            submitBtn.setEnabled(true);
                            if (plansRecycler != null) plansRecycler.setVisibility(View.VISIBLE);
                            submitBtn.setText(R.string.btn_submit_request);
                        }
                        else{
                            statusTxt.setText(R.string.status_not_subscribed);
                            uploadBtn.setEnabled(true);
                            submitBtn.setEnabled(true);
                            if (plansRecycler != null) plansRecycler.setVisibility(View.VISIBLE);
                            submitBtn.setText(R.string.btn_submit_request);
                        }
                    }
                    public void onCancelled(DatabaseError e){}
                });
    }

    private String getLocalizedPlanName(String canonical) {
        if (canonical == null) return "";
        String c = canonical.toLowerCase();
        if (c.contains("silver")) return getString(R.string.plan_silver);
        if (c.contains("gold")) return getString(R.string.plan_gold);
        if (c.contains("diamond")) return getString(R.string.plan_diamond);
        if (c.contains("custom") || c.contains("7 days")) return getString(R.string.plan_custom);
        if (c.contains("1 month")) return getString(R.string.plan_1_month);
        if (c.contains("3 month")) return getString(R.string.plan_3_month);
        if (c.contains("6 month")) return getString(R.string.plan_6_month);
        if (c.contains("1 year")) return getString(R.string.plan_1_year);
        return canonical;
    }

    // ---------------------------

    void pickImage() {
        // Using ACTION_PICK with MediaStore for better stability (fixes PickerSyncController errors)
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(i);
    }

    @Override
    protected void onActivityResult(int r, int c, Intent data) {
        super.onActivityResult(r, c, data);
    }

    interface UploadCallback {
        void onSuccess(String url);
        void onError(String message);
    }

    private void uploadImageToServer(Uri uri, UploadCallback callback) {
        new Thread(() -> {
            try {
                String boundary = "----RMPLUS" + System.currentTimeMillis();

                java.net.URL url = new java.net.URL("http://187.77.184.84/upload.php");
                java.net.HttpURLConnection conn =
                        (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type",
                        "multipart/form-data; boundary=" + boundary);

                java.io.DataOutputStream out =
                        new java.io.DataOutputStream(conn.getOutputStream());

                out.writeBytes("--" + boundary + "\r\n");
                out.writeBytes(
                        "Content-Disposition: form-data; name=\"file\"; filename=\"proof.jpg\"\r\n");
                out.writeBytes("Content-Type: image/jpeg\r\n\r\n");

                InputStream input = getContentResolver().openInputStream(uri);
                byte[] buffer = new byte[4096];
                int len;
                while ((len = input.read(buffer)) != -1) out.write(buffer, 0, len);
                input.close();

                out.writeBytes("\r\n--" + boundary + "--\r\n");
                out.flush();
                out.close();

                int code = conn.getResponseCode();
                if (code == 200) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(conn.getInputStream()));
                    StringBuilder res = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) res.append(line);
                    reader.close();

                    org.json.JSONObject json = new org.json.JSONObject(res.toString());
                    if (json.has("url")) {
                        callback.onSuccess(json.getString("url"));
                    } else {
                        callback.onError("Server rejected: " + res);
                    }
                } else {
                    callback.onError("HTTP " + code);
                }

            } catch (Exception e) {
                e.printStackTrace();
                callback.onError(e.getMessage());
            }
        }).start();
    }


    // ---------------------------

    void submitRequest() {

        // Must have selected a proof image
        if (proofUri == null) {
            Toast.makeText(this,
                    R.string.msg_upload_proof,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (dynamicPlans.isEmpty() || planAdapter == null) {
            Toast.makeText(this, R.string.msg_plans_not_loaded, Toast.LENGTH_SHORT).show();
            return;
        }

        com.example.rmplus.models.SubscriptionPlan selectedPlan = dynamicPlans.get(planAdapter.getSelectedPosition());

        // ✅ Upload to VPS only NOW (on submit)
        setSubmitting(true);

        uploadImageToServer(proofUri, new UploadCallback() {

            @Override
            public void onSuccess(String proofUrl) {
                // Now save to Firebase with the real URL
                userRef.addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            public void onDataChange(DataSnapshot u) {

                                HashMap<String, Object> map = new HashMap<>();
                                map.put("uid",    auth.getUid());
                                map.put("name",   u.child("name").getValue(String.class));
                                map.put("email",  u.child("email").getValue(String.class));
                                map.put("mobile", u.child("mobile").getValue(String.class));
                                map.put("plan",   selectedPlan.duration);
                                map.put("amount", selectedPlan.amount);
                                map.put("discountPrice", selectedPlan.discountPrice);
                                map.put("proofPath", proofUrl);
                                map.put("status", "pending");
                                map.put("time",   System.currentTimeMillis());

                                requestRef.setValue(map)
                                        .addOnSuccessListener(v -> {
                                            userRef.child("subscriptionStatus").setValue("pending");

                                            // 📢 Notify USER
                                            NotificationHelper.send(
                                                    SubscriptionActivity.this,
                                                    auth.getUid(),
                                                    "Subscription Request Sent",
                                                    "Your subscription request has been submitted for review.");

                                            // 📢 Notify ADMINS
                                            NotificationHelper.notifyAdmins(
                                                    SubscriptionActivity.this,
                                                    "New Subscription Request",
                                                    "A user has submitted a new subscription request.",
                                                    "OPEN_SUBSCRIPTION_REQUESTS",
                                                    auth.getUid()
                                            );

                                            runOnUiThread(() -> {
                                                setSubmitting(false);
                                                statusTxt.setText(R.string.status_pending);
                                                Toast.makeText(
                                                        SubscriptionActivity.this,
                                                        R.string.msg_request_sent,
                                                        Toast.LENGTH_SHORT).show();
                                            });
                                        })
                                        .addOnFailureListener(e -> runOnUiThread(() -> {
                                            setSubmitting(false);
                                            Toast.makeText(SubscriptionActivity.this,
                                                    "Error: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        }));
                            }
                            public void onCancelled(DatabaseError e) {
                                runOnUiThread(() -> {
                                    setSubmitting(false);
                                    Toast.makeText(SubscriptionActivity.this,
                                            "DB Error: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
            }

            @Override
            public void onError(String msg) {
                runOnUiThread(() -> {
                    setSubmitting(false);
                    Toast.makeText(SubscriptionActivity.this,
                            getString(R.string.msg_upload_failed) + ": " + msg,
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setSubmitting(boolean submitting) {
        submitBtn.setEnabled(!submitting);
        submitBtn.setAlpha(submitting ? 0.6f : 1.0f);
        submitBtn.setText(submitting
                ? getString(R.string.msg_uploading_wait)
                : getString(R.string.btn_submit_request));
        if (progressBar != null)
            progressBar.setVisibility(submitting ? View.VISIBLE : View.GONE);
    }
}
