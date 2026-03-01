package com.example.rmplus;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.util.Calendar;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.io.File;
import com.yalantis.ucrop.UCrop;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class UploadTemplatesActivity extends BaseActivity {

    Spinner spinnerSection, spinnerSubSection;
    Button btnSelectImage, btnSave;
    Uri selectedImageUri, originalImageUri;
    LinearLayout dateContainer, subSectionContainer;
    ImageView previewImage;
    TextView btnPickDate, btnPickExpiry; // Changed from Button to TextView, added btnPickExpiry
    long expiryTime = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000); // Default 7 days
    String selectedDate = "";
    EditText editAdLink;   // NEW


    ArrayList<String> sections = new ArrayList<>();
    ArrayList<String> sectionKeys = new ArrayList<>();
    ArrayList<String> subSectionKeys = new ArrayList<>();


    String getLocalizedSubCatName(String key) {
        if (key == null) return "";
        if (key.equalsIgnoreCase("Political")) return getString(R.string.cat_political);
        if (key.equalsIgnoreCase("NGO")) return getString(R.string.cat_ngo);
        if (key.equalsIgnoreCase("Business")) return getString(R.string.cat_business);
        return key;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_templates);

        SharedPreferences sps =
                getSharedPreferences("APP_DATA", MODE_PRIVATE);

        String role = sps.getString("role", "user");

        setupBase(role, R.id.upload);

        // Bind views
        spinnerSection = findViewById(R.id.spinnerSection);
        spinnerSubSection = findViewById(R.id.spinnerSubSection);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        previewImage = findViewById(R.id.previewImage);
        btnSave = findViewById(R.id.btnSave);
        dateContainer = findViewById(R.id.dateContainer);
        subSectionContainer = findViewById(R.id.subSectionContainer);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnPickExpiry = findViewById(R.id.btnPickExpiry);
        editAdLink = findViewById(R.id.editAdLink);

        btnSelectImage.setAlpha(0.5f);

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (view, insets) -> {
            // ... (keep existing padding logic)
            view.setPadding(
                    view.getPaddingLeft(),
                    view.getPaddingTop(),
                    view.getPaddingRight(),
                    0
            );
            return insets;
        });

        loadDynamicSections();

        spinnerSection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 🛡️ Get the real Firebase key instead of the localized display name
                String sectionKey = sectionKeys.get(position);

                if (sectionKey.equalsIgnoreCase("Select Section")) {
                    btnSelectImage.setEnabled(false);
                    btnSelectImage.setAlpha(0.5f);
                } else {
                    btnSelectImage.setEnabled(true);
                    btnSelectImage.setAlpha(1.0f);
                }

                if (sectionKey.equalsIgnoreCase("Festival Cards")) {
                    dateContainer.setVisibility(View.VISIBLE);
                    editAdLink.setVisibility(View.GONE);

                } else if (sectionKey.equalsIgnoreCase("Advertisement")) {
                    editAdLink.setVisibility(View.VISIBLE);
                    dateContainer.setVisibility(View.GONE);
                    subSectionContainer.setVisibility(View.GONE);
                } else if (sectionKey.equalsIgnoreCase("Business Frame")) {
                    subSectionContainer.setVisibility(View.VISIBLE);
                    dateContainer.setVisibility(View.GONE);
                    editAdLink.setVisibility(View.GONE);
                    
                    subSectionKeys.clear();
                    subSectionKeys.add("Political");
                    subSectionKeys.add("NGO");
                    subSectionKeys.add("Business");

                    ArrayList<String> subDisplays = new ArrayList<>();
                    for (String k : subSectionKeys) subDisplays.add(getLocalizedSubCatName(k));

                    spinnerSubSection.setAdapter(new ArrayAdapter<>(UploadTemplatesActivity.this, android.R.layout.simple_spinner_dropdown_item, subDisplays));
                } else {
                    dateContainer.setVisibility(View.GONE);
                    editAdLink.setVisibility(View.GONE);
                    subSectionContainer.setVisibility(View.GONE);
                    selectedDate = "";
                    btnPickDate.setText(R.string.btn_pick_date);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Select Image (Dynamically switch between Image and Video)
        btnSelectImage.setOnClickListener(v1 -> {
            int pos = spinnerSection.getSelectedItemPosition();
            if (pos < 0 || pos >= sectionKeys.size()) return;
            String sectionKey = sectionKeys.get(pos);

            Intent i;
            
            if (sectionKey.equalsIgnoreCase("Reel Maker")) {
                i = new Intent(Intent.ACTION_PICK);
                i.setType("video/*");
            } else if (sectionKey.equalsIgnoreCase("Business Frame")) {
                // 🛡️ Use ACTION_GET_CONTENT for strict '.png' filtering
                i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("image/png");
                i.addCategory(Intent.CATEGORY_OPENABLE);
            } else {
                i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                i.setType("image/*");
                String[] mimeTypes = {"image/jpeg", "image/png", "image/jpg"};
                i.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            }
            
            startActivityForResult(i, 101);
        });

        // Preview Image
        previewImage.setOnClickListener(v -> {
            int pos = spinnerSection.getSelectedItemPosition();
            if (pos < 0 || pos >= sectionKeys.size()) return;
            String sectionKey = sectionKeys.get(pos);

            if (selectedImageUri != null) {
                if (sectionKey.equalsIgnoreCase("Reel Maker") || 
                    sectionKey.equalsIgnoreCase("Business Frame")) {
                    
                    Intent i = new Intent(UploadTemplatesActivity.this, ImagePreviewActivity.class);
                    i.putExtra("img", selectedImageUri.toString());
                    startActivity(i);
                } else {
                    // Re-crop!
                    startCrop(originalImageUri != null ? originalImageUri : selectedImageUri);
                }
            }
        });

        // Pick date
        btnPickDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();

            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (view, year, month, day) -> {
                        month++;
                        selectedDate = day + "-" + month + "-" + year;
                        btnPickDate.setText(getString(R.string.label_selected, selectedDate));
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });

        btnPickExpiry.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, day, 23, 59, 59);
                expiryTime = selected.getTimeInMillis();
                btnPickExpiry.setText(getString(R.string.label_expires, day + "-" + (month + 1) + "-" + year));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Save image
        btnSave.setOnClickListener(v -> {
 
            if (selectedImageUri == null) {
                Toast.makeText(this, R.string.msg_please_select_img, Toast.LENGTH_SHORT).show();
                return;
            }
 
            int pos = spinnerSection.getSelectedItemPosition();
            if (pos < 0 || pos >= sectionKeys.size()) {
                toast(R.string.msg_invalid_section);
                return;
            }
            String sectionKey = sectionKeys.get(pos);
 
            if (sectionKey.equalsIgnoreCase("Select Section")) {
                Toast.makeText(this, R.string.msg_invalid_section, Toast.LENGTH_SHORT).show();
                return;
            }
 
            if (sectionKey.equalsIgnoreCase("Festival Cards") && selectedDate.isEmpty()) {
                Toast.makeText(this, R.string.msg_select_fest_date, Toast.LENGTH_SHORT).show();
                return;
            }

            // ---------- TYPE & SIZE VALIDATION ----------
            String mimeType = getContentResolver().getType(selectedImageUri);
            
            if (sectionKey.equalsIgnoreCase("Reel Maker")) {
                // Video Validation
                if (mimeType == null || !mimeType.startsWith("video/")) {
                    Toast.makeText(this, R.string.msg_select_valid_video, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!mimeType.contains("mp4") && !mimeType.contains("webm") && !mimeType.contains("quicktime") && !mimeType.contains("x-matroska")) {
                    Toast.makeText(this, R.string.msg_format_supported, Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                // Image Validation
                if (mimeType == null || !mimeType.startsWith("image/")) {
                    Toast.makeText(this, R.string.msg_select_valid_img, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (sectionKey.equalsIgnoreCase("Business Frame")) {
                    // Strictly PNG for Business Frame
                    if (!mimeType.contains("png") && !selectedImageUri.toString().toLowerCase().endsWith(".png")) {
                         toast(R.string.msg_only_png_frames);
                         return;
                    }
                } else {
                    if (!mimeType.contains("jpeg") && !mimeType.contains("jpg") && !mimeType.contains("png")) {
                        Toast.makeText(this, R.string.msg_format_supported, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                // 📏 SQUARE RATIO CHECK (For all except Ad and Video)
                if (!sectionKey.equalsIgnoreCase("Advertisement") && !sectionKey.equalsIgnoreCase("Reel Maker")) {
                    try {
                        InputStream is = getContentResolver().openInputStream(selectedImageUri);
                        BitmapFactory.Options opts = new BitmapFactory.Options();
                        opts.inJustDecodeBounds = true;
                        BitmapFactory.decodeStream(is, null, opts);
                        if (is != null) is.close();

                        if (opts.outWidth != opts.outHeight) {
                            toast(R.string.msg_frame_square);
                            return;
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
 
            if (sectionKey.equalsIgnoreCase("Advertisement")) {

                String link = editAdLink.getText().toString().trim();

                if (link.isEmpty()) {
                    Toast.makeText(this,
                            R.string.msg_enter_ad_link,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }


            new AlertDialog.Builder(this)
                    .setTitle(R.string.title_confirm_upload)
                    .setMessage(getString(R.string.msg_confirm_upload_format, getLocalizedSectionName(sectionKey)))
                    .setPositiveButton(android.R.string.ok, (d, w) -> saveImage(sectionKey))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            Uri sourceUri = data.getData();
            if (sourceUri != null) {
                originalImageUri = sourceUri;
                int pos = spinnerSection.getSelectedItemPosition();
                String sectionKey = (pos >= 0 && pos < sectionKeys.size()) ? sectionKeys.get(pos) : "";

                if (sectionKey.equalsIgnoreCase("Reel Maker")) {
                    selectedImageUri = sourceUri;
                    previewImage.setVisibility(View.VISIBLE);
                    previewImage.setImageURI(selectedImageUri);
                    toast(R.string.msg_video_selected);
                } else if (sectionKey.equalsIgnoreCase("Business Frame")) {
                     // 🚫 SKIP MANDATORY CROP
                     selectedImageUri = sourceUri;
                     previewImage.setVisibility(View.VISIBLE);
                     previewImage.setImageURI(selectedImageUri);
                } else {
                    // Re-crop!
                    startCrop(sourceUri);
                }
            }
        } else if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            final Uri resultUri = UCrop.getOutput(data);
            if (resultUri != null) {
                selectedImageUri = resultUri;
                previewImage.setVisibility(View.VISIBLE);
                previewImage.setImageURI(selectedImageUri);
                toast(R.string.msg_crop_success);
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
            if (cropError != null) toast("Crop Error: " + cropError.getMessage());
        }
    }

    private void startCrop(@NonNull Uri uri) {
        int pos = spinnerSection.getSelectedItemPosition();
        String sectionKey = (pos >= 0 && pos < sectionKeys.size()) ? sectionKeys.get(pos) : "";

        String destinationFileName = "cropped_" + System.currentTimeMillis() + ".jpg";
        UCrop uCrop = UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), destinationFileName)));
 
        if (sectionKey.equalsIgnoreCase("Advertisement")) {
            uCrop.withAspectRatio(16, 9);
        } else {
            uCrop.withAspectRatio(1, 1);
        }

        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(android.graphics.Bitmap.CompressFormat.JPEG);
        options.setHideBottomControls(false);

        // Custom Styling to fix visibility and add space from screen edges
        options.setToolbarTitle(getString(R.string.title_crop_image));
        options.setToolbarColor(Color.parseColor("#1B1B1B")); 
        options.setStatusBarColor(Color.parseColor("#1B1B1B"));
        options.setToolbarWidgetColor(Color.WHITE); // Make "Done" and "Cancel" buttons bright white
        options.setActiveControlsWidgetColor(Color.parseColor("#4A6CF7"));
        options.setLogoColor(Color.TRANSPARENT);
        options.setDimmedLayerColor(Color.parseColor("#CC000000")); 
        options.setCropFrameStrokeWidth(12);
        options.setCropGridStrokeWidth(2);
        options.setShowCropGrid(true);
        options.setFreeStyleCropEnabled(true);
        
        // Root view background should be black to blend with the bars
        options.setRootViewBackgroundColor(Color.BLACK);
        
        uCrop.withOptions(options);
        uCrop.start(this);
    }

    void saveImage(String section) {

        uploadImageToServer(selectedImageUri, new UploadCallback() {

            @Override
            public void onSuccess(String imageUrl) {

                runOnUiThread(() -> {

                    SharedPreferences sp =
                            getSharedPreferences("HOME_DATA", MODE_PRIVATE);

                    Gson gson = new Gson();
                    com.google.firebase.database.DatabaseReference dbRef;
                    if (section.equalsIgnoreCase("Business Frame")) {
                        int subPos = spinnerSubSection.getSelectedItemPosition();
                        String sub = (subPos >= 0 && subPos < subSectionKeys.size()) ? subSectionKeys.get(subPos) : "Business";
                        dbRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                                .getReference("templates")
                                .child(section)
                                .child(sub);
                    } else {
                        dbRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                                .getReference("templates")
                                .child(section);
                    }

                    String templateId = dbRef.push().getKey();
                    if (templateId == null) templateId = String.valueOf(System.currentTimeMillis());

                    // =============================
                    // 📢 ADVERTISEMENT
                    // =============================
                    if (section.equalsIgnoreCase("Advertisement")) {
                        String link = editAdLink.getText().toString().trim();
                        AdvertisementItem adItem = new AdvertisementItem(imageUrl, link, expiryTime, "Admin", System.currentTimeMillis());

                        // 1. Firebase
                        dbRef.child(templateId).setValue(adItem);

                        // 2. SharedPreferences
                        Type t = new TypeToken<ArrayList<AdvertisementItem>>(){}.getType();
                        ArrayList<AdvertisementItem> list = gson.fromJson(sp.getString("Advertisement", "[]"), t);
                        if (list == null) list = new ArrayList<>();
                        list.add(0, adItem);
                        sp.edit().putString("Advertisement", gson.toJson(list)).apply();

                        toast(R.string.msg_upload_success);
                        finish();
                        return;
                    }

                    // =============================
                    // 🎉 FESTIVAL
                    // =============================
                    if (section.equalsIgnoreCase("Festival Cards")) {
                        FestivalCardItem festItem = new FestivalCardItem(imageUrl, selectedDate, expiryTime);

                        // 1. Firebase
                        dbRef.child(templateId).setValue(festItem);

                        // 2. SharedPreferences
                        Type t = new TypeToken<ArrayList<FestivalCardItem>>(){}.getType();
                        ArrayList<FestivalCardItem> list = gson.fromJson(sp.getString(section, "[]"), t);
                        if (list == null) list = new ArrayList<>();
                        list.add(0, festItem);
                        sp.edit().putString(section, gson.toJson(list)).apply();

                        toast(R.string.msg_upload_success);
                        finish();
                        return;
                    }

                    // =============================
                    // 🧩 NORMAL SECTIONS / REELS
                    // =============================
                    java.util.Map<String, Object> normalItem = new java.util.HashMap<>();
                    normalItem.put("url", imageUrl);
                    normalItem.put("timestamp", System.currentTimeMillis());
                    normalItem.put("expiryDate", expiryTime);
                    if (section.equalsIgnoreCase("Reel Maker")) normalItem.put("type", "video"); // ✅ canonical key

                    // 1. Firebase
                    dbRef.child(templateId).setValue(normalItem);

                    // 2. SharedPreferences
                    Type type = new TypeToken<ArrayList<String>>(){}.getType();
                    ArrayList<String> images = gson.fromJson(sp.getString(section, "[]"), type);
                    if (images == null) images = new ArrayList<>();
                    if (!images.contains(imageUrl)) images.add(0, imageUrl);
                    sp.edit().putString(section, gson.toJson(images)).apply();

                    toast(R.string.msg_upload_success);
                    setResult(RESULT_OK);
                    finish();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() ->
                        Toast.makeText(UploadTemplatesActivity.this,
                                message,
                                Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void uploadImageToServer(Uri imageUri, UploadCallback callback) {

        new Thread(() -> {
            try {

                // ===== GET REAL FILE SIZE =====
                android.database.Cursor cursor = getContentResolver().query(
                        imageUri, null, null, null, null);

                int size = 0;

                if (cursor != null) {
                    int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                    if (cursor.moveToFirst() && sizeIndex != -1) {
                        size = (int) cursor.getLong(sizeIndex);
                    }
                    cursor.close();
                }

                int pos = spinnerSection.getSelectedItemPosition();
                String sectionKey = (pos >= 0 && pos < sectionKeys.size()) ? sectionKeys.get(pos) : "";

                if (size > 5 * 1024 * 1024) {
                    int limit = sectionKey.equalsIgnoreCase("Reel Maker") ? 30 * 1024 * 1024 : 5 * 1024 * 1024;
                    String limitText = sectionKey.equalsIgnoreCase("Reel Maker") ? "30 MB" : "5 MB";
                    
                    if (size > limit) {
                        callback.onError(getString(R.string.msg_file_size_limit, limitText));
                        return;
                    }
                }

                String boundary = "----RMPLUS" + System.currentTimeMillis();
                String mimeType = getContentResolver().getType(imageUri);
                if (mimeType == null) mimeType = sectionKey.equalsIgnoreCase("Reel Maker") ? "video/mp4" : "image/jpeg";

                java.net.URL url =
                        new java.net.URL("http://187.77.184.84/upload.php");

                java.net.HttpURLConnection conn =
                        (java.net.HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty(
                        "Content-Type",
                        "multipart/form-data; boundary=" + boundary
                );

                java.io.DataOutputStream out =
                        new java.io.DataOutputStream(conn.getOutputStream());

                String ext = ".jpg";
                if (mimeType.contains("png")) ext = ".png";
                else if (mimeType.contains("mp4")) ext = ".mp4";
                else if (mimeType.contains("webm")) ext = ".webm";
                else if (mimeType.contains("mkv") || mimeType.contains("matroska")) ext = ".mkv";

                String fileName = "up_" + System.currentTimeMillis() + ext;

                // ===== FILE PART =====
                out.writeBytes("--" + boundary + "\r\n");
                out.writeBytes(
                        "Content-Disposition: form-data; name=\"file\"; filename=\"" +
                                fileName + "\"\r\n"
                );
                out.writeBytes("Content-Type: " + mimeType + "\r\n\r\n");

                InputStream input =
                        getContentResolver().openInputStream(imageUri);

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }

                input.close();

                out.writeBytes("\r\n");
                out.writeBytes("--" + boundary + "--\r\n");
                out.flush();
                out.close();

                // ===== RESPONSE =====
                int responseCode = conn.getResponseCode();

                if (responseCode == 200) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();

                    try {
                        org.json.JSONObject json = new org.json.JSONObject(response.toString());
                        if (json.has("status") && "success".equals(json.getString("status"))) {
                            String imageUrl = json.getString("url");
                            callback.onSuccess(imageUrl);
                        } else {
                            callback.onError("Server rejected upload");
                        }
                    } catch (org.json.JSONException e) {
                        callback.onError("Invalid server response");
                    }
                } else {
                    callback.onError("Server error: " + responseCode);
                }

            } catch (java.net.SocketTimeoutException e) {
                callback.onError("Connection timeout");
            } catch (java.io.IOException e) {
                callback.onError("Network error: " + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                callback.onError("Upload failed");
            }
        }).start();
    }

    void loadDynamicSections() {
        FirebaseDatabase.getInstance().getReference("templates")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        sections.clear();
                        sectionKeys.clear();

                        sections.add(getString(R.string.section_select));
                        sectionKeys.add("Select Section");

                        for (DataSnapshot d : snapshot.getChildren()) {
                            String key = d.getKey();
                            if ("Frame".equalsIgnoreCase(key)) continue;
                            sectionKeys.add(key);
                            sections.add(getLocalizedSectionName(key));
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(UploadTemplatesActivity.this, android.R.layout.simple_spinner_dropdown_item, sections);
                        spinnerSection.setAdapter(adapter);
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    void toast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    interface UploadCallback {
        void onSuccess(String imageUrl);
        void onError(String message);
    }
}