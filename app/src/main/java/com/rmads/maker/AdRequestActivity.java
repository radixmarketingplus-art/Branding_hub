package com.rmads.maker;

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

import com.rmads.maker.models.AdvertisementRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.yalantis.ucrop.UCrop;

import java.io.*;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

public class AdRequestActivity extends BaseActivity {

    EditText etLink;
    ImageView imgTemplate, imgProof;
    Button btnUploadTemplate, btnUploadProof, btnSubmit;
    TextView tvReCropHint;
    ProgressBar progressBar; // Shows actual upload % for videos
    TextView tvUploadPercent;  // "Uploading… 45%"
    View uploadProgressContainer; // Wraps bar + percent label

    // ── Template (advertisement image) — crop → kept locally until submit ──
    Uri templateUri; // cropped local URI (not yet on VPS)
    Uri templateOriginalUri; // original picked URI (for re-crop)

    // ── Proof (payment screenshot) — kept locally until submit ──
    Uri proofUri; // local URI (not yet on VPS)

    DatabaseReference usersRef, adRef;

    // ─── Image Picker for TEMPLATE ─────────────────────────────────────────────
    ActivityResultLauncher<Intent> templatePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri sourceUri = result.getData().getData();
                    if (sourceUri == null)
                        return;

                    String mimeType = getContentResolver().getType(sourceUri);
                    String uriStr = sourceUri.toString().toLowerCase();
                    boolean isVideo = (mimeType != null && mimeType.startsWith("video/")) || 
                                      uriStr.contains(".mp4") || uriStr.contains(".mkv") || uriStr.contains(".webm") || uriStr.contains(".mov") || uriStr.contains(".3gp");

                    if (isVideo) {
                        // 🎬 Skip crop for videos
                        templateOriginalUri = sourceUri;
                        templateUri = sourceUri; // local URI
                        showTemplatePreview(true);
                        toast(R.string.msg_video_selected);
                    } else {
                        // 🖼️ Proceed with crop for images
                        templateOriginalUri = sourceUri;
                        startCropForTemplate(sourceUri);
                    }
                }
            });

    // ─── UCrop result for TEMPLATE ─────────────────────────────────────────────
    ActivityResultLauncher<Intent> cropLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri croppedUri = UCrop.getOutput(result.getData());
                    if (croppedUri != null) {
                        // ✅ Only keep locally — NO VPS upload here
                        templateUri = croppedUri;
                        showTemplatePreview(false);
                        toast(R.string.msg_crop_success);
                    }
                } else if (result.getResultCode() == UCrop.RESULT_ERROR && result.getData() != null) {
                    Throwable err = UCrop.getError(result.getData());
                    if (err != null)
                        toast("Crop Error: " + err.getMessage());
                }
            });

    ImageView icPlayVideo;

    private void showTemplatePreview(boolean isVideo) {
        if (templateUri == null) return;
        
        imgTemplate.setVisibility(View.VISIBLE);
        View tp = findViewById(R.id.templatePlaceholder);
        if (tp != null) tp.setVisibility(View.GONE);
        tvReCropHint.setVisibility(isVideo ? View.GONE : View.VISIBLE);

        if (isVideo) {
            icPlayVideo.setVisibility(View.VISIBLE);
            // Load thumbnail frame
            com.bumptech.glide.Glide.with(this)
                    .asBitmap()
                    .load(templateUri)
                    .frame(1000000)
                    .into(imgTemplate);
            
            // Clicking play icon or thumbnail opens full video preview
            View.OnClickListener playListener = v -> {
                Intent i = new Intent(this, ImagePreviewActivity.class);
                i.putExtra("img", templateUri.toString());
                i.putExtra("is_video", true);
                startActivity(i);
            };
            imgTemplate.setOnClickListener(playListener);
            icPlayVideo.setOnClickListener(playListener);
        } else {
            icPlayVideo.setVisibility(View.GONE);
            imgTemplate.setImageURI(templateUri);
            
            // Restore re-crop listener for images
            imgTemplate.setOnClickListener(v -> {
                if (templateOriginalUri != null) {
                    startCropForTemplate(templateOriginalUri);
                }
            });
        }
    }

    // ─── Image Picker for PROOF ────────────────────────────────────────────────
    ActivityResultLauncher<Intent> proofPicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri sourceUri = result.getData().getData();
                    if (sourceUri == null)
                        return;

                    String mimeType = getContentResolver().getType(sourceUri);
                    if (!isValidImage(mimeType)) {
                        toast(R.string.msg_invalid_img_format);
                        return;
                    }
                    // ✅ Only keep locally — NO VPS upload here
                    proofUri = sourceUri;
                    imgProof.setImageURI(sourceUri);
                    imgProof.setVisibility(ImageView.VISIBLE);
                    View pp = findViewById(R.id.proofPlaceholder);
                    if (pp != null)
                        pp.setVisibility(View.GONE);
                }
            });

    // ─── onCreate ──────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_ad_request);

        // This initializes the bottom nav from the layout
        setupBase("user", R.id.ad_request);

        etLink = findViewById(R.id.etLink);
        tvReCropHint = findViewById(R.id.tvReCropHint);
        imgTemplate = findViewById(R.id.imgTemplate);
        imgProof = findViewById(R.id.imgProof);
        icPlayVideo = findViewById(R.id.icPlayVideo);
        btnUploadTemplate = findViewById(R.id.btnUploadTemplate);
        btnUploadProof = findViewById(R.id.btnUploadProof);
        btnSubmit = findViewById(R.id.btnSubmitAd);
        progressBar = findViewById(R.id.progressBar);
        tvUploadPercent = findViewById(R.id.tvUploadPercent);
        uploadProgressContainer = findViewById(R.id.uploadProgressContainer);

        // Back button
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null)
            btnBack.setOnClickListener(v -> finish());

        String uid = FirebaseAuth.getInstance().getUid();

        usersRef = FirebaseDatabase.getInstance()
                .getReference("users").child(uid);

        adRef = FirebaseDatabase.getInstance()
                .getReference("advertisement_requests");

        btnUploadTemplate.setOnClickListener(v -> pickTemplateImage());

        // Note: Template preview click is now handled inside showTemplatePreview()
        // based on image vs video type.

        btnUploadProof.setOnClickListener(v -> pickProofImage());

        // ✅ Proof Preview
        imgProof.setOnClickListener(v -> {
            if (proofUri != null) {
                Intent i = new Intent(AdRequestActivity.this, ImagePreviewActivity.class);
                i.putExtra("img", proofUri.toString());
                startActivity(i);
            }
        });

        // ✅ Submit: validate → upload both → save to Firebase
        btnSubmit.setOnClickListener(v -> validateAndSubmit());

        loadAdScanner();
    }

    private void loadAdScanner() {
        ImageView imgAdScanner = findViewById(R.id.imgAdScanner);
        if (imgAdScanner == null) return;

        FirebaseDatabase.getInstance().getReference("admin_settings").child("ad_request_scanner")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        try {
                            if (!isFinishing() && !isDestroyed() && snapshot.exists() && snapshot.hasChild("imageUrl")) {
                                String imgUrl = snapshot.child("imageUrl").getValue(String.class);
                                if (imgUrl != null && !imgUrl.isEmpty()) {
                                    com.bumptech.glide.Glide.with(AdRequestActivity.this)
                                            .load(imgUrl)
                                            .into(imgAdScanner);
                                }
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // ─── Pick Images ───────────────────────────────────────────────────────────

    private void pickTemplateImage() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        String[] mimeTypes = { "image/*", "video/*" };
        i.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        templatePicker.launch(i);
    }

    private void pickProofImage() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[] { "image/jpeg", "image/jpg", "image/png" });
        proofPicker.launch(i);
    }

    // ─── UCrop ─────────────────────────────────────────────────────────────────

    private void startCropForTemplate(@NonNull Uri uri) {
        String destName = "cropped_ad_" + System.currentTimeMillis() + ".jpg";
        UCrop uCrop = UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), destName)));
        uCrop.withAspectRatio(380, 160); // Advertisement: now wider (approx 2.37:1)

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
        // Link is now optional

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

                String name = s.child("name").getValue(String.class);
                String email = s.child("email").getValue(String.class);
                String mobile = s.child("mobile").getValue(String.class);

                String id = adRef.push().getKey();

                AdvertisementRequest r = new AdvertisementRequest();
                r.requestId = id;
                r.uid = FirebaseAuth.getInstance().getUid();
                r.userName = name;
                r.email = email;
                r.mobile = mobile;
                r.adLink = adLink;
                r.templatePath = templateUrl;
                r.proofPath = proofUrl;
                r.status = "pending";
                r.time = System.currentTimeMillis();
                
                String mime = getContentResolver().getType(templateUri);
                String lowerUri = templateUri.toString().toLowerCase();
                boolean isVid = (mime != null && mime.startsWith("video/")) || 
                                lowerUri.contains(".mp4") || lowerUri.contains(".mov") || 
                                lowerUri.contains(".mkv") || lowerUri.contains(".webm") || 
                                lowerUri.contains(".3gp");
                r.type = isVid ? "video" : "image";
                
                adRef.child(id).setValue(r)
                        .addOnSuccessListener(u -> {

                            // 📢 Notify USER
                            NotificationHelper.send(
                                    AdRequestActivity.this,
                                    FirebaseAuth.getInstance().getUid(),
                                    "Advertisement Request Sent",
                                    "Your advertisement request has been submitted for review.");

                            // 📢 Notify ADMINS
                            NotificationHelper.notifyAdmins(
                                    AdRequestActivity.this,
                                    "New Advertisement Request",
                                    "A user has submitted a new advertisement request.",
                                    "OPEN_AD_REQUESTS", // Action to handle in NotificationActivity
                                    id);

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
        if (uploadProgressContainer != null) {
            uploadProgressContainer.setVisibility(submitting ? View.VISIBLE : View.GONE);
        }
        if (progressBar != null && submitting) progressBar.setProgress(0);
        if (tvUploadPercent != null && submitting) tvUploadPercent.setText("Uploading\u2026 0%");
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private boolean isValidImage(String mimeType) {
        return mimeType != null &&
                (mimeType.equals("image/jpeg") ||
                        mimeType.equals("image/jpg") ||
                        mimeType.equals("image/png"));
    }

    private boolean isValidMedia(String mimeType) {
        return mimeType != null &&
                (mimeType.startsWith("image/") || mimeType.startsWith("video/"));
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
                // ✅ Check size limit (50MB for advertisements)
                long limit = 50L * 1024 * 1024;
                long fileSize = -1;
                try (android.content.res.AssetFileDescriptor afd = getContentResolver().openAssetFileDescriptor(uri, "r")) {
                    if (afd != null) fileSize = afd.getLength();
                } catch (Exception ignored) {}

                if (fileSize > limit) {
                    runOnUiThread(() -> callback.onError("File is too big. Max limit is 50MB."));
                    return;
                }

                String boundary = "----RMPLUS" + System.currentTimeMillis();
                String mimeType = getContentResolver().getType(uri);
                if (mimeType == null) mimeType = "image/jpeg";

                // Determine extension
                String ext = ".jpg";
                if (mimeType.contains("png")) ext = ".png";
                else if (mimeType.startsWith("video/")) ext = ".mp4";
                String fileName = "ad_" + System.currentTimeMillis() + ext;

                java.net.URL url = new java.net.URL("http://187.77.184.84/upload.php");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                
                String header = "--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n" +
                        "Content-Type: " + mimeType + "\r\n\r\n";
                String footer = "\r\n--" + boundary + "--\r\n";

                // ✅ Fixed length streaming: prevents OutOfMemoryError AND provides Content-Length for PHP!
                // PHP does not populate $_FILES for chunked encoding without a Content-Length.
                if (fileSize > 0) {
                    long totalLength = header.getBytes("UTF-8").length + fileSize + footer.getBytes("UTF-8").length;
                    conn.setFixedLengthStreamingMode(totalLength);
                } else {
                    conn.setChunkedStreamingMode(65536);
                }
                
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Content-Type",
                        "multipart/form-data; boundary=" + boundary);

                java.io.OutputStream rawOut = conn.getOutputStream();
                java.io.DataOutputStream out = new java.io.DataOutputStream(
                        new java.io.BufferedOutputStream(rawOut, 65536));

                out.write(header.getBytes("UTF-8"));

                InputStream input = getContentResolver().openInputStream(uri);
                // ✅ 64KB buffer — 16x larger than before, dramatically speeds up video uploads
                byte[] buffer = new byte[65536];
                long uploaded = 0;
                final long total = fileSize;
                int len;
                while ((len = input.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                    uploaded += len;
                    if (total > 0) {
                        final int pct = (int) (uploaded * 100 / total);
                        runOnUiThread(() -> {
                            if (progressBar != null) progressBar.setProgress(pct);
                            if (tvUploadPercent != null)
                                tvUploadPercent.setText("Uploading\u2026 " + pct + "%");
                        });
                    }
                }
                input.close();

                out.write(footer.getBytes("UTF-8"));
                out.flush();
                out.close();

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder res = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null)
                        res.append(line);
                    reader.close();

                    try {
                        org.json.JSONObject json = new org.json.JSONObject(res.toString());
                        if (json.has("url")) {
                            callback.onSuccess(json.getString("url"));
                        } else {
                            callback.onError("Server rejected: " + res);
                        }
                    } catch (org.json.JSONException e) {
                        callback.onError("Server returned an invalid response (file might be too large for VPS limits).");
                    }
                } else {
                    callback.onError("HTTP " + code);
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }
}
