package com.example.rmplus;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.rmplus.models.AdvertisementRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.yalantis.ucrop.UCrop;

import java.io.*;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

public class AdRequestActivity extends AppCompatActivity {

    EditText etLink;
    ImageView imgTemplate, imgProof;
    Button btnUploadTemplate, btnUploadProof, btnSubmit;
    TextView tvReCropHint;
    ProgressBar progressBar;   // Loading indicator during submit

    // ── Template (advertisement image) — crop → kept locally until submit ──
    Uri templateUri;            // cropped local URI (not yet on VPS)
    Uri templateOriginalUri;    // original picked URI (for re-crop)

    // ── Proof (payment screenshot) — kept locally until submit ──
    Uri proofUri;               // local URI (not yet on VPS)

    DatabaseReference usersRef, adRef;

    // ─── Image Picker for TEMPLATE ─────────────────────────────────────────────
    ActivityResultLauncher<Intent> templatePicker =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri sourceUri = result.getData().getData();
                            if (sourceUri == null) return;

                            String mimeType = getContentResolver().getType(sourceUri);
                            if (!isValidImage(mimeType)) {
                                toast(R.string.msg_invalid_img_format);
                                return;
                            }
                            templateOriginalUri = sourceUri;
                            startCropForTemplate(sourceUri);
                        }
                    });

    // ─── UCrop result for TEMPLATE ─────────────────────────────────────────────
    ActivityResultLauncher<Intent> cropLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri croppedUri = UCrop.getOutput(result.getData());
                            if (croppedUri != null) {
                                // ✅ Only keep locally — NO VPS upload here
                                templateUri = croppedUri;
                                imgTemplate.setImageURI(croppedUri);
                                imgTemplate.setVisibility(ImageView.VISIBLE);
                                tvReCropHint.setVisibility(View.VISIBLE);
                                toast(R.string.msg_crop_success);
                            }
                        } else if (result.getResultCode() == UCrop.RESULT_ERROR && result.getData() != null) {
                            Throwable err = UCrop.getError(result.getData());
                            if (err != null) toast("Crop Error: " + err.getMessage());
                        }
                    });

    // ─── Image Picker for PROOF ────────────────────────────────────────────────
    ActivityResultLauncher<Intent> proofPicker =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri sourceUri = result.getData().getData();
                            if (sourceUri == null) return;

                            String mimeType = getContentResolver().getType(sourceUri);
                            if (!isValidImage(mimeType)) {
                                toast(R.string.msg_invalid_img_format);
                                return;
                            }
                            // ✅ Only keep locally — NO VPS upload here
                            proofUri = sourceUri;
                            imgProof.setImageURI(sourceUri);
                            imgProof.setVisibility(ImageView.VISIBLE);
                        }
                    });

    // ─── onCreate ──────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_ad_request);

        etLink            = findViewById(R.id.etLink);
        tvReCropHint      = findViewById(R.id.tvReCropHint);
        imgTemplate       = findViewById(R.id.imgTemplate);
        imgProof          = findViewById(R.id.imgProof);
        btnUploadTemplate = findViewById(R.id.btnUploadTemplate);
        btnUploadProof    = findViewById(R.id.btnUploadProof);
        btnSubmit         = findViewById(R.id.btnSubmitAd);
        progressBar       = findViewById(R.id.progressBar);

        String uid = FirebaseAuth.getInstance().getUid();

        usersRef = FirebaseDatabase.getInstance()
                .getReference("users").child(uid);

        adRef = FirebaseDatabase.getInstance()
                .getReference("advertisement_requests");

        btnUploadTemplate.setOnClickListener(v -> pickTemplateImage());

        // Tap preview → re-crop from original
        imgTemplate.setOnClickListener(v -> {
            if (templateOriginalUri != null) {
                startCropForTemplate(templateOriginalUri);
            }
        });

        btnUploadProof.setOnClickListener(v -> pickProofImage());

        // ✅ Submit: validate → upload both → save to Firebase
        btnSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    // ─── Pick Images ───────────────────────────────────────────────────────────

    private void pickTemplateImage() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/jpeg", "image/jpg", "image/png"});
        templatePicker.launch(i);
    }

    private void pickProofImage() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/jpeg", "image/jpg", "image/png"});
        proofPicker.launch(i);
    }

    // ─── UCrop ─────────────────────────────────────────────────────────────────

    private void startCropForTemplate(@NonNull Uri uri) {
        String destName = "cropped_ad_" + System.currentTimeMillis() + ".jpg";
        UCrop uCrop = UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), destName)));
        uCrop.withAspectRatio(16, 9);   // Advertisement: always 16:9

        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(android.graphics.Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(90);
        options.setHideBottomControls(false);
        options.setToolbarTitle(getString(R.string.title_crop_image));
        options.setToolbarColor(Color.parseColor("#1B1B1B"));
        options.setStatusBarColor(Color.parseColor("#1B1B1B"));
        options.setToolbarWidgetColor(Color.WHITE);
        options.setActiveControlsWidgetColor(Color.parseColor("#4A6CF7"));
        options.setLogoColor(Color.TRANSPARENT);
        options.setDimmedLayerColor(Color.parseColor("#CC000000"));
        options.setCropFrameStrokeWidth(12);
        options.setCropGridStrokeWidth(2);
        options.setShowCropGrid(true);
        options.setFreeStyleCropEnabled(true);
        options.setRootViewBackgroundColor(Color.BLACK);

        uCrop.withOptions(options);
        cropLauncher.launch(uCrop.getIntent(this));
    }

    // ─── Validate Then Upload Both Images on Submit ─────────────────────────────

    private void validateAndSubmit() {

        String link = etLink.getText().toString().trim();

        if (link.isEmpty()) {
            etLink.setError(getString(R.string.hint_adv_link));
            return;
        }

        if (templateUri == null) {
            toast(R.string.msg_please_select_img);
            return;
        }

        // Disable UI & show progress
        setSubmitting(true);

        // ── Upload template first, then proof, then save to Firebase ──────────
        uploadImageToServer(templateUri, new UploadCallback() {

            @Override
            public void onSuccess(String templateUrl) {

                // Proof is optional — if not selected, use empty string
                if (proofUri == null) {
                    saveToFirebase(link, templateUrl, "");
                    return;
                }

                uploadImageToServer(proofUri, new UploadCallback() {

                    @Override
                    public void onSuccess(String proofUrl) {
                        saveToFirebase(link, templateUrl, proofUrl);
                    }

                    @Override
                    public void onError(String msg) {
                        runOnUiThread(() -> {
                            setSubmitting(false);
                            toast(getString(R.string.msg_upload_failed) + ": " + msg);
                        });
                    }
                });
            }

            @Override
            public void onError(String msg) {
                runOnUiThread(() -> {
                    setSubmitting(false);
                    toast(getString(R.string.msg_upload_failed) + ": " + msg);
                });
            }
        });
    }

    // ─── Save to Firebase ───────────────────────────────────────────────────────

    private void saveToFirebase(String adLink, String templateUrl, String proofUrl) {

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot s) {

                String name   = s.child("name").getValue(String.class);
                String email  = s.child("email").getValue(String.class);
                String mobile = s.child("mobile").getValue(String.class);

                String id = adRef.push().getKey();

                AdvertisementRequest r = new AdvertisementRequest();
                r.requestId    = id;
                r.uid          = FirebaseAuth.getInstance().getUid();
                r.userName     = name;
                r.email        = email;
                r.mobile       = mobile;
                r.adLink       = adLink;
                r.templatePath = templateUrl;
                r.proofPath    = proofUrl;
                r.status       = "pending";
                r.time         = System.currentTimeMillis();

                adRef.child(id).setValue(r)
                        .addOnSuccessListener(u -> {

                            // 📢 Notify USER
                            NotificationHelper.send(
                                    AdRequestActivity.this,
                                    FirebaseAuth.getInstance().getUid(),
                                    "Advertisement Request Sent",
                                    "Your advertisement request has been submitted for review."
                            );

                            // 📢 Notify ADMINS
                            NotificationHelper.notifyAdmins(
                                    AdRequestActivity.this,
                                    "New Advertisement Request",
                                    "A user has submitted a new advertisement request.",
                                    "OPEN_AD_REQUESTS", // Action to handle in NotificationActivity
                                    id
                            );

                            runOnUiThread(() -> {
                                setSubmitting(false);
                                toast(R.string.msg_adv_request_sent);
                                finish();
                            });
                        })
                        .addOnFailureListener(e -> {
                            runOnUiThread(() -> {
                                setSubmitting(false);
                                toast("Firebase error: " + e.getMessage());
                            });
                        });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                runOnUiThread(() -> {
                    setSubmitting(false);
                    toast("Database error: " + error.getMessage());
                });
            }
        });
    }

    // ─── UI State ──────────────────────────────────────────────────────────────

    private void setSubmitting(boolean submitting) {
        btnSubmit.setEnabled(!submitting);
        btnSubmit.setAlpha(submitting ? 0.6f : 1.0f);
        btnSubmit.setText(submitting
                ? getString(R.string.msg_uploading_wait)
                : getString(R.string.btn_submit_adv_request));
        progressBar.setVisibility(submitting ? View.VISIBLE : View.GONE);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private boolean isValidImage(String mimeType) {
        return mimeType != null &&
                (mimeType.equals("image/jpeg") ||
                 mimeType.equals("image/jpg") ||
                 mimeType.equals("image/png"));
    }

    void toast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // ─── Upload to VPS Server ──────────────────────────────────────────────────

    interface UploadCallback {
        void onSuccess(String url);
        void onError(String message);
    }

    private void uploadImageToServer(Uri uri, UploadCallback callback) {
        new Thread(() -> {
            try {
                String boundary = "----RMPLUS" + System.currentTimeMillis();
                String mimeType = getContentResolver().getType(uri);
                if (mimeType == null) mimeType = "image/jpeg";

                java.net.URL url = new java.net.URL("http://187.77.184.84/upload.php");
                java.net.HttpURLConnection conn =
                        (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Content-Type",
                        "multipart/form-data; boundary=" + boundary);

                // Determine extension
                String ext = ".jpg";
                if (mimeType.contains("png")) ext = ".png";
                String fileName = "ad_" + System.currentTimeMillis() + ext;

                java.io.DataOutputStream out =
                        new java.io.DataOutputStream(conn.getOutputStream());

                out.writeBytes("--" + boundary + "\r\n");
                out.writeBytes("Content-Disposition: form-data; name=\"file\";" +
                        " filename=\"" + fileName + "\"\r\n");
                out.writeBytes("Content-Type: " + mimeType + "\r\n\r\n");

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
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
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
}
