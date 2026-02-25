package com.example.rmplus;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UserDetailActivity extends AppCompatActivity {

    private String uid;
    private DatabaseReference userRef;

    private EditText etName, etDesignation, etDob, etEmail, etMobile;
    private ShapeableImageView imgProfile;
    private FloatingActionButton btnUploadImg;
    private TextView btnRemoveImg, txtUid;
    private Spinner countryCodePicker, stateSpinner, citySpinner, roleSpinner;
    private RadioGroup genderGroup;
    private RadioButton rbMale, rbFemale, rbOther;
    private Button btnSave;

    private String profileUrl = "";
    private Uri croppedUri;
    private String[] countryCodes = {"+91", "+1", "+44", "+971", "+61", "+234", "+92", "+880", "+94"};
    private String[] indiaStates;
    private String[] roles = {"user", "admin"};
    private Map<String, Integer> cityMapRes = new HashMap<>();
    private String[] englishCities = new String[0];
    private String initialCity = "";

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cropLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_detail);

        uid = getIntent().getStringExtra("uid");
        if (uid == null) {
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        initViews();
        setupToolbar();
        setupSpinners();
        setupActivityResultLaunchers();
        loadUserData();

        etDob.setOnClickListener(v -> showDatePicker());
        btnSave.setOnClickListener(v -> confirmSave());
        btnUploadImg.setOnClickListener(v -> pickImage());
        btnRemoveImg.setOnClickListener(v -> removeImage());
        imgProfile.setOnClickListener(v -> showFullImage());
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etDesignation = findViewById(R.id.etDesignation);
        etDob = findViewById(R.id.etDob);
        etEmail = findViewById(R.id.etEmail);
        etMobile = findViewById(R.id.etMobile);
        imgProfile = findViewById(R.id.imgProfile);
        btnUploadImg = findViewById(R.id.btnUploadImg);
        btnRemoveImg = findViewById(R.id.btnRemoveImg);
        txtUid = findViewById(R.id.txtUid);
        countryCodePicker = findViewById(R.id.countryCodePicker);
        stateSpinner = findViewById(R.id.stateSpinner);
        citySpinner = findViewById(R.id.citySpinner);
        roleSpinner = findViewById(R.id.roleSpinner);
        genderGroup = findViewById(R.id.genderGroup);
        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);
        rbOther = findViewById(R.id.rbOther);
        btnSave = findViewById(R.id.btnSave);

        txtUid.setText("UID: " + uid);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        // Country Code
        ArrayAdapter<String> cpAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_country_code, countryCodes);
        cpAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        countryCodePicker.setAdapter(cpAdapter);

        // Roles
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles);
        roleSpinner.setAdapter(roleAdapter);

        // States
        indiaStates = getResources().getStringArray(R.array.india_states);
        ArrayAdapter<String> stateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, indiaStates);
        stateSpinner.setAdapter(stateAdapter);

        initializeCityMap();

        stateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                updateCitySpinner(indiaStates[position]);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupActivityResultLaunchers() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        startCrop(result.getData().getData());
                    }
                }
        );

        cropLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri resultUri = UCrop.getOutput(result.getData());
                        if (resultUri != null) {
                            croppedUri = resultUri;
                            imgProfile.setImageURI(null);
                            imgProfile.setImageURI(resultUri);
                            btnRemoveImg.setVisibility(View.VISIBLE);
                        }
                    }
                }
        );
    }

    private void loadUserData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                if (s.exists()) {
                    etName.setText(getValue(s, "name"));
                    etDesignation.setText(getValue(s, "designation"));
                    etDob.setText(getValue(s, "dob"));
                    etEmail.setText(getValue(s, "email"));

                    String fullMobile = getValue(s, "mobile");
                    if (!fullMobile.isEmpty()) {
                        boolean found = false;
                        for (int i = 0; i < countryCodes.length; i++) {
                            if (fullMobile.startsWith(countryCodes[i])) {
                                countryCodePicker.setSelection(i);
                                etMobile.setText(fullMobile.substring(countryCodes[i].length()));
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            countryCodePicker.setSelection(0);
                            etMobile.setText(fullMobile);
                        }
                    }

                    initialCity = getValue(s, "city");
                    String savedState = getValue(s, "state");
                    if (!savedState.isEmpty()) {
                        String[] englishStates = getContextForLocale("en").getResources().getStringArray(R.array.india_states);
                        for (int i = 0; i < englishStates.length; i++) {
                            if (englishStates[i].equals(savedState)) {
                                stateSpinner.setSelection(i);
                                break;
                            }
                        }
                    }

                    String gender = getValue(s, "gender");
                    if (gender.equalsIgnoreCase("Male")) rbMale.setChecked(true);
                    else if (gender.equalsIgnoreCase("Female")) rbFemale.setChecked(true);
                    else rbOther.setChecked(true);

                    String role = getValue(s, "role");
                    if (role != null && !role.isEmpty()) {
                        for (int i = 0; i < roles.length; i++) {
                            if (roles[i].equalsIgnoreCase(role)) {
                                roleSpinner.setSelection(i);
                                break;
                            }
                        }
                    }

                    profileUrl = getValue(s, "profileImage");
                    if (!profileUrl.isEmpty()) {
                        Glide.with(UserDetailActivity.this)
                                .load(profileUrl)
                                .placeholder(R.drawable.ic_profile)
                                .error(R.drawable.ic_profile)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(imgProfile);
                        btnRemoveImg.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String getValue(DataSnapshot s, String key) {
        if (s.child(key).exists()) {
            Object val = s.child(key).getValue();
            return val != null ? val.toString() : "";
        }
        return "";
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog picker = new DatePickerDialog(this, (view, y, m, d) -> {
            String selectedDate = String.format(Locale.US, "%02d/%02d/%d", d, (m + 1), y);
            etDob.setText(selectedDate);
        }, year, month, day);
        picker.show();
    }

    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(i);
    }

    private void startCrop(Uri uri) {
        String dest = "Profile_" + System.currentTimeMillis() + ".jpg";
        UCrop uCrop = UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), dest)));
        uCrop.withAspectRatio(1, 1);
        cropLauncher.launch(uCrop.getIntent(this));
    }

    private void removeImage() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.link_remove_img)
                .setMessage(R.string.msg_confirm_remove_img)
                .setPositiveButton(R.string.yes, (d, w) -> {
                    profileUrl = "";
                    croppedUri = null;
                    imgProfile.setImageResource(R.drawable.ic_profile);
                    btnRemoveImg.setVisibility(View.GONE);
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void showFullImage() {
        AppCompatDialog dialog = new AppCompatDialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView fullImg = new ImageView(this);
        if (!profileUrl.isEmpty()) {
            Glide.with(this).load(profileUrl).placeholder(R.drawable.ic_profile).into(fullImg);
        } else if (croppedUri != null) {
            fullImg.setImageURI(croppedUri);
        } else {
            fullImg.setImageResource(R.drawable.ic_profile);
        }
        fullImg.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(fullImg);
        dialog.show();
    }

    private void confirmSave() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.btn_save_changes)
                .setMessage("Are you sure you want to save these changes?")
                .setPositiveButton(R.string.yes, (d, w) -> {
                    if (croppedUri != null) {
                        btnSave.setEnabled(false);
                        btnSave.setText(R.string.msg_uploading_wait);
                        uploadImageToVPS(croppedUri);
                    } else {
                        saveUserData();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void uploadImageToVPS(Uri uri) {
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
                while ((len = input.read(buffer)) != -1) out.write(buffer, 0, len);
                input.close();

                out.writeBytes("\r\n--" + boundary + "--\r\n");
                out.flush();
                out.close();

                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                StringBuilder res = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) res.append(line);
                reader.close();

                org.json.JSONObject json = new org.json.JSONObject(res.toString());
                profileUrl = json.getString("url");

                runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    btnSave.setText(R.string.btn_save_changes);
                    saveUserData();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    btnSave.setText(R.string.btn_save_changes);
                    Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void saveUserData() {
        String name = etName.getText().toString();
        String designation = etDesignation.getText().toString();
        String dob = etDob.getText().toString();
        String email = etEmail.getText().toString();
        String mobile = countryCodePicker.getSelectedItem().toString() + etMobile.getText().toString();

        String[] englishStates = getContextForLocale("en").getResources().getStringArray(R.array.india_states);
        String state = englishStates[stateSpinner.getSelectedItemPosition()];

        int cityPos = citySpinner.getSelectedItemPosition();
        String city = (englishCities != null && cityPos >= 0 && cityPos < englishCities.length)
                ? englishCities[cityPos]
                : (citySpinner.getSelectedItem() != null ? citySpinner.getSelectedItem().toString() : "");

        String gender = "Other";
        if (rbMale.isChecked()) gender = "Male";
        else if (rbFemale.isChecked()) gender = "Female";

        String role = roles[roleSpinner.getSelectedItemPosition()];

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("designation", designation);
        updates.put("dob", dob);
        updates.put("email", email);
        updates.put("mobile", mobile);
        updates.put("state", state);
        updates.put("city", city);
        updates.put("gender", gender);
        updates.put("role", role);
        updates.put("profileImage", profileUrl);

        userRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(UserDetailActivity.this, "User updated successfully", Toast.LENGTH_SHORT).show();
                NotificationHelper.send(this, uid, "Profile Updated by Admin", "Your profile has been updated by an administrator.");
                finish();
            } else {
                Toast.makeText(UserDetailActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
            }
        });
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
        String[] allEnglishStates = getContextForLocale("en").getResources().getStringArray(R.array.india_states);
        String stateKey = allEnglishStates[stateSpinner.getSelectedItemPosition()];

        if (cityMapRes.containsKey(stateKey)) {
            englishCities = getContextForLocale("en").getResources().getStringArray(cityMapRes.get(stateKey));
        } else {
            englishCities = new String[]{"Other"};
        }

        String[] displayCities;
        if (cityMapRes.containsKey(stateKey)) {
            displayCities = getResources().getStringArray(cityMapRes.get(stateKey));
        } else {
            displayCities = new String[]{"Other"};
        }

        Arrays.sort(englishCities);
        Arrays.sort(displayCities);

        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, displayCities);
        citySpinner.setAdapter(cityAdapter);

        if (!initialCity.isEmpty()) {
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
