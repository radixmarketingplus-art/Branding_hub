package com.example.rmplus;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.rmplus.models.CustomerRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.io.InputStream;

public class CreateRequestActivity extends AppCompatActivity {

    Spinner spType;
    EditText etTitle, etDesc;
    Button btnAttach, btnSubmit;
    ImageView imgPreview;
    ProgressBar progressBar;

    Uri selectedUri;            // kept locally — uploaded only on submit

    // English keys stored in Firebase regardless of locale
    private final String[] typesEn = {"Issue", "Order", "Custom Template", "Other"};

    DatabaseReference usersRef, requestRef;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_create_request);

        spType      = findViewById(R.id.spType);
        etTitle     = findViewById(R.id.etTitle);
        etDesc      = findViewById(R.id.etDesc);
        btnAttach   = findViewById(R.id.btnAttach);
        btnSubmit   = findViewById(R.id.btnSubmit);
        imgPreview  = findViewById(R.id.imgPreview);
        progressBar = findViewById(R.id.progressBar);

        String[] arr = {
                getString(R.string.cat_issue),
                getString(R.string.cat_order),
                getString(R.string.cat_custom_template),
                getString(R.string.cat_other)
        };
        spType.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                arr));

        String uid = FirebaseAuth.getInstance().getUid();

        // Tap image preview → fullscreen
        imgPreview.setOnClickListener(v -> {
            if (selectedUri != null) {
                Intent i = new Intent(CreateRequestActivity.this, ImagePreviewActivity.class);
                i.putExtra("img", selectedUri.toString());
                startActivity(i);
            }
        });

        usersRef = FirebaseDatabase.getInstance()
                .getReference("users").child(uid);

        requestRef = FirebaseDatabase.getInstance()
                .getReference("customer_requests");

        // ✅ Modern ActivityResultLauncher — no deprecated onActivityResult
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri == null) return;

                        String mimeType = getContentResolver().getType(uri);
                        if (mimeType == null ||
                                (!mimeType.equals("image/jpeg") &&
                                 !mimeType.equals("image/jpg") &&
                                 !mimeType.equals("image/png"))) {
                            Toast.makeText(this, R.string.msg_invalid_img_format, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // ✅ Keep locally only — NO VPS upload here
                        selectedUri = uri;
                        imgPreview.setImageURI(selectedUri);
                        imgPreview.setVisibility(ImageView.VISIBLE);
                    }
                }
        );

        btnAttach.setOnClickListener(v -> pickImage());
        btnSubmit.setOnClickListener(v -> saveRequest());
    }

    // -----------------------

    void pickImage() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/jpeg", "image/jpg", "image/png"});
        imagePickerLauncher.launch(i);
    }

    // -----------------------

    void saveRequest() {

        if (etTitle.getText().toString().isEmpty()) {
            etTitle.setError(getString(R.string.err_enter_title));
            return;
        }

        if (etDesc.getText().toString().isEmpty()) {
            etDesc.setError(getString(R.string.err_enter_description));
            return;
        }

        // If no image attached, submit directly without upload
        if (selectedUri == null) {
            submitToFirebase("");
            return;
        }

        // ✅ Upload to VPS only NOW (on submit)
        setSubmitting(true);
        uploadImageToServer(selectedUri, new UploadCallback() {

            @Override
            public void onSuccess(String attachmentUrl) {
                submitToFirebase(attachmentUrl);
            }

            @Override
            public void onError(String msg) {
                runOnUiThread(() -> {
                    setSubmitting(false);
                    Toast.makeText(CreateRequestActivity.this,
                            getString(R.string.msg_upload_failed) + ": " + msg,
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // -----------------------

    private void submitToFirebase(String attachmentUrl) {
        usersRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot s) {

                        String name   = s.child("name").getValue(String.class);
                        String email  = s.child("email").getValue(String.class);
                        String mobile = s.child("mobile").getValue(String.class);

                        String id = requestRef.push().getKey();

                        CustomerRequest r = new CustomerRequest();
                        r.requestId     = id;
                        r.uid           = FirebaseAuth.getInstance().getUid();
                        r.userName      = name;
                        r.email         = email;
                        r.mobile        = mobile;
                        r.type          = typesEn[spType.getSelectedItemPosition()];
                        r.title         = etTitle.getText().toString();
                        r.description   = etDesc.getText().toString();
                        r.status        = "pending";
                        r.time          = System.currentTimeMillis();
                        r.attachmentUrl = attachmentUrl;

                        requestRef.child(id).setValue(r)
                                .addOnSuccessListener(u -> {
                                    NotificationHelper.send(
                                            CreateRequestActivity.this,
                                            "ADMIN",
                                            "New Support Request",
                                            r.title);

                                    runOnUiThread(() -> {
                                        setSubmitting(false);
                                        Toast.makeText(
                                                CreateRequestActivity.this,
                                                R.string.msg_request_sent,
                                                Toast.LENGTH_SHORT).show();
                                        finish();
                                    });
                                })
                                .addOnFailureListener(e -> runOnUiThread(() -> {
                                    setSubmitting(false);
                                    Toast.makeText(CreateRequestActivity.this,
                                            "Error: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }));
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        runOnUiThread(() -> {
                            setSubmitting(false);
                            Toast.makeText(CreateRequestActivity.this,
                                    "DB Error: " + error.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    // -----------------------

    private void setSubmitting(boolean submitting) {
        btnSubmit.setEnabled(!submitting);
        btnSubmit.setAlpha(submitting ? 0.6f : 1.0f);
        btnSubmit.setText(submitting
                ? getString(R.string.msg_uploading_wait)
                : getString(R.string.btn_submit));
        if (progressBar != null)
            progressBar.setVisibility(submitting ? View.VISIBLE : View.GONE);
    }

    // -----------------------

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
                        "Content-Disposition: form-data; name=\"file\"; filename=\"req.jpg\"\r\n");
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
