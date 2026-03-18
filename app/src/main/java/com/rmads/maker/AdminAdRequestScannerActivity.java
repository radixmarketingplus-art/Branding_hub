package com.rmads.maker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import com.yalantis.ucrop.UCrop;
import java.io.File;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AdminAdRequestScannerActivity extends BaseActivity {

    ImageView btnBack, imgPreview;
    LinearLayout placeholderLayout;
    MaterialCardView imgPreviewCard;
    MaterialButton btnSave;
    ProgressBar progressBar;

    Uri selectedImageUri;
    String existingImageUrl = "";

    DatabaseReference dbRef;

    ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri sourceUri = result.getData().getData();
                    if (sourceUri != null) {
                        startCrop(sourceUri);
                    }
                }
            });

    private void startCrop(@NonNull Uri uri) {
        String destinationFileName = "ScannerCrop_" + System.currentTimeMillis() + ".jpg";
        UCrop uCrop = UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), destinationFileName)));
        uCrop.withAspectRatio(1, 1);
        uCrop.withMaxResultSize(1000, 1000);
        uCrop.start(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            final Uri resultUri = UCrop.getOutput(data);
            if (resultUri != null) {
                selectedImageUri = resultUri;
                imgPreview.setImageURI(selectedImageUri);
                placeholderLayout.setVisibility(View.GONE);
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
            if (cropError != null) {
                Toast.makeText(this, "Crop error: " + cropError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_ad_request_scanner);

        btnBack = findViewById(R.id.btnBack);
        imgPreview = findViewById(R.id.imgPreview);
        placeholderLayout = findViewById(R.id.placeholderLayout);
        imgPreviewCard = findViewById(R.id.imgPreviewCard);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        dbRef = FirebaseDatabase.getInstance().getReference("admin_settings").child("ad_request_scanner");

        btnBack.setOnClickListener(v -> finish());

        imgPreviewCard.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        btnSave.setOnClickListener(v -> saveAdRequestScanner());

        loadExistingScanner();
    }

    private void loadExistingScanner() {
        progressBar.setVisibility(View.VISIBLE);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                if (snapshot.exists() && snapshot.hasChild("imageUrl")) {
                    existingImageUrl = snapshot.child("imageUrl").getValue(String.class);

                    if (existingImageUrl != null && !existingImageUrl.isEmpty()) {
                        Glide.with(AdminAdRequestScannerActivity.this)
                                .load(existingImageUrl)
                                .into(imgPreview);
                        placeholderLayout.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminAdRequestScannerActivity.this, getString(R.string.msg_failed_to_load), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveAdRequestScanner() {
        if (selectedImageUri != null) {
            uploadImageAndSave();
        } else {
            // Check if they already have an existing image, or warn them.
            if (!existingImageUrl.isEmpty()) {
                saveDataToFirebase(existingImageUrl);
            } else {
                Toast.makeText(this, getString(R.string.msg_select_image_first), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadImageAndSave() {
        setLoading(true);

        new Thread(() -> {
            try {
                String boundary = "----RMPLUS" + System.currentTimeMillis();
                URL url = new URL("http://187.77.184.84/upload.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream out = new DataOutputStream(conn.getOutputStream());

                out.writeBytes("--" + boundary + "\r\n");
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"scanner_" + System.currentTimeMillis() + ".jpg\"\r\n");
                out.writeBytes("Content-Type: image/jpeg\r\n\r\n");

                InputStream input = getContentResolver().openInputStream(selectedImageUri);
                if (input != null) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = input.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                    input.close();
                }

                out.writeBytes("\r\n--" + boundary + "--\r\n");
                out.flush();
                out.close();

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder res = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) res.append(line);
                    reader.close();

                    JSONObject json = new JSONObject(res.toString());
                    if (json.has("url")) {
                        String uploadedUrl = json.getString("url");
                        runOnUiThread(() -> saveDataToFirebase(uploadedUrl));
                    } else {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(this, "Upload failed: Invalid response", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(this, "Upload failed: HTTP " + code, Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Upload Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void saveDataToFirebase(String imageUrl) {
        setLoading(true);
        dbRef.child("imageUrl").setValue(imageUrl)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(AdminAdRequestScannerActivity.this, getString(R.string.msg_scanner_saved), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(AdminAdRequestScannerActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnSave.setEnabled(false);
            imgPreviewCard.setEnabled(false);
            btnSave.setText(R.string.msg_saving);
        } else {
            progressBar.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            imgPreviewCard.setEnabled(true);
            btnSave.setText(R.string.btn_save_scanner);
        }
    }
}
