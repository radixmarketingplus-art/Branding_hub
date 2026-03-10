package com.example.rmplus;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.InputStream;
import java.util.HashMap;

public class AdminAppOpenOfferActivity extends AppCompatActivity {

    private SwitchMaterial switchEnable;
    private View imgPreviewCard;
    private ImageView imgPreview;
    private LinearLayout placeholderLayout;
    private TextInputEditText etActionLink;
    private MaterialButton btnSave;
    private ProgressBar progressBar;

    private Uri selectedUri;
    private String currentImageUrl = "";
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private DatabaseReference offerRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_app_open_offer);

        switchEnable = findViewById(R.id.switchEnable);
        imgPreviewCard = findViewById(R.id.imgPreviewCard);
        imgPreview = findViewById(R.id.imgPreview);
        placeholderLayout = findViewById(R.id.placeholderLayout);
        etActionLink = findViewById(R.id.etActionLink);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        offerRef = FirebaseDatabase.getInstance().getReference("admin_settings").child("app_open_offer");

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri == null) return;

                        String mimeType = getContentResolver().getType(uri);
                        if (mimeType == null || (!mimeType.equals("image/jpeg") && !mimeType.equals("image/jpg") && !mimeType.equals("image/png"))) {
                            Toast.makeText(this, R.string.msg_invalid_img_format, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        selectedUri = uri;
                        imgPreview.setImageURI(selectedUri);
                        placeholderLayout.setVisibility(View.GONE);
                    }
                });

        imgPreviewCard.setOnClickListener(v -> pickImage());
        btnSave.setOnClickListener(v -> saveOffer());

        loadCurrentOffer();
    }

    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/jpeg", "image/jpg", "image/png"});
        imagePickerLauncher.launch(i);
    }

    private void loadCurrentOffer() {
        progressBar.setVisibility(View.VISIBLE);
        offerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                if (snapshot.exists()) {
                    boolean isEnabled = snapshot.child("enabled").getValue(Boolean.class) != null ? snapshot.child("enabled").getValue(Boolean.class) : false;
                    String link = snapshot.child("actionLink").getValue(String.class);
                    String imgUrl = snapshot.child("imageUrl").getValue(String.class);

                    switchEnable.setChecked(isEnabled);
                    if (link != null) etActionLink.setText(link);
                    if (imgUrl != null && !imgUrl.isEmpty()) {
                        currentImageUrl = imgUrl;
                        Glide.with(AdminAppOpenOfferActivity.this).load(currentImageUrl).into(imgPreview);
                        placeholderLayout.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminAppOpenOfferActivity.this, "Error loading offer", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveOffer() {
        boolean isEnabled = switchEnable.isChecked();
        String link = etActionLink.getText() != null ? etActionLink.getText().toString().trim() : "";

        if (isEnabled && selectedUri == null && currentImageUrl.isEmpty()) {
            Toast.makeText(this, "Please select an offer image to enable.", Toast.LENGTH_SHORT).show();
            return;
        }

        setSaving(true);
        if (selectedUri != null) {
            uploadImageToServer(selectedUri, new UploadCallback() {
                @Override
                public void onSuccess(String url) {
                    saveToFirebase(isEnabled, link, url);
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        setSaving(false);
                        Toast.makeText(AdminAppOpenOfferActivity.this, "Upload failed: " + message, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } else {
            saveToFirebase(isEnabled, link, currentImageUrl);
        }
    }

    private void saveToFirebase(boolean isEnabled, String link, String imageUrl) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("enabled", isEnabled);
        data.put("actionLink", link);
        data.put("imageUrl", imageUrl);
        data.put("timestamp", System.currentTimeMillis());

        offerRef.setValue(data).addOnSuccessListener(unused -> {
            runOnUiThread(() -> {
                setSaving(false);
                Toast.makeText(AdminAppOpenOfferActivity.this, "Offer Saved Successfully!", Toast.LENGTH_SHORT).show();

                // ✅ BROADCAST NOTIFICATION: If offer is enabled, notify all users
                if (isEnabled) {
                    NotificationHelper.sendBroadcast(
                            AdminAppOpenOfferActivity.this,
                            "offer_" + System.currentTimeMillis(),
                            "New Special Offer!",
                            "Check out the exclusive offer now!",
                            "OPEN_OFFER",
                            "",
                            0
                    );
                }

                finish();
            });
        }).addOnFailureListener(e -> {
            runOnUiThread(() -> {
                setSaving(false);
                Toast.makeText(AdminAppOpenOfferActivity.this, "Failed to save offer.", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void setSaving(boolean saving) {
        btnSave.setEnabled(!saving);
        progressBar.setVisibility(saving ? View.VISIBLE : View.GONE);
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
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                java.io.DataOutputStream out = new java.io.DataOutputStream(conn.getOutputStream());

                out.writeBytes("--" + boundary + "\r\n");
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"offer.jpg\"\r\n");
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
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                    StringBuilder res = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) res.append(line);
                    reader.close();

                    org.json.JSONObject json = new org.json.JSONObject(res.toString());
                    if (json.has("url")) {
                        callback.onSuccess(json.getString("url"));
                    } else {
                        callback.onError("No URL in response");
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
