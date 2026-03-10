package com.example.rmplus;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import android.graphics.Color;
import java.lang.reflect.Type;
import java.util.ArrayList;
import com.yalantis.ucrop.UCrop;

public class UploadTemplatesFragment extends Fragment {

    Spinner spinnerSection, spinnerSubSection;
    MaterialButton btnSelectImage, btnSave, btnPickDate;
    ImageView previewImage;
    LinearLayout dateContainer, subSectionContainer;
    View placeholderLayout, btnChangeMedia, icPlayVideo;

    Uri selectedImageUri, originalImageUri;
    String selectedDate = "";
    EditText editAdLink;
    MaterialButton btnPickExpiry;
    long expiryTime = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000); // Default 7 days
    // --- EDIT MODE FIELDS ---
    boolean isEditMode = false;
    String oldUrl = "";
    String oldCategory = "";
    String oldRealId = "";

    // Localized display names for spinner UI
    String[] getSections() {
        return new String[] {
                getString(R.string.section_select),
                getString(R.string.section_advertisement),
                getString(R.string.section_festival_cards),
                getString(R.string.section_latest_update),
                getString(R.string.section_business_special),
                getString(R.string.section_reel_maker),
                getString(R.string.section_business_frame),
                getString(R.string.section_motivation),
                getString(R.string.section_greetings),
                getString(R.string.section_business_ethics)
        };
    }

    // ✅ Canonical English keys — always used for Firebase & SharedPreferences
    String[] getSectionKeys() {
        return new String[] {
                "Select Section",
                "Advertisement",
                "Festival Cards",
                "Latest Update",
                "Business Special",
                "Reel Maker",
                "Business Frame",
                "Motivation",
                "Greetings",
                "Business Ethics"
        };
    }

    // ✅ Helper: get canonical English key for currently selected section
    String getSelectedSectionKey() {
        int pos = spinnerSection.getSelectedItemPosition();
        String[] keys = getSectionKeys();
        return (pos >= 0 && pos < keys.length) ? keys[pos] : "";
    }

    // ✅ Canonical sub-section keys (for Business Frame)
    static final String[] SUB_SECTION_KEYS = { "Political", "NGO", "Business" };

    public UploadTemplatesFragment() {
        super(R.layout.fragment_upload_templates);
    }

    // ---------- Image Picker ----------
    ActivityResultLauncher<Intent> imagePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK
                        && result.getData() != null) {

                    Uri sourceUri = result.getData().getData();
                    if (sourceUri != null) {
                        // ✅ Use canonical key for logic comparison
                        String sectionKey = getSelectedSectionKey();
                        if (sectionKey.equalsIgnoreCase("Reel Maker")) {
                            selectedImageUri = sourceUri;
                            updatePreviewVisibility();
                            // Use Glide to show a thumbnail for the video
                            com.bumptech.glide.Glide.with(requireContext())
                                .asBitmap()
                                .load(selectedImageUri)
                                .into(previewImage);
                        } else if (sectionKey.equalsIgnoreCase("Business Frame")) {
                            // 🚫 SKIPPING MANDATORY UCROP FOR THESE SECTIONS
                            selectedImageUri = sourceUri;
                            updatePreviewVisibility();
                            previewImage.setImageURI(selectedImageUri);
                        } else {
                            originalImageUri = sourceUri;
                            startCrop(sourceUri);
                        }
                    }
                }
            });

    // Modern ActivityResultLauncher for UCrop
    ActivityResultLauncher<Intent> cropLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    final Uri resultUri = UCrop.getOutput(result.getData());
                    if (resultUri != null) {
                        selectedImageUri = resultUri;
                        updatePreviewVisibility();
                        previewImage.setImageURI(selectedImageUri);
                        toast(R.string.msg_crop_success);
                    }
                } else if (result.getResultCode() == UCrop.RESULT_ERROR && result.getData() != null) {
                    final Throwable cropError = UCrop.getError(result.getData());
                    if (cropError != null)
                        toast("Crop Error: " + cropError.getMessage());
                }
            });

    private void startCrop(@NonNull Uri uri) {
        // ✅ Use canonical key for aspect ratio decisions
        String sectionKey = getSelectedSectionKey();
        String destinationFileName = "cropped_" + System.currentTimeMillis() + ".jpg";
        UCrop uCrop = UCrop.of(uri, Uri.fromFile(new File(requireContext().getCacheDir(), destinationFileName)));

        if (sectionKey.equalsIgnoreCase("Advertisement")) {
            uCrop.withAspectRatio(16, 9);
        } else {
            uCrop.withAspectRatio(1, 1);
        }

        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(android.graphics.Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(90);
        options.setHideBottomControls(false);

        // Custom Styling to fix visibility and add space from screen edges
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
        // Use launcher instead of deprecated start()
        cropLauncher.launch(uCrop.getIntent(requireContext()));
    }

    boolean isPopulating = false;

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        super.onViewCreated(v, b);

        // Bind views
        spinnerSection = v.findViewById(R.id.spinnerSection);
        btnSelectImage = v.findViewById(R.id.btnSelectImage);
        btnSave = v.findViewById(R.id.btnSave);
        btnPickDate = v.findViewById(R.id.btnPickDate);
        previewImage = v.findViewById(R.id.previewImage);
        dateContainer = v.findViewById(R.id.dateContainer);
        subSectionContainer = v.findViewById(R.id.subSectionContainer);
        spinnerSubSection = v.findViewById(R.id.spinnerSubSection);
        editAdLink = v.findViewById(R.id.editAdLink);
        btnPickExpiry = v.findViewById(R.id.btnPickExpiry);
        placeholderLayout = v.findViewById(R.id.placeholderLayout);
        btnChangeMedia = v.findViewById(R.id.btnChangeMedia);
        icPlayVideo = v.findViewById(R.id.icPlayVideo);

        btnSelectImage.setAlpha(0.5f);
        updatePreviewVisibility();

        btnChangeMedia.setOnClickListener(v1 -> btnSelectImage.performClick());

        // Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                getSections());
        spinnerSection.setAdapter(adapter);

        spinnerSection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {

                // ✅ Use canonical key for comparison
                String selectedKey = getSectionKeys()[position];

                if (selectedKey.equalsIgnoreCase("Select Section")) {
                    btnSelectImage.setEnabled(false);
                    btnSelectImage.setAlpha(0.5f);
                } else {
                    btnSelectImage.setEnabled(true);
                    btnSelectImage.setAlpha(1.0f);
                }

                // 🧹 AUTO-CLEAR IMAGE ON SECTION CHANGE
                // This ensures old non-PNG images are removed when switching to 'Frame'
                if (!isPopulating && selectedImageUri != null) {
                    selectedImageUri = null;
                    originalImageUri = null;
                    updatePreviewVisibility();
                    toast(R.string.msg_reselect_section);
                }

                // 🛑 PREVENT RESET IF POPULATING
                if (isPopulating) {
                    if (selectedKey.equalsIgnoreCase("Festival Cards")) {
                        dateContainer.setVisibility(View.VISIBLE);
                        editAdLink.setVisibility(View.GONE);
                        subSectionContainer.setVisibility(View.GONE);
                    } else if (selectedKey.equalsIgnoreCase("Advertisement")) {
                        editAdLink.setVisibility(View.VISIBLE);
                        dateContainer.setVisibility(View.GONE);
                        subSectionContainer.setVisibility(View.GONE);
                    } else if (selectedKey.equalsIgnoreCase("Business Frame")) {
                        subSectionContainer.setVisibility(View.VISIBLE);
                        dateContainer.setVisibility(View.GONE);
                        editAdLink.setVisibility(View.GONE);
                        // ✅ Display localized sub-categories, but keys stay English
                        String[] subs = {
                                getString(R.string.cat_political),
                                getString(R.string.cat_ngo),
                                getString(R.string.cat_business)
                        };
                        spinnerSubSection.setAdapter(new ArrayAdapter<>(requireContext(),
                                android.R.layout.simple_spinner_dropdown_item, subs));
                    }
                    return;
                }

                if (selectedKey.equalsIgnoreCase("Festival Cards")) {

                    dateContainer.setVisibility(View.VISIBLE);
                    editAdLink.setVisibility(View.GONE);
                    subSectionContainer.setVisibility(View.GONE);

                } else if (selectedKey.equalsIgnoreCase("Advertisement")) {
                    editAdLink.setVisibility(View.VISIBLE);
                    dateContainer.setVisibility(View.GONE);
                    subSectionContainer.setVisibility(View.GONE);
                } else if (selectedKey.equalsIgnoreCase("Business Frame")) {
                    subSectionContainer.setVisibility(View.VISIBLE);
                    dateContainer.setVisibility(View.GONE);
                    editAdLink.setVisibility(View.GONE);
                    String[] subs = {
                            getString(R.string.cat_political),
                            getString(R.string.cat_ngo),
                            getString(R.string.cat_business)
                    };
                    spinnerSubSection.setAdapter(
                            new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, subs));
                } else {
                    dateContainer.setVisibility(View.GONE);
                    editAdLink.setVisibility(View.GONE);
                    subSectionContainer.setVisibility(View.GONE);
                    selectedDate = "";
                    btnPickDate.setText(R.string.btn_pick_date);
                    btnPickExpiry.setText(R.string.btn_set_expiry);
                    expiryTime = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // 🛠️ MOVED EDIT MODE LOGIC HERE (After Adapters & Listeners are Set)
        Bundle args = getArguments();
        if (args != null && args.containsKey("edit_url")) {
            isPopulating = true;
            isEditMode = true;
            oldUrl = args.getString("edit_url");
            oldCategory = args.getString("category", "");
            oldRealId = args.getString("realId", "");

            btnSave.setText(R.string.btn_update_template);

            if (!oldUrl.isEmpty()) {
                selectedImageUri = Uri.parse(oldUrl);
                updatePreviewVisibility();
                if (oldCategory.equalsIgnoreCase("Reel Maker")) {
                    com.bumptech.glide.Glide.with(this)
                            .asBitmap()
                            .load(oldUrl)
                            .frame(1000000) // Try at 1 second mark
                            .into(previewImage);
                } else {
                    com.bumptech.glide.Glide.with(this).load(oldUrl).into(previewImage);
                }
            }

            long passedExpiry = args.getLong("expiryDate", 0);
            if (passedExpiry > 0) {
                expiryTime = passedExpiry;
                String dStr = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.US) // ✅ ASCII digits always
                        .format(new java.util.Date(expiryTime));
                btnPickExpiry.setText(getString(R.string.label_expires, dStr));
            }

            String passedLink = args.getString("link", "");
            if (!passedLink.isEmpty())
                editAdLink.setText(passedLink);

            String passedDate = args.getString("date", "");
            if (!passedDate.isEmpty()) {
                selectedDate = passedDate;
                btnPickDate.setText(getString(R.string.label_selected, selectedDate));
            }

            String cat = args.getString("category", "");
            String sub = "";
            if (cat.contains("/")) {
                String[] parts = cat.split("/");
                cat = parts[0];
                sub = parts[1];
            }

            String[] keysList = getSectionKeys();
            for (int i = 0; i < keysList.length; i++) {
                // ✅ Match oldCategory (English from Firebase) against English section keys
                if (keysList[i].equalsIgnoreCase(cat)) {
                    spinnerSection.setSelection(i);
                    final String finalSub = sub;
                    spinnerSection.postDelayed(() -> {
                        if (!finalSub.isEmpty()) {
                            // ✅ Match sub against English canonical keys
                            for (int j = 0; j < SUB_SECTION_KEYS.length; j++) {
                                if (SUB_SECTION_KEYS[j].equalsIgnoreCase(finalSub)) {
                                    spinnerSubSection.setSelection(j);
                                    break;
                                }
                            }
                        }
                        isPopulating = false;
                        toast(R.string.msg_template_loaded);
                    }, 300);
                    break;
                }
            }
            // If not a section with sub-categories, we finish populating immediately
            if (!cat.equalsIgnoreCase("Business Frame")) {
                isPopulating = false;
                toast(R.string.msg_template_loaded);
            }
        }

        // Select Image (Dynamically switch between Image and Video)
        btnSelectImage.setOnClickListener(v1 -> {
            // ✅ Use canonical key
            String sectionKey = getSelectedSectionKey();
            Intent i;

            if (sectionKey.equalsIgnoreCase("Reel Maker")) {
                i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                i.setType("video/*");
            } else if (sectionKey.equalsIgnoreCase("Business Frame")) {
                // 🛡️ ACTION_GET_CONTENT is more reliable for strict MIME filtering in the
                // system picker
                i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("image/png");
                i.addCategory(Intent.CATEGORY_OPENABLE);
            } else {
                i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                i.setType("image/*");
                String[] mimeTypes = { "image/jpeg", "image/png", "image/jpg" };
                i.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            }

        imagePicker.launch(i);
        });

        // Also handle click on play button overlay
        icPlayVideo.setOnClickListener(v1 -> previewImage.performClick());

        // Preview image
        previewImage.setOnClickListener(v12 -> {
            // ✅ Use canonical key
            String sectionKey = getSelectedSectionKey();
            if (selectedImageUri != null) {
                if (sectionKey.equalsIgnoreCase("Reel Maker") ||
                        sectionKey.equalsIgnoreCase("Business Frame")) {

                    // These sections open full-screen preview instead of crop by default
                    // to avoid accidental transparency loss.
                    Intent i = new Intent(requireContext(), ImagePreviewActivity.class);
                    i.putExtra("img", selectedImageUri.toString());
                    i.putExtra("is_video", sectionKey.equalsIgnoreCase("Reel Maker"));
                    startActivity(i);
                } else {
                    // 🛡️ FIX: Only allow cropping local images for other sections
                    if (originalImageUri != null) {
                        startCrop(originalImageUri);
                    } else if (isEditMode) {
                        toast(R.string.msg_select_valid_img);
                    } else if (selectedImageUri != null && !selectedImageUri.toString().startsWith("http")) {
                        startCrop(selectedImageUri);
                    }
                }
            }
        });

        // Pick Date
        btnPickDate.setOnClickListener(v13 -> {
            Calendar cal = Calendar.getInstance();

            new DatePickerDialog(
                    requireContext(),
                    (view, year, month, day) -> {
                        month++;
                        selectedDate = day + "-" + month + "-" + year;
                        btnPickDate.setText(getString(R.string.label_selected, selectedDate));
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Pick Expiry Date
        btnPickExpiry.setOnClickListener(v1 -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(requireContext(), (view, year, month, day) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, day, 23, 59, 59);
                expiryTime = selected.getTimeInMillis();
                btnPickExpiry.setText(getString(R.string.label_expires, day + "-" + (month + 1) + "-" + year));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Save
        btnSave.setOnClickListener(v14 -> validateAndSave());
    }

    // ---------------- SAVE ----------------

    void validateAndSave() {

        if (selectedImageUri == null) {
            toast(R.string.msg_please_select_img);
            return;
        }

        // ✅ Use canonical key for all logic — NOT localized display string
        String sectionKey = getSelectedSectionKey();

        if (sectionKey.equals("Select Section")) {
            toast(R.string.msg_invalid_section);
            return;
        }

        if (sectionKey.equalsIgnoreCase("Advertisement")) {

            String link = editAdLink.getText().toString().trim();

            if (link.isEmpty()) {
                toast(R.string.msg_enter_ad_link);
                return;
            }
        }

        if (sectionKey.equalsIgnoreCase("Festival Cards") && selectedDate.isEmpty()) {
            toast(R.string.msg_select_fest_date);
            return;
        }

        // ---------- TYPE & SIZE VALIDATION ----------
        String mimeType = requireContext().getContentResolver().getType(selectedImageUri);
        String uriString = selectedImageUri.toString();

        if (sectionKey.equalsIgnoreCase("Reel Maker")) {
            // Video Validation
            if (mimeType == null && !uriString.contains(".mp4") && !uriString.contains(".mkv")
                    && !uriString.contains(".mov")) {
                toast(R.string.msg_select_valid_video);
                return;
            }

            // Check if Reel Maker video is 9:16 (vertical reel) or something similar.
            try {
                android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
                retriever.setDataSource(requireContext(), selectedImageUri);
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
                        toast("Video must be in vertical ratio (like 9:16) for Reel Maker");
                        return;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Image Validation: Check MIME OR check URI path
            String uriStr = selectedImageUri.toString().toLowerCase();
            boolean isImage = (mimeType != null && mimeType.startsWith("image/"))
                    || uriStr.contains(".jpg") || uriStr.contains(".jpeg") || uriStr.contains(".png");

            if (!isImage) {
                toast(R.string.msg_select_valid_img);
                return;
            }

            if (sectionKey.equalsIgnoreCase("Business Frame")) {
                // Strictly PNG for Frame sections
                boolean isPng = (mimeType != null && mimeType.contains("png")) || uriStr.contains(".png");
                if (!isPng) {
                    toast(R.string.msg_only_png_frames);
                    return;
                }
            } else {
                // For other sections, allow JPG/JPEG/PNG
                boolean validFormat = (mimeType != null && (mimeType.contains("jpeg") || mimeType.contains("jpg") || mimeType.contains("png")))
                        || uriStr.contains(".jpg") || uriStr.contains(".jpeg") || uriStr.contains(".png");

                if (!validFormat) {
                    toast(R.string.msg_format_supported);
                    return;
                }
            }

            // 📏 SQUARE ASPECT RATIO CHECK (For all except Ad and Video)
            if (!sectionKey.equalsIgnoreCase("Advertisement") && !sectionKey.equalsIgnoreCase("Reel Maker")) {
                try {
                    InputStream is = requireContext().getContentResolver().openInputStream(selectedImageUri);
                    android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
                    opts.inJustDecodeBounds = true;
                    android.graphics.BitmapFactory.decodeStream(is, null, opts);
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

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.title_confirm_upload)
                .setMessage(getString(R.string.msg_confirm_upload_format, sectionKey))
                .setPositiveButton(android.R.string.ok, (d, w) -> saveImage(sectionKey))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    void saveImage(String section) {
        // 🛡️ FIX: If in edit mode and image hasn't changed (it's still a web URL),
        // don't re-upload
        if (isEditMode && selectedImageUri != null && selectedImageUri.toString().startsWith("http")) {
            saveTemplateDetails(section, selectedImageUri.toString());
            return;
        }

        uploadImageToServer(selectedImageUri, new UploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                if (isAdded()) {
                    saveTemplateDetails(section, imageUrl);
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> toast(message));
                }
            }
        });
    }

    private void saveTemplateDetails(String section, String imageUrl) {
        if (isEditMode) {
            String newCategoryPath = section; // section here is already canonical English key
            if (section.equalsIgnoreCase("Business Frame")) {
                // ✅ sub-section: use canonical English key (index-based), not localized display
                // text
                int subPos = spinnerSubSection.getSelectedItemPosition();
                String subKey = (subPos >= 0 && subPos < SUB_SECTION_KEYS.length) ? SUB_SECTION_KEYS[subPos]
                        : spinnerSubSection.getSelectedItem().toString();
                newCategoryPath += "/" + subKey;
            }

            // 🎯 Only remove the old node if the location (category) or ID is changing.
            // If they are the same, Firebase's setValue() will simply update the existing
            // node.
            boolean locationChanged = !oldCategory.equals(newCategoryPath);

            if (locationChanged && !oldRealId.isEmpty() && !oldCategory.isEmpty()) {
                com.google.firebase.database.FirebaseDatabase.getInstance()
                        .getReference("templates")
                        .child(oldCategory)
                        .child(oldRealId)
                        .removeValue();
            }

            // Remove from old local list (always safest to refresh the list)
            removeFromLocal(oldUrl, oldCategory);

            // 🛡️ VPS & Stats Cleanup: Only if image URL is actually different
            if (!oldUrl.isEmpty() && !oldUrl.equals(imageUrl)) {
                deleteFromVPS(oldUrl);

                String safeKey = android.util.Base64.encodeToString(oldUrl.getBytes(), android.util.Base64.NO_WRAP);
                com.google.firebase.database.FirebaseDatabase.getInstance()
                        .getReference("template_activity")
                        .child(safeKey).removeValue();
            }
        }

        requireActivity().runOnUiThread(() -> {
            if (!isAdded()) return;

            SharedPreferences sp = requireContext().getSharedPreferences(
                    "HOME_DATA",
                    android.content.Context.MODE_PRIVATE);

            Gson gson = new Gson();
            DatabaseReference dbRef;
            if (section.equalsIgnoreCase("Business Frame")) {
                // ✅ Use canonical English key for sub-section
                int subPos = spinnerSubSection.getSelectedItemPosition();
                String subKey = (subPos >= 0 && subPos < SUB_SECTION_KEYS.length) ? SUB_SECTION_KEYS[subPos]
                        : spinnerSubSection.getSelectedItem().toString();
                dbRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                        .getReference("templates")
                        .child(section)
                        .child(subKey);
            } else {
                dbRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                        .getReference("templates")
                        .child(section);
            }

            // Generate Unique ID or use safe key
            String templateId;
            if (isEditMode) {
                templateId = (!oldRealId.isEmpty()) ? oldRealId
                        : android.util.Base64.encodeToString(oldUrl.getBytes(), android.util.Base64.NO_WRAP);
            } else {
                templateId = dbRef.push().getKey();
            }

            if (templateId == null)
                templateId = String.valueOf(System.currentTimeMillis());

            // =============================
            // 📢 ADVERTISEMENT
            // =============================
            if (section.equalsIgnoreCase("Advertisement")) {
                String link = editAdLink.getText().toString().trim();
                AdvertisementItem adItem = new AdvertisementItem(imageUrl, link, expiryTime, "Admin",
                        System.currentTimeMillis());
                adItem.id = templateId; // Important for redirection highlight

                // 1. Save to Firebase
                dbRef.child(templateId).setValue(adItem);

                // 2. Save to SharedPreferences (Backward Compatibility)
                Type t = new TypeToken<ArrayList<AdvertisementItem>>() {
                }.getType();
                ArrayList<AdvertisementItem> list = gson.fromJson(sp.getString("Advertisement", "[]"), t);
                if (list == null)
                    list = new ArrayList<>();
                list.add(0, adItem);
                sp.edit().putString("Advertisement", gson.toJson(list)).apply();

                toast(R.string.msg_upload_success);

                // 📢 SEND/UPDATE BROADCAST NOTIFICATION IF "Advertisement"
                NotificationHelper.sendBroadcast(
                        requireContext(),
                        templateId,
                        getString(R.string.title_notif_new_ad),
                        getString(R.string.msg_notif_new_ad),
                        "OPEN_AD",
                        templateId,
                        expiryTime);

                clearForm();
                return;
            }

            // =============================
            // 🎉 FESTIVAL
            // =============================
            if (section.equalsIgnoreCase("Festival Cards")) {
                FestivalCardItem festItem = new FestivalCardItem(imageUrl, selectedDate, expiryTime);

                // 1. Save to Firebase
                dbRef.child(templateId).setValue(festItem);

                // 2. Save to SharedPreferences
                Type t = new TypeToken<ArrayList<FestivalCardItem>>() {
                }.getType();
                ArrayList<FestivalCardItem> list = gson.fromJson(sp.getString(section, "[]"), t);
                if (list == null)
                    list = new ArrayList<>();
                list.add(0, festItem);
                sp.edit().putString(section, gson.toJson(list)).apply();

                toast(R.string.msg_upload_success);
                clearForm();
                return;
            }

            // =============================
            // 🧩 NORMAL SECTIONS / REELS
            // =============================
            java.util.Map<String, Object> normalItem = new java.util.HashMap<>();
            normalItem.put("url", imageUrl);
            normalItem.put("timestamp", System.currentTimeMillis());
            normalItem.put("expiryDate", expiryTime);
            if (section.equalsIgnoreCase("Reel Maker"))
                normalItem.put("type", "video");

            // 1. Save to Firebase
            dbRef.child(templateId).setValue(normalItem);

            // 2. Save to SharedPreferences
            Type type = new TypeToken<ArrayList<String>>() {
            }.getType();
            ArrayList<String> list = gson.fromJson(sp.getString(section, "[]"), type);
            if (list == null)
                list = new ArrayList<>();
            if (!list.contains(imageUrl))
                list.add(0, imageUrl);
            sp.edit().putString(section, gson.toJson(list)).apply();

            toast(R.string.msg_upload_success);

            // 📢 SEND/UPDATE BROADCAST NOTIFICATION IF "Latest Update"
            if (section.equalsIgnoreCase("Latest Update")) {
                NotificationHelper.sendBroadcast(
                        requireContext(),
                        templateId, // Use templateId as key to allow updates
                        getString(R.string.title_notif_latest_update),
                        getString(R.string.msg_check_latest_update),
                        "OPEN_TEMPLATE",
                        templateId,
                        expiryTime // Expiry matches template
                );
            }

            // 📢 SEND/UPDATE BROADCAST NOTIFICATION IF "Business Frame"
            if (section.equalsIgnoreCase("Business Frame")) {
                NotificationHelper.sendBroadcast(
                        requireContext(),
                        templateId,
                        getString(R.string.title_notif_business_frame),
                        getString(R.string.msg_notif_business_frame),
                        "OPEN_BUSINESS_FRAME",
                        templateId,
                        expiryTime);
            }

            clearForm();
        });
    }

    // ---------------- HELPERS ----------------

    void clearForm() {
        selectedImageUri = null;
        updatePreviewVisibility();
        selectedDate = "";
        btnPickDate.setText(R.string.btn_pick_date);
        btnSelectImage.setEnabled(false);
        btnSelectImage.setAlpha(0.5f);

        // RESET EDIT MODE
        isEditMode = false;
        oldUrl = "";
        oldCategory = "";
        btnSave.setText(R.string.btn_save_continue);
    }

    private void updatePreviewVisibility() {
        if (selectedImageUri != null) {
            placeholderLayout.setVisibility(View.GONE);
            previewImage.setVisibility(View.VISIBLE);
            btnChangeMedia.setVisibility(View.VISIBLE);

            String sectionKey = getSelectedSectionKey();
            if (sectionKey.equalsIgnoreCase("Reel Maker")) {
                icPlayVideo.setVisibility(View.VISIBLE);
            } else {
                icPlayVideo.setVisibility(View.GONE);
            }
        } else {
            placeholderLayout.setVisibility(View.VISIBLE);
            previewImage.setVisibility(View.GONE);
            btnChangeMedia.setVisibility(View.GONE);
            icPlayVideo.setVisibility(View.GONE);
        }
    }

    void toast(int resId) {
        if (isAdded()) {
            Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show();
        }
    }

    void toast(String msg) {
        if (isAdded()) {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------- NUCLEAR DELETE (FOR EDIT MODE) ----------------

    private void removeFromLocal(String url, String category) {
        SharedPreferences sp = requireContext().getSharedPreferences("HOME_DATA", android.content.Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sp.getString(category, null);
        if (json == null)
            return;

        SharedPreferences.Editor editor = sp.edit();
        if ("Festival Cards".equalsIgnoreCase(category)) {
            Type t = new TypeToken<ArrayList<FestivalCardItem>>() {
            }.getType();
            ArrayList<FestivalCardItem> list = gson.fromJson(json, t);
            if (list != null) {
                list.removeIf(item -> item.imagePath.equals(url));
                editor.putString(category, gson.toJson(list));
            }
        } else if ("Advertisement".equalsIgnoreCase(category)) {
            Type t = new TypeToken<ArrayList<AdvertisementItem>>() {
            }.getType();
            ArrayList<AdvertisementItem> list = gson.fromJson(json, t);
            if (list != null) {
                list.removeIf(item -> item.imagePath.equals(url));
                editor.putString(category, gson.toJson(list));
            }
        } else {
            Type t = new TypeToken<ArrayList<String>>() {
            }.getType();
            ArrayList<String> list = gson.fromJson(json, t);
            if (list != null) {
                list.remove(url);
                editor.putString(category, gson.toJson(list));
            }
        }
        editor.apply();
    }

    private void deleteFromVPS(String url) {
        new Thread(() -> {
            try {
                java.net.URL deleteUrl = new java.net.URL("http://187.77.184.84/delete.php");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) deleteUrl.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                String data = "url=" + java.net.URLEncoder.encode(url, "UTF-8");
                conn.getOutputStream().write(data.getBytes());
                conn.getResponseCode();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void uploadImageToServer(Uri imageUri, UploadCallback callback) {
        android.content.ContentResolver resolver = requireContext().getContentResolver();
        String sectionKey = getSelectedSectionKey();
        boolean isReel = sectionKey.equalsIgnoreCase("Reel Maker");
        long limit = (isReel ? 50L : 15L) * 1024 * 1024; // 50MB for reels as requested

        new Thread(() -> {
            try {
                // 1. Get File Size
                long fileSize = -1;
                try (android.content.res.AssetFileDescriptor afd = resolver.openAssetFileDescriptor(imageUri, "r")) {
                    if (afd != null) fileSize = afd.getLength();
                } catch (Exception ignored) {}

                if (fileSize > limit) {
                    callback.onError("File is too big. Max limit is " + (limit / 1024 / 1024) + "MB");
                    return;
                }

                String mimeType = resolver.getType(imageUri);
                if (mimeType == null) mimeType = isReel ? "video/mp4" : "image/jpeg";

                // 2. Connection Settings
                String boundary = "----RMPLUS" + System.currentTimeMillis();
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL("http://187.77.184.84/upload.php").openConnection();
                conn.setConnectTimeout(60000);
                conn.setReadTimeout(180000); // 3 mins for response
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setUseCaches(false);
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                // Multipart overhead calculation
                String ext = ".jpg";
                if (mimeType.contains("png")) ext = ".png";
                else if (mimeType.contains("mp4")) ext = ".mp4";
                else if (mimeType.contains("webm")) ext = ".webm";
                else if (mimeType.contains("mkv")) ext = ".mkv";
                else if (mimeType.contains("mov")) ext = ".mov";

                String head = "--" + boundary + "\r\n" +
                              "Content-Disposition: form-data; name=\"file\"; filename=\"up_" + System.currentTimeMillis() + ext + "\"\r\n" +
                              "Content-Type: " + mimeType + "\r\n\r\n";
                String tail = "\r\n--" + boundary + "--\r\n";

                byte[] headBytes = head.getBytes("UTF-8");
                byte[] tailBytes = tail.getBytes("UTF-8");

                if (fileSize > 0) {
                    conn.setFixedLengthStreamingMode(headBytes.length + fileSize + tailBytes.length);
                } else {
                    conn.setChunkedStreamingMode(16384);
                }

                // 3. Write Data
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

                // 4. Parse Response
                int responseCode = conn.getResponseCode();
                InputStream responseStream = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                
                StringBuilder sb = new StringBuilder();
                if (responseStream != null) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(responseStream));
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                }

                String rawResponse = sb.toString().trim();
                if (responseCode == 200 && !rawResponse.isEmpty()) {
                    try {
                        org.json.JSONObject json = new org.json.JSONObject(rawResponse);
                        if (json.has("status") && "success".equals(json.getString("status"))) {
                            callback.onSuccess(json.getString("url"));
                        } else {
                            // SHOW FULL SERVER MESSAGE OR RAW RESPONSE
                            String msg = json.optString("message", rawResponse);
                            callback.onError("Server Rejected: " + msg);
                        }
                    } catch (org.json.JSONException e) {
                        callback.onError("Server logic error or file too large for server config. Response: " + rawResponse);
                    }
                } else {
                    callback.onError("Server Error " + responseCode + ": " + (rawResponse.isEmpty() ? "No response" : rawResponse));
                }

            } catch (Exception e) {
                e.printStackTrace();
                callback.onError("Upload Error: " + e.getLocalizedMessage());
            }
        }).start();
    }

    interface UploadCallback {
        void onSuccess(String imageUrl);

        void onError(String message);
    }
}
