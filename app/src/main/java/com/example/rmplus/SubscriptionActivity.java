package com.example.rmplus;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.io.InputStream;
import java.util.HashMap;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class SubscriptionActivity extends AppCompatActivity {

    TextView statusTxt, amountTxt;
    Spinner planSpinner;
    ImageView qrImage, proofPreview;
    Button uploadBtn, submitBtn;
    ProgressBar progressBar;

    FirebaseAuth auth;
    DatabaseReference userRef, requestRef;

    Uri proofUri;           // kept locally — uploaded only on submit
    // proofUrl is no longer stored as a field; upload happens at submit time

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    String[] plans;          // localized display names (shown in spinner)
    String[] plansEn;         // canonical English keys (saved to Firebase)
    String[] prices={"₹199","₹499","₹899","₹1499"};

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_subscription);

        statusTxt=findViewById(R.id.statusTxt);
        amountTxt=findViewById(R.id.amountTxt);
        planSpinner=findViewById(R.id.planSpinner);
        qrImage=findViewById(R.id.qrImage);
        proofPreview=findViewById(R.id.proofPreview);
        uploadBtn=findViewById(R.id.uploadBtn);
        submitBtn=findViewById(R.id.submitBtn);
        progressBar=findViewById(R.id.progressBar);
        TextView upiIdTxt = findViewById(R.id.upiIdTxt);
        upiIdTxt.setText(getString(R.string.label_upi_id, getString(R.string.upi_id_value)));

        auth=FirebaseAuth.getInstance();

        userRef= FirebaseDatabase.getInstance()
                .getReference("users")
                .child(auth.getUid());

        requestRef= FirebaseDatabase.getInstance()
                .getReference("subscription_requests")
                .child(auth.getUid());

        // ✅ Canonical English keys — always saved to Firebase
        plansEn = new String[]{"1 Month", "3 Months", "6 Months", "1 Year"};

        // Localized display names — shown in UI only
        plans = new String[]{
                getString(R.string.plan_1_month),
                getString(R.string.plan_3_month),
                getString(R.string.plan_6_month),
                getString(R.string.plan_1_year)
        };

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
                        amountTxt.setText(getString(R.string.label_amount, prices[position]));
                    }

                    public void onNothingSelected(AdapterView<?> parent) {}
                });


        checkSubscriptionStatus();

        uploadBtn.setOnClickListener(v->pickImage());
        submitBtn.setOnClickListener(v->submitRequest());

        setupActivityResultLaunchers();
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
        // ✅ Only show preview locally — NO VPS upload here
        proofPreview.setImageURI(proofUri);
        proofPreview.setVisibility(View.VISIBLE);
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
                            planSpinner.setEnabled(false);

                        }
                        else if(status!=null &&
                                status.equals("rejected")){

                            statusTxt.setText(R.string.status_rejected_retry);
                            uploadBtn.setEnabled(true);
                            submitBtn.setEnabled(true);
                            planSpinner.setEnabled(true);

                        }
                        else{
                            statusTxt.setText(R.string.status_not_subscribed);
                        }
                    }
                    public void onCancelled(DatabaseError e){}
                });
    }

    // ---------------------------

    void pickImage(){
        Intent i=new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        String[] mimeTypes = {"image/jpeg", "image/jpg", "image/png"};
        i.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
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

        // ✅ Always save English canonical plan name (not localized display text)
        String plan = plansEn[planSpinner.getSelectedItemPosition()];

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
                                map.put("plan",   plan);
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
