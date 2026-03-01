package com.example.rmplus;

import android.os.Bundle;
import android.view.View;
import android.graphics.Bitmap;
import android.widget.*;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.app.AlertDialog;
import android.app.DatePickerDialog;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import android.content.res.Configuration;
import androidx.core.os.ConfigurationCompat;

import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.File;
import java.io.InputStream;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.yalantis.ucrop.UCrop;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.example.rmplus.NotificationHelper;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class EditProfileActivity extends AppCompatActivity {

    EditText name, designation, dob, email, mobile;
    Spinner stateSpinner, citySpinner;
    RadioGroup genderGroup;
    RadioButton male, female, other;
    Button saveBtn;
    Spinner countryCodePicker;
    ImageView profileImg;
    FloatingActionButton btnUploadImg;
    TextView btnRemoveImg;
    String profileUrl = "";
    Uri imgUri; // original picked image
    Uri croppedUri; // locally cropped — uploaded only on save
    String[] countryCodes = { "+91", "+1", "+44", "+971", "+61", "+234", "+92", "+880", "+94" };
    String[] indiaStates;
    Map<String, Integer> cityMapRes = new HashMap<>();
    Map<String, String[]> cityMap = new HashMap<>();
    String initialCity = "";
    // ✅ English canonical city names — used for DB save (independent of locale)
    String[] englishCities = new String[0];

    DatabaseReference userRef;

    String oldName, oldDesignation, oldDob, oldMobile, oldCity,
            oldState, oldGender, initialProfileUrl = "";

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cropLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        name = findViewById(R.id.name);
        designation = findViewById(R.id.designation);
        dob = findViewById(R.id.dob);
        email = findViewById(R.id.email);
        mobile = findViewById(R.id.mobile);
        citySpinner = findViewById(R.id.citySpinner);
        stateSpinner = findViewById(R.id.stateSpinner);

        profileImg = findViewById(R.id.profileImg);
        btnUploadImg = findViewById(R.id.btnUploadImg);
        btnRemoveImg = findViewById(R.id.btnRemoveImg);

        genderGroup = findViewById(R.id.genderGroup);
        male = findViewById(R.id.male);
        female = findViewById(R.id.female);
        other = findViewById(R.id.other);

        countryCodePicker = findViewById(R.id.countryCodePicker);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item_country_code, countryCodes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        countryCodePicker.setAdapter(adapter);

        indiaStates = getResources().getStringArray(R.array.india_states);
        ArrayAdapter<String> stateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                indiaStates);
        stateSpinner.setAdapter(stateAdapter);

        initializeCityMap();

        stateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                updateCitySpinner(indiaStates[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        saveBtn = findViewById(R.id.saveBtn);

        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null) {
            Toast.makeText(this, R.string.user_not_logged_in, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid);

        loadData();

        dob.setOnClickListener(v -> showDatePicker());
        // Also handle the touch on the icon
        dob.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (dob.getRight() - dob.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width()
                        - dob.getPaddingRight())) {
                    showDatePicker();
                    return true;
                }
            }
            return false;
        });

        saveBtn.setOnClickListener(v -> confirmSave());

        btnUploadImg.setOnClickListener(v -> pickImage());
        btnRemoveImg.setOnClickListener(v -> removeImage());

        profileImg.setOnClickListener(v -> onProfileImageClick());

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        setupActivityResultLaunchers();
    }

    private void setupActivityResultLaunchers() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imgUri = result.getData().getData(); // ✅ Store source for re-cropping
                        startCrop(imgUri);
                    }
                });

        cropLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri resultUri = UCrop.getOutput(result.getData());
                        if (resultUri != null) {
                            // ✅ Store locally — NO VPS upload here (Deferred until Save)
                            croppedUri = resultUri;
                            Glide.with(EditProfileActivity.this)
                                    .load(resultUri)
                                    .placeholder(R.drawable.ic_profile)
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .skipMemoryCache(true)
                                    .into(profileImg);
                            
                            btnRemoveImg.setVisibility(View.VISIBLE);
                            Toast.makeText(this, R.string.msg_img_uploaded, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void onProfileImageClick() {
        if (imgUri != null) {
            // ✅ Case: User picked a new image but hasn't saved yet. 
            // Re-open crop screen so they can fix any mistakes.
            startCrop(imgUri);
        } else if (profileUrl != null && !profileUrl.isEmpty()) {
            // ✅ Case: Image is already saved in DB. Show full screen preview.
            showFullImage();
        }
    }

    private void showFullImage() {
        AppCompatDialog dialog = new AppCompatDialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView fullImg = new ImageView(this);

        // ✅ Check for unsaved cropped image first, then for existing profile URL
        Object imageSource = null;
        if (croppedUri != null) {
            imageSource = croppedUri;
        } else if (profileUrl != null && !profileUrl.isEmpty()) {
            imageSource = profileUrl;
        }

        if (imageSource != null) {
            Glide.with(this)
                    .load(imageSource)
                    .placeholder(R.drawable.ic_profile)
                    .into(fullImg);
        } else {
            fullImg.setImageResource(R.drawable.ic_profile);
        }

        fullImg.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(fullImg);
        dialog.show();
    }

    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(i);
    }

    private void removeImage() {
        if ((profileUrl == null || profileUrl.isEmpty()) && croppedUri == null)
            return;

        new AlertDialog.Builder(this)
                .setTitle(R.string.link_remove_img)
                .setMessage(R.string.msg_confirm_remove_img)
                .setPositiveButton(R.string.yes, (d, w) -> {
                    // ✅ ONLY update local state. NO Firebase/VPS calls until Save button is clicked.
                    profileUrl = "";
                    croppedUri = null;
                    imgUri = null; // Clear local picked state
                    profileImg.setImageResource(R.drawable.ic_profile);
                    btnRemoveImg.setVisibility(View.GONE);
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void deleteImageFromVPS(String url) {
        new Thread(() -> {
            try {
                java.net.URL deleteUrl = new java.net.URL("http://187.77.184.84/delete.php");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) deleteUrl.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String data = "url=" + java.net.URLEncoder.encode(url, "UTF-8");
                java.io.OutputStream out = conn.getOutputStream();
                out.write(data.getBytes());
                out.flush();
                out.close();

                int code = conn.getResponseCode();
                runOnUiThread(() -> {
                    if (code == 200) {
                        Toast.makeText(this, R.string.msg_img_deleted_server, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int rc, int res, Intent data) {
        super.onActivityResult(rc, res, data);
    }

    private void startCrop(Uri uri) {
        String dest = "Profile_" + System.currentTimeMillis() + ".jpg";
        UCrop uCrop = UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), dest)));
        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(90);
        uCrop.withOptions(options);
        uCrop.withAspectRatio(1, 1); // Square crop

        Intent cropIntent = uCrop.getIntent(this);
        cropLauncher.launch(cropIntent);
    }

    private void uploadImageToVPS(Uri uri) {
        uploadImageToVPS(uri, null);
    }

    interface VpsUploadCallback {
        void onDone(String url);
    }

    private void uploadImageToVPS(Uri uri, VpsUploadCallback callback) {
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
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"profile.jpg\"\r\n");
                out.writeBytes("Content-Type: image/jpeg\r\n\r\n");

                InputStream input = getContentResolver().openInputStream(uri);
                byte[] buffer = new byte[4096];
                int len;
                while ((len = input.read(buffer)) != -1)
                    out.write(buffer, 0, len);
                input.close();

                out.writeBytes("\r\n--" + boundary + "--\r\n");
                out.flush();
                out.close();

                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream()));
                StringBuilder res = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                    res.append(line);
                reader.close();

                org.json.JSONObject json = new org.json.JSONObject(res.toString());
                String uploadedUrl = json.getString("url");

                runOnUiThread(() -> {
                    if (callback != null) {
                        callback.onDone(uploadedUrl);
                    } else {
                        profileUrl = uploadedUrl;
                        btnRemoveImg.setVisibility(View.VISIBLE);
                        Toast.makeText(this, R.string.msg_img_uploaded, Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.msg_upload_failed, Toast.LENGTH_SHORT).show();
                    if (callback != null) {
                        callback.onDone(null); // Indicate failure
                    }
                });
            }
        }).start();
    }

    private void showDatePicker() {
        // Set Localized Calendar
        Locale currentLocale = ConfigurationCompat.getLocales(getResources().getConfiguration()).get(0);
        if (currentLocale == null)
            currentLocale = Locale.getDefault();
        Locale.setDefault(currentLocale);

        final Calendar c = Calendar.getInstance(currentLocale);
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog picker = new DatePickerDialog(this, (view, y, m, d) -> {
            String selectedDate = String.format(Locale.US, "%02d/%02d/%d", d, (m + 1), y);
            dob.setText(selectedDate);
        }, year, month, day);
        picker.show();
    }

    // -----------------------------

    void loadData() {

        userRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot s) {

                        name.setText(getValue(s, "name"));
                        designation.setText(getValue(s, "designation"));
                        dob.setText(getValue(s, "dob"));
                        email.setText(getValue(s, "email"));

                        String fullMobile = getValue(s, "mobile");
                        if (!fullMobile.isEmpty()) {
                            boolean found = false;
                            for (int i = 0; i < countryCodes.length; i++) {
                                if (fullMobile.startsWith(countryCodes[i])) {
                                    countryCodePicker.setSelection(i);
                                    mobile.setText(fullMobile.substring(countryCodes[i].length()));
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                countryCodePicker.setSelection(0); // Default to +91
                                mobile.setText(fullMobile);
                            }
                        } else {
                            countryCodePicker.setSelection(0); // Default to +91
                            mobile.setText("");
                        }

                        initialCity = getValue(s, "city");

                        String savedState = getValue(s, "state");
                        if (!savedState.isEmpty()) {
                            // Find state by canonical (English) name comparison or index
                            // To be safe, we check both current and english list
                            String[] englishStates = getContextForLocale("en").getResources()
                                    .getStringArray(R.array.india_states);
                            for (int i = 0; i < englishStates.length; i++) {
                                if (englishStates[i].equals(savedState)) {
                                    stateSpinner.setSelection(i);
                                    break;
                                }
                            }
                        }

                        profileUrl = getValue(s, "profileImage");
                        initialProfileUrl = profileUrl; // Store original for deferred deletion
                        if (!profileUrl.isEmpty()) {
                            Glide.with(EditProfileActivity.this)
                                    .load(profileUrl)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .placeholder(R.drawable.ic_profile)
                                    .into(profileImg);
                            btnRemoveImg.setVisibility(View.VISIBLE);
                        } else {
                            profileImg.setImageResource(R.drawable.ic_profile);
                            btnRemoveImg.setVisibility(View.GONE);
                        }

                        oldName = getValue(s, "name");
                        oldDesignation = getValue(s, "designation");
                        oldDob = getValue(s, "dob");
                        oldMobile = getValue(s, "mobile");
                        oldCity = getValue(s, "city");
                        oldState = getValue(s, "state");
                        oldGender = getValue(s, "gender");

                        if (oldGender.equals("Male"))
                            male.setChecked(true);
                        else if (oldGender.equals("Female"))
                            female.setChecked(true);
                        else
                            other.setChecked(true);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                    }
                });
    }

    String getValue(DataSnapshot s, String key) {
        if (s.child(key).exists())
            return s.child(key).getValue(String.class);
        return "";
    }

    // -----------------------------

    void confirmSave() {

        new AlertDialog.Builder(this)
                .setTitle(R.string.btn_save_changes)
                .setMessage(R.string.msg_confirm_save_profile)
                .setPositiveButton(R.string.yes, (d, w) -> {
                    if (croppedUri != null) {
                        // ✅ Upload to VPS only NOW (on save)
                        saveBtn.setEnabled(false);
                        saveBtn.setText(R.string.msg_uploading_wait);
                        uploadImageToVPS(croppedUri, uploadedUrl -> {
                            profileUrl = uploadedUrl;
                            croppedUri = null;
                            runOnUiThread(() -> {
                                saveBtn.setEnabled(true);
                                saveBtn.setText(R.string.btn_save_changes);
                                saveProfile();
                            });
                        });
                    } else {
                        saveProfile();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    // -----------------------------

    void saveProfile() {

        String newName = name.getText().toString();
        String newDesignation = designation.getText().toString();
        String newDob = dob.getText().toString();
        String newMobile = countryCodePicker.getSelectedItem().toString() + mobile.getText().toString();
        // For saving to DB, we prefer canonical (English) names to keep data consistent
        String[] englishStates = getContextForLocale("en").getResources().getStringArray(R.array.india_states);
        String newState = englishStates[stateSpinner.getSelectedItemPosition()];

        // ✅ Always save the canonical English city name (not the translated display
        // name)
        int cityPos = citySpinner.getSelectedItemPosition();
        String newCity = (englishCities != null && cityPos >= 0 && cityPos < englishCities.length)
                ? englishCities[cityPos]
                : (citySpinner.getSelectedItem() != null ? citySpinner.getSelectedItem().toString() : "");

        String gender = "Other";
        if (male.isChecked())
            gender = "Male";
        if (female.isChecked())
            gender = "Female";

        userRef.child("name").setValue(newName);
        userRef.child("designation").setValue(newDesignation);
        userRef.child("dob").setValue(newDob);
        userRef.child("mobile").setValue(newMobile);
        userRef.child("city").setValue(newCity);
        userRef.child("state").setValue(newState);
        userRef.child("gender").setValue(gender);

        // ✅ If the image was changed or removed, delete the OLD image from VPS
        if (!initialProfileUrl.isEmpty() && !initialProfileUrl.equals(profileUrl)) {
            deleteImageFromVPS(initialProfileUrl);
            initialProfileUrl = profileUrl; // Update original to new state
        }

        userRef.child("profileImage").setValue(profileUrl);

        Toast.makeText(this, R.string.msg_profile_updated, Toast.LENGTH_SHORT).show();
        NotificationHelper.send(
                EditProfileActivity.this,
                FirebaseAuth.getInstance().getUid(),
                "Profile Updated",
                "Your profile information has been successfully updated.");

        finish();
    }

    private android.content.Context getContextForLocale(String lang) {
        Configuration conf = new Configuration(getResources().getConfiguration());
        conf.setLocale(new Locale(lang));
        return createConfigurationContext(conf);
    }

    private void initializeCityMap() {
        cityMapRes.put("Andhra Pradesh", R.array.cities_andhra_pradesh);
        cityMapRes.put("Arunachal Pradesh", R.array.cities_arunachal_pradesh);
        cityMapRes.put("Assam", R.array.cities_assam);
        cityMapRes.put("Bihar", R.array.cities_bihar);
        cityMapRes.put("Chhattisgarh", R.array.cities_chhattisgarh);
        cityMapRes.put("Goa", R.array.cities_goa);
        cityMapRes.put("Gujarat", R.array.cities_gujarat);
        cityMapRes.put("Haryana", R.array.cities_haryana);
        cityMapRes.put("Himachal Pradesh", R.array.cities_himachal_pradesh);
        cityMapRes.put("Jharkhand", R.array.cities_jharkhand);
        cityMapRes.put("Karnataka", R.array.cities_karnataka);
        cityMapRes.put("Kerala", R.array.cities_kerala);
        cityMapRes.put("Madhya Pradesh", R.array.cities_mp);
        cityMapRes.put("Maharashtra", R.array.cities_maharashtra);
        cityMapRes.put("Manipur", R.array.cities_manipur);
        cityMapRes.put("Meghalaya", R.array.cities_meghalaya);
        cityMapRes.put("Mizoram", R.array.cities_mizoram);
        cityMapRes.put("Nagaland", R.array.cities_nagaland);
        cityMapRes.put("Odisha", R.array.cities_odisha);
        cityMapRes.put("Punjab", R.array.cities_punjab);
        cityMapRes.put("Rajasthan", R.array.cities_rajasthan);
        cityMapRes.put("Sikkim", R.array.cities_sikkim);
        cityMapRes.put("Tamil Nadu", R.array.cities_tamil_nadu);
        cityMapRes.put("Telangana", R.array.cities_telangana);
        cityMapRes.put("Tripura", R.array.cities_tripura);
        cityMapRes.put("Uttar Pradesh", R.array.cities_up);
        cityMapRes.put("Uttarakhand", R.array.cities_uttarakhand);
        cityMapRes.put("West Bengal", R.array.cities_west_bengal);
        cityMapRes.put("Andaman and Nicobar Islands", R.array.cities_andaman);
        cityMapRes.put("Chandigarh", R.array.cities_chandigarh);
        cityMapRes.put("Dadra and Nagar Haveli and Daman and Diu", R.array.cities_dadra);
        cityMapRes.put("Delhi", R.array.cities_delhi);
        cityMapRes.put("Jammu and Kashmir", R.array.cities_jammu_kashmir);
        cityMapRes.put("Ladakh", R.array.cities_ladakh);
        cityMapRes.put("Lakshadweep", R.array.cities_lakshadweep);
        cityMapRes.put("Puducherry", R.array.cities_puducherry);
    }

    private void updateCitySpinner(String stateLabel) {
        // Get the English name of the selected state to find resource
        String[] allEnglishStates = getContextForLocale("en").getResources().getStringArray(R.array.india_states);
        String stateKey = allEnglishStates[stateSpinner.getSelectedItemPosition()];

        // ✅ Always load English canonical city names first (for DB save)
        if (cityMapRes.containsKey(stateKey)) {
            englishCities = getContextForLocale("en").getResources().getStringArray(cityMapRes.get(stateKey));
        } else {
            englishCities = new String[] { "Other" };
        }

        // Displayed cities = localized (Hindi or English based on current locale)
        String[] displayCities;
        if (cityMapRes.containsKey(stateKey)) {
            displayCities = getResources().getStringArray(cityMapRes.get(stateKey));
        } else {
            displayCities = new String[] { getString(R.string.cat_other) };
        }

        // Sort both arrays together by English name (keeps them in sync)
        // Simple approach: sort English, then reorder display accordingly
        // Since array-resource order is fixed, just sort each separately — they share
        // same order
        Arrays.sort(englishCities);
        Arrays.sort(displayCities);

        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                displayCities);
        citySpinner.setAdapter(cityAdapter);

        if (!initialCity.isEmpty()) {
            // ✅ Match initialCity (stored as English in DB) against englishCities array
            for (int i = 0; i < englishCities.length; i++) {
                if (englishCities[i].equals(initialCity)) {
                    citySpinner.setSelection(i);
                    break;
                }
            }
            initialCity = "";
        }
    }
}
