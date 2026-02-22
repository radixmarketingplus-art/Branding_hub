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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class UploadTemplatesActivity extends BaseActivity {

    Spinner spinnerSection;
    Button btnSelectImage, btnSave;
    Uri selectedImageUri;
    LinearLayout dateContainer;
    ImageView previewImage;
    Button btnPickDate;
    String selectedDate = "";
    EditText editAdLink;   // NEW


    String[] sections = {
            "Select Section",
            "Advertisement",
            "Festival Cards",
            "Latest Update",
            "Business Special",
            "Reel Maker",
            "Business Frame",
            "Motivation",
            "Good Morning",
            "Business Ethics"
    };

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
        btnSelectImage = findViewById(R.id.btnSelectImage);
        previewImage = findViewById(R.id.previewImage);
        btnSave = findViewById(R.id.btnSave);
        dateContainer = findViewById(R.id.dateContainer);
        btnPickDate = findViewById(R.id.btnPickDate);
        editAdLink = findViewById(R.id.editAdLink);

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (view, insets) -> {

            // ❌ bottom inset consume mat karo
            view.setPadding(
                    view.getPaddingLeft(),
                    view.getPaddingTop(),
                    view.getPaddingRight(),
                    0
            );

            return insets;
        });

        // Spinner adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                sections
        );
        spinnerSection.setAdapter(adapter);

        spinnerSection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();

                if (selected.equalsIgnoreCase("Festival Cards")) {
                    dateContainer.setVisibility(View.VISIBLE);
                    editAdLink.setVisibility(View.GONE);

                } else if (selected.equalsIgnoreCase("Advertisement")) {

                    editAdLink.setVisibility(View.VISIBLE);
                    dateContainer.setVisibility(View.GONE);

                } else {
                    dateContainer.setVisibility(View.GONE);
                    editAdLink.setVisibility(View.GONE);
                    selectedDate = "";
                    btnPickDate.setText("Pick Date");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Select image
        btnSelectImage.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_PICK);
            i.setType("image/*");
            startActivityForResult(i, 101);
        });

        // Preview Image
        previewImage.setOnClickListener(v -> {

            if (selectedImageUri != null) {
                Intent i = new Intent(
                        UploadTemplatesActivity.this,
                        ImagePreviewActivity.class
                );
                i.putExtra("img", selectedImageUri.toString());
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
                        btnPickDate.setText("Selected: " + selectedDate);
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });

        // Save image
        btnSave.setOnClickListener(v -> {

            if (selectedImageUri == null) {
                Toast.makeText(this, "Please select image", Toast.LENGTH_SHORT).show();
                return;
            }

            String section = spinnerSection.getSelectedItem().toString();

            if (section.equals("Select Section")) {
                Toast.makeText(this, "Please select a valid section", Toast.LENGTH_SHORT).show();
                return;
            }

            if (section.equalsIgnoreCase("Festival Cards") && selectedDate.isEmpty()) {
                Toast.makeText(this, "Please select festival date", Toast.LENGTH_SHORT).show();
                return;
            }

            if (section.equalsIgnoreCase("Advertisement")) {

                String link = editAdLink.getText().toString().trim();

                if (link.isEmpty()) {
                    Toast.makeText(this,
                            "Enter advertisement link",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }


            new AlertDialog.Builder(this)
                    .setTitle("Confirm Upload")
                    .setMessage("Save image to \"" + section + "\" section?")
                    .setPositiveButton("OK", (d, w) -> saveImage(section))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {

            selectedImageUri = data.getData();

            if (selectedImageUri != null) {

                previewImage.setVisibility(View.VISIBLE);
                previewImage.setImageURI(selectedImageUri);

                Toast.makeText(this, "Image Selected", Toast.LENGTH_SHORT).show();
            }
        }
    }

    void saveImage(String section) {

        if (section.equalsIgnoreCase("Advertisement")) {

            String link = editAdLink.getText().toString().trim();

            SharedPreferences sp =
                    getSharedPreferences("HOME_DATA", MODE_PRIVATE);

            Gson gson = new Gson();

            ArrayList<AdvertisementItem> list;
            String oldJson = sp.getString("Advertisement", null);

            Type t = new TypeToken<ArrayList<AdvertisementItem>>(){}.getType();
            list = oldJson == null ? new ArrayList<>() : gson.fromJson(oldJson, t);

            String localPath = saveImageToInternalStorage(selectedImageUri);

            if (localPath == null) {
                Toast.makeText(this, "Image save failed", Toast.LENGTH_SHORT).show();
                return;
            }

            list.add(0, new AdvertisementItem(localPath, link));

            sp.edit().putString("Advertisement", gson.toJson(list)).apply();

            Toast.makeText(this,
                    "Advertisement saved",
                    Toast.LENGTH_SHORT).show();

            finish();
            return;
        }


        // ==== NORMALIZE SECTION KEYS (VERY IMPORTANT) ====
//        if (section.equalsIgnoreCase("Trending Now")) {
//            section = "Trending Now"; // force exact key
//        }

        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select image", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sp = getSharedPreferences("HOME_DATA", MODE_PRIVATE);
        Gson gson = new Gson();

        if (section.equalsIgnoreCase("Festival Cards")) {

            ArrayList<FestivalCardItem> list;
            String oldJson = sp.getString("Festival Cards", null);

            try {
                Type t = new TypeToken<ArrayList<FestivalCardItem>>() {}.getType();
                list = oldJson == null ? new ArrayList<>() : gson.fromJson(oldJson, t);
            } catch (Exception e) {
                list = new ArrayList<>();
            }

            String localPath = saveImageToInternalStorage(selectedImageUri);
            if (localPath == null) {
                Toast.makeText(this, "Image save failed", Toast.LENGTH_SHORT).show();
                return;
            }

            list.add(0, new FestivalCardItem(localPath, selectedDate));

            sp.edit().putString("Festival Cards", gson.toJson(list)).apply();
            Toast.makeText(this, "Festival card saved", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ArrayList<String> list;
        String oldJson = sp.getString(section, null);

        if (oldJson != null) {
            Type type = new TypeToken<ArrayList<String>>() {}.getType();
            list = gson.fromJson(oldJson, type);
        } else {
            list = new ArrayList<>();
        }

        String localPath = saveImageToInternalStorage(selectedImageUri);
        if (localPath == null) {
            Toast.makeText(this, "Image save failed", Toast.LENGTH_SHORT).show();
            return;
        }
        if(!list.contains(localPath)){
            list.add(0, localPath);
        }
        sp.edit().putString(section, gson.toJson(list)).apply();

        Toast.makeText(this, "Saved to Home Page", Toast.LENGTH_SHORT).show();

        setResult(RESULT_OK);
        finish();
    }

    private String saveImageToInternalStorage(Uri sourceUri) {
        try {
            String fileName = "img_" + System.currentTimeMillis() + ".jpg";

            InputStream input = getContentResolver().openInputStream(sourceUri);
            if (input == null) return null;

            File file = new File(getFilesDir(), fileName);
            FileOutputStream output = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }

            input.close();
            output.flush();
            output.close();

            return file.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}