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
import com.example.rmplus.NotificationHelper;
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
    EditText editAdLink; // NEW
    View placeholderLayout, previewWrapper, btnChangeMedia, icPlayVideo; // Added icPlayVideo

    ArrayList<String> sections = new ArrayList<>();
    ArrayList<String> sectionKeys = new ArrayList<>();
    ArrayList<String> subSectionKeys = new ArrayList<>();

    // Modern Activity Result Launchers
    private final androidx.activity.result.ActivityResultLauncher<Intent> imagePicker = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri sourceUri = result.getData().getData();
                    if (sourceUri != null) {
                        originalImageUri = sourceUri;
                        int pos = spinnerSection.getSelectedItemPosition();
                        String sectionKey = (pos >= 0 && pos < sectionKeys.size()) ? sectionKeys.get(pos) : "";

                        if (sectionKey.equalsIgnoreCase("Reel Maker")) {
                            selectedImageUri = sourceUri;
                            previewImage.setVisibility(View.VISIBLE);
                            placeholderLayout.setVisibility(View.GONE);
                            btnChangeMedia.setVisibility(View.VISIBLE);

                            com.bumptech.glide.Glide.with(this)
                                    .load(selectedImageUri)
                                    .into(previewImage);

                            icPlayVideo.setVisibility(View.VISIBLE);
                            toast(R.string.msg_video_selected);
                        } else if (sectionKey.equalsIgnoreCase("Business Frame")) {
                            selectedImageUri = sourceUri;
                            previewImage.setVisibility(View.VISIBLE);
                            placeholderLayout.setVisibility(View.GONE);
                            btnChangeMedia.setVisibility(View.VISIBLE);
                            previewImage.setImageURI(selectedImageUri);
                        } else {
                            startCrop(sourceUri);
                        }
                    }
                }
            }
    );

    private final androidx.activity.result.ActivityResultLauncher<Intent> cropLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    final Uri resultUri = UCrop.getOutput(result.getData());
                    if (resultUri != null) {
                        selectedImageUri = resultUri;
                        previewImage.setVisibility(View.VISIBLE);
                        placeholderLayout.setVisibility(View.GONE);
                        btnChangeMedia.setVisibility(View.VISIBLE);
                        previewImage.setImageURI(selectedImageUri);
                        icPlayVideo.setVisibility(View.GONE);
                        toast(R.string.msg_crop_success);
                    }
                } else if (result.getResultCode() == UCrop.RESULT_ERROR && result.getData() != null) {
                    final Throwable cropError = UCrop.getError(result.getData());
                    if (cropError != null)
                        toast("Crop Error: " + cropError.getMessage());
                }
            }
    );

    String getLocalizedSubCatName(String key) {
        if (key == null)
            return "";
        if (key.equalsIgnoreCase("Political"))
            return getString(R.string.cat_political);
        if (key.equalsIgnoreCase("NGO"))
            return getString(R.string.cat_ngo);
        if (key.equalsIgnoreCase("Business"))
            return getString(R.string.cat_business);
        return key;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_templates);

        SharedPreferences sps = getSharedPreferences("APP_DATA", MODE_PRIVATE);

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
        placeholderLayout = findViewById(R.id.placeholderLayout);
        previewWrapper = findViewById(R.id.previewWrapper);
        btnChangeMedia = findViewById(R.id.btnChangeMedia);

        icPlayVideo = findViewById(R.id.icPlayVideo);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });

        btnChangeMedia.setOnClickListener(v -> pickMedia());
        btnChangeMedia.setVisibility(View.GONE);

        btnSelectImage.setAlpha(0.5f);

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (view, insets) -> {
            // ... (keep existing padding logic)
            view.setPadding(
                    view.getPaddingLeft(),
                    view.getPaddingTop(),
                    view.getPaddingRight(),
                    0);
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
                    for (String k : subSectionKeys)
                        subDisplays.add(getLocalizedSubCatName(k));

                    spinnerSubSection.setAdapter(new ArrayAdapter<>(UploadTemplatesActivity.this,
                            android.R.layout.simple_spinner_dropdown_item, subDisplays));
                } else {
                    dateContainer.setVisibility(View.GONE);
                    editAdLink.setVisibility(View.GONE);
                    subSectionContainer.setVisibility(View.GONE);
                    selectedDate = "";
                    btnPickDate.setText(R.string.btn_pick_date);
                }

                updatePreviewVisibility(sectionKey);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Select Image (Dynamically switch between Image and Video)
        btnSelectImage.setOnClickListener(v1 -> pickMedia());
        previewWrapper.setOnClickListener(v1 -> pickMedia());

        // Preview Image
        previewImage.setOnClickListener(v -> {
            int pos = spinnerSection.getSelectedItemPosition();
            if (pos < 0 || pos >= sectionKeys.size())
                return;
            String sectionKey = sectionKeys.get(pos);

            if (selectedImageUri != null) {
                if (sectionKey.equalsIgnoreCase("Reel Maker")) {
                    Intent i = new Intent(UploadTemplatesActivity.this, ImagePreviewActivity.class);
                    i.putExtra("img", selectedImageUri.toString());
                    i.putExtra("is_video", true);
                    startActivity(i);
                } else if (sectionKey.equalsIgnoreCase("Business Frame")) {
                    Intent i = new Intent(UploadTemplatesActivity.this, ImagePreviewActivity.class);
                    i.putExtra("img", selectedImageUri.toString());
                    startActivity(i);
                } else {
                    // Re-crop!
                    startCrop(originalImageUri != null ? originalImageUri : selectedImageUri);
                }
            }
        });

        icPlayVideo.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                Intent i = new Intent(UploadTemplatesActivity.this, ImagePreviewActivity.class);
                i.putExtra("img", selectedImageUri.toString());
                i.putExtra("is_video", true);
                startActivity(i);
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
                    cal.get(Calendar.DAY_OF_MONTH));
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
                String uriString = selectedImageUri.toString().toLowerCase();
                if (mimeType == null && !uriString.contains(".mp4") && !uriString.contains(".mkv")
                        && !uriString.contains(".mov") && !uriString.contains(".webm")) {
                    Toast.makeText(this, R.string.msg_select_valid_video, Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Size check is handled inside uploadImageToServer
                
                // Check if Reel Maker video is 9:16 (vertical reel) or something similar.
                try {
                    android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
                    retriever.setDataSource(this, selectedImageUri);
                    String widthStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                    String heightStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                    String rotationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
                    
                    if (widthStr != null && heightStr != null) {
                        int width = Integer.parseInt(widthStr);
                        int height = Integer.parseInt(heightStr);
                        int rotation = rotationStr != null ? Integer.parseInt(rotationStr) : 0;
                        
                        if (rotation == 90 || rotation == 270) {
                            int temp = width;
                            width = height;
                            height = temp;
                        }
                        
                        // Allow 9:16 strictly (or very close to it)
                        // width / height should be ~ 0.5625 (9/16)
                        float ratio = (float) width / height;
                        if (ratio > 0.6f) { // it is wider than 9:16 (e.g., 1:1 is 1.0, 16:9 is 1.77)
                            Toast.makeText(this, R.string.msg_video_ratio_error, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // Image Validation
                String uriStr = selectedImageUri.toString().toLowerCase();
                boolean isImage = (mimeType != null && mimeType.startsWith("image/"))
                        || uriStr.contains(".jpg") || uriStr.contains(".jpeg") || uriStr.contains(".png");

                if (!isImage) {
                    Toast.makeText(this, R.string.msg_select_valid_img, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (sectionKey.equalsIgnoreCase("Business Frame")) {
                    // Strictly PNG for Business Frame
                    boolean isPng = (mimeType != null && mimeType.contains("png")) || uriStr.contains(".png");
                    if (!isPng) {
                        toast(R.string.msg_only_png_frames);
                        return;
                    }
                } else {
                    // For other sections, allow JPG/JPEG/PNG
                    boolean isJpgOrPng = (mimeType != null && (mimeType.contains("jpeg") || mimeType.contains("jpg") || mimeType.contains("png")))
                            || uriStr.contains(".jpg") || uriStr.contains(".jpeg") || uriStr.contains(".png");

                    if (!isJpgOrPng) {
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
                        if (is != null)
                            is.close();

                        if (opts.outWidth != opts.outHeight) {
                            toast(R.string.msg_frame_square);
                            return;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            if (sectionKey.equalsIgnoreCase("Advertisement")) {
                // Link is now optional
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
                    placeholderLayout.setVisibility(View.GONE);
                    btnChangeMedia.setVisibility(View.VISIBLE);
                    
                    com.bumptech.glide.Glide.with(this)
                            .load(selectedImageUri)
                            .into(previewImage);

                    icPlayVideo.setVisibility(View.VISIBLE);
                    toast(R.string.msg_video_selected);
                } else if (sectionKey.equalsIgnoreCase("Business Frame")) {
                    // 🚫 SKIP MANDATORY CROP
                    selectedImageUri = sourceUri;
                    previewImage.setVisibility(View.VISIBLE);
                    placeholderLayout.setVisibility(View.GONE);
                    btnChangeMedia.setVisibility(View.VISIBLE);
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
                placeholderLayout.setVisibility(View.GONE);
                btnChangeMedia.setVisibility(View.VISIBLE);
                previewImage.setImageURI(selectedImageUri);
                icPlayVideo.setVisibility(View.GONE);
                toast(R.string.msg_crop_success);
            }
        }
    }
    private void startCrop(@NonNull Uri uri) {
        int pos = spinnerSection.getSelectedItemPosition();
        String sectionKey = (pos >= 0 && pos < sectionKeys.size()) ? sectionKeys.get(pos) : "";

        String destinationFileName = "cropped_" + System.currentTimeMillis() + ".jpg";
        UCrop uCrop = UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), destinationFileName)));

        if (sectionKey.equalsIgnoreCase("Advertisement")) {
            uCrop.withAspectRatio(380, 160);
        } else {
            uCrop.withAspectRatio(1, 1);
        }

        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(android.graphics.Bitmap.CompressFormat.JPEG);
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

    void saveImage(String section) {
        uploadImageToServer(selectedImageUri, new UploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                runOnUiThread(() -> {
                    SharedPreferences sp = getSharedPreferences("HOME_DATA", MODE_PRIVATE);
                    Gson gson = new Gson();
                    com.google.firebase.database.DatabaseReference dbRef;
                    if (section.equalsIgnoreCase("Business Frame")) {
                        int subPos = spinnerSubSection.getSelectedItemPosition();
                        String sub = (subPos >= 0 && subPos < subSectionKeys.size()) ? subSectionKeys.get(subPos) : "Business";
                        dbRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("templates").child(section).child(sub);
                    } else {
                        dbRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("templates").child(section);
                    }

                    String templateId = dbRef.push().getKey();
                    if (templateId == null) templateId = String.valueOf(System.currentTimeMillis());

                    if (section.equalsIgnoreCase("Advertisement")) {
                        String link = editAdLink.getText().toString().trim();
                        AdvertisementItem adItem = new AdvertisementItem(imageUrl, link, expiryTime, "Admin", System.currentTimeMillis());
                        adItem.id = templateId;
                        dbRef.child(templateId).setValue(adItem);
                        NotificationHelper.sendBroadcast(UploadTemplatesActivity.this, templateId, getString(R.string.title_notif_new_ad), getString(R.string.msg_notif_new_ad), "OPEN_AD", templateId, expiryTime);
                        Type t = new TypeToken<ArrayList<AdvertisementItem>>() {}.getType();
                        ArrayList<AdvertisementItem> list = gson.fromJson(sp.getString("Advertisement", "[]"), t);
                        if (list == null) list = new ArrayList<>();
                        list.add(0, adItem);
                        sp.edit().putString("Advertisement", gson.toJson(list)).apply();
                        toast(R.string.msg_upload_success);
                        finish();
                        return;
                    }

                    if (section.equalsIgnoreCase("Festival Cards")) {
                        FestivalCardItem festItem = new FestivalCardItem(imageUrl, selectedDate, expiryTime);
                        dbRef.child(templateId).setValue(festItem);
                        Type t = new TypeToken<ArrayList<FestivalCardItem>>() {}.getType();
                        ArrayList<FestivalCardItem> list = gson.fromJson(sp.getString(section, "[]"), t);
                        if (list == null) list = new ArrayList<>();
                        list.add(0, festItem);
                        sp.edit().putString(section, gson.toJson(list)).apply();
                        toast(R.string.msg_upload_success);
                        finish();
                        return;
                    }

                    java.util.Map<String, Object> normalItem = new java.util.HashMap<>();
                    normalItem.put("url", imageUrl);
                    normalItem.put("timestamp", System.currentTimeMillis());
                    normalItem.put("expiryDate", expiryTime);
                    if (section.equalsIgnoreCase("Reel Maker")) normalItem.put("type", "video");
                    dbRef.child(templateId).setValue(normalItem);

                    if (section.equalsIgnoreCase("Latest Update")) {
                        NotificationHelper.sendBroadcast(UploadTemplatesActivity.this, templateId, getString(R.string.title_notif_latest_update), getString(R.string.msg_check_latest_update), "OPEN_TEMPLATE", templateId, expiryTime);
                    }
                    if (section.equalsIgnoreCase("Business Frame")) {
                        NotificationHelper.sendBroadcast(UploadTemplatesActivity.this, templateId, getString(R.string.title_notif_business_frame), getString(R.string.msg_notif_business_frame), "OPEN_BUSINESS_FRAME", templateId, expiryTime);
                    }

                    Type type = new TypeToken<ArrayList<String>>() {}.getType();
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
                runOnUiThread(() -> Toast.makeText(UploadTemplatesActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updatePreviewVisibility(String sectionKey) {
        if (selectedImageUri == null) {
            icPlayVideo.setVisibility(View.GONE);
            return;
        }
        if (sectionKey.equalsIgnoreCase("Reel Maker")) {
            icPlayVideo.setVisibility(View.VISIBLE);
            com.bumptech.glide.Glide.with(this).load(selectedImageUri).into(previewImage);
        } else {
            icPlayVideo.setVisibility(View.GONE);
            previewImage.setImageURI(selectedImageUri);
        }
    }

    private void uploadImageToServer(Uri imageUri, UploadCallback callback) {
        android.content.ContentResolver resolver = getContentResolver();
        int pos = spinnerSection.getSelectedItemPosition();
        String sectionKey = (pos >= 0 && pos < sectionKeys.size()) ? sectionKeys.get(pos) : "";
        boolean isReel = sectionKey.equalsIgnoreCase("Reel Maker");
        long limit = (isReel ? 50L : 15L) * 1024 * 1024;

        new Thread(() -> {
            try {
                long fileSize = -1;
                try (android.content.res.AssetFileDescriptor afd = resolver.openAssetFileDescriptor(imageUri, "r")) {
                    if (afd != null) fileSize = afd.getLength();
                } catch (Exception ignored) {}

                if (fileSize > limit) {
                    callback.onError("File is too big (" + (fileSize / 1024 / 1024) + "MB). Max limit is 50MB.");
                    return;
                }

                String mimeType = resolver.getType(imageUri);
                if (mimeType == null) mimeType = isReel ? "video/mp4" : "image/jpeg";

                String boundary = "----RMPLUS" + System.currentTimeMillis();
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL("http://187.77.184.84/upload.php").openConnection();
                conn.setConnectTimeout(60000);
                conn.setReadTimeout(180000);
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setUseCaches(false);
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                String ext = isReel ? ".mp4" : ".jpg";
                if (mimeType.contains("png")) ext = ".png";

                String head = "--" + boundary + "\r\n" +
                              "Content-Disposition: form-data; name=\"file\"; filename=\"up_" + System.currentTimeMillis() + ext + "\"\r\n" +
                              "Content-Type: " + mimeType + "\r\n\r\n";
                String tail = "\r\n--" + boundary + "--\r\n";

                byte[] headBytes = head.getBytes("UTF-8");
                byte[] tailBytes = tail.getBytes("UTF-8");

                if (fileSize > 0) {
                    conn.setFixedLengthStreamingMode(headBytes.length + (int)fileSize + tailBytes.length);
                } else {
                    conn.setChunkedStreamingMode(16384);
                }

                try (java.io.OutputStream out = conn.getOutputStream()) {
                    out.write(headBytes);
                    try (InputStream input = resolver.openInputStream(imageUri)) {
                        byte[] buffer = new byte[16384];
                        int read;
                        while ((read = input.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                    }
                    out.write(tailBytes);
                    out.flush();
                }

                int responseCode = conn.getResponseCode();
                InputStream resStream = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                StringBuilder sb = new StringBuilder();
                if (resStream != null) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(resStream));
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                }

                String raw = sb.toString().trim();
                if (responseCode == 200 && !raw.isEmpty()) {
                    org.json.JSONObject json = new org.json.JSONObject(raw);
                    if (json.has("status") && "success".equals(json.getString("status"))) {
                        callback.onSuccess(json.getString("url"));
                    } else {
                        callback.onError("Server: " + json.optString("message", raw));
                    }
                } else {
                    callback.onError("Server Error " + responseCode + (raw.isEmpty() ? "" : ": " + raw));
                }
            } catch (Exception e) {
                e.printStackTrace();
                callback.onError("Upload Error: " + e.getLocalizedMessage());
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

    private void pickMedia() {
        int pos = spinnerSection.getSelectedItemPosition();
        if (pos < 0 || pos >= sectionKeys.size()) return;
        String sectionKey = sectionKeys.get(pos);
        Intent i;
        if (sectionKey.equalsIgnoreCase("Reel Maker")) {
            i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            i.setType("video/*");
        } else if (sectionKey.equalsIgnoreCase("Business Frame")) {
            i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/png");
            i.addCategory(Intent.CATEGORY_OPENABLE);
        } else {
            i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            i.setType("image/*");
            String[] mimeTypes = {"image/jpeg", "image/png", "image/jpg"};
            i.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        }
        imagePicker.launch(i);
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