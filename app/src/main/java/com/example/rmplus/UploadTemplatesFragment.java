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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class UploadTemplatesFragment extends Fragment {

    Spinner spinnerSection;
    MaterialButton btnSelectImage, btnSave, btnPickDate;
    ImageView previewImage;
    LinearLayout dateContainer;

    Uri selectedImageUri;
    String selectedDate = "";
    EditText editAdLink;   // NEW


    String[] sections = {
            "Select Section",
            "Advertisement",   // ✅ NEW
            "Festival Cards",
            "Latest Update",
            "Business Special",
            "Reel Maker",
            "Business Frame",
            "Motivation",
            "Good Morning",
            "Business Ethics"
    };

    public UploadTemplatesFragment() {
        super(R.layout.fragment_upload_templates);
    }

    // ---------- Image Picker ----------
    ActivityResultLauncher<Intent> imagePicker =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == android.app.Activity.RESULT_OK
                                && result.getData() != null) {

                            selectedImageUri = result.getData().getData();
                            if (selectedImageUri != null) {
                                previewImage.setVisibility(View.VISIBLE);
                                previewImage.setImageURI(selectedImageUri);
                                Toast.makeText(requireContext(),
                                        "Image Selected", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

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
        editAdLink = v.findViewById(R.id.editAdLink);

        // Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                sections
        );
        spinnerSection.setAdapter(adapter);

        spinnerSection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {

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

        // Select Image
        btnSelectImage.setOnClickListener(v1 -> {
            Intent i = new Intent(Intent.ACTION_PICK);
            i.setType("image/*");
            imagePicker.launch(i);
        });

        // Preview image
        previewImage.setOnClickListener(v12 -> {
            if (selectedImageUri != null) {
                Intent i = new Intent(requireContext(),
                        ImagePreviewActivity.class);
                i.putExtra("img", selectedImageUri.toString());
                startActivity(i);
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
                        btnPickDate.setText("Selected: " + selectedDate);
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        // Save
        btnSave.setOnClickListener(v14 -> validateAndSave());
    }

    // ---------------- SAVE ----------------

    void validateAndSave() {

        if (selectedImageUri == null) {
            toast("Please select image");
            return;
        }

        String section = spinnerSection.getSelectedItem().toString();

        if (section.equals("Select Section")) {
            toast("Please select a valid section");
            return;
        }

        if (section.equalsIgnoreCase("Advertisement")) {

            String link = editAdLink.getText().toString().trim();

            if (link.isEmpty()) {
                toast("Enter advertisement link");
                return;
            }
        }


        if (section.equalsIgnoreCase("Festival Cards") && selectedDate.isEmpty()) {
            toast("Please select festival date");
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Upload")
                .setMessage("Save image to \"" + section + "\" section?")
                .setPositiveButton("OK", (d, w) -> saveImage(section))
                .setNegativeButton("Cancel", null)
                .show();
    }

    void saveImage(String section) {

        SharedPreferences sp =
                requireContext().getSharedPreferences("HOME_DATA",
                        requireContext().MODE_PRIVATE);

        Gson gson = new Gson();
        String localPath = saveImageToInternalStorage(selectedImageUri);

        if (localPath == null) {
            toast("Image save failed");
            return;
        }

        // ==================================================
        // 📢 ADVERTISEMENT SAVE
        // ==================================================

        if (section.equalsIgnoreCase("Advertisement")) {

            String link = editAdLink.getText().toString().trim();

            Type t =
                    new TypeToken<ArrayList<AdvertisementItem>>(){}.getType();

            ArrayList<AdvertisementItem> list =
                    gson.fromJson(sp.getString("Advertisement", "[]"), t);

            if (list == null) list = new ArrayList<>();

            list.add(0, new AdvertisementItem(localPath, link));

            sp.edit().putString("Advertisement",
                    gson.toJson(list)).apply();

            toast("Advertisement saved");
            clearForm();
            return;
        }

        // ==================================================
        // 🎉 FESTIVAL SAVE
        // ==================================================

        if (section.equalsIgnoreCase("Festival Cards")) {

            Type t =
                    new TypeToken<ArrayList<FestivalCardItem>>(){}.getType();

            ArrayList<FestivalCardItem> list =
                    gson.fromJson(sp.getString(section, "[]"), t);

            if (list == null) list = new ArrayList<>();

            list.add(0, new FestivalCardItem(localPath, selectedDate));

            sp.edit().putString(section, gson.toJson(list)).apply();
            toast("Festival card saved");
            clearForm();
            return;
        }

        // ==================================================
        // 🧩 NORMAL SECTIONS
        // ==================================================

        Type type =
                new TypeToken<ArrayList<String>>(){}.getType();

        ArrayList<String> list =
                gson.fromJson(sp.getString(section, "[]"), type);

        if (list == null) list = new ArrayList<>();

        if (!list.contains(localPath)) {
            list.add(0, localPath);
        }

        sp.edit().putString(section, gson.toJson(list)).apply();
        toast("Saved to Home Page");
        clearForm();
    }

    // ---------------- HELPERS ----------------

    void clearForm() {
        previewImage.setVisibility(View.GONE);
        selectedImageUri = null;
        selectedDate = "";
        spinnerSection.setSelection(0);
        btnPickDate.setText("Pick Date");
    }

    void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    String saveImageToInternalStorage(Uri sourceUri) {
        try {
            String name = "img_" + System.currentTimeMillis() + ".jpg";
            InputStream in =
                    requireContext().getContentResolver()
                            .openInputStream(sourceUri);

            if (in == null) return null;

            File file =
                    new File(requireContext().getFilesDir(), name);

            FileOutputStream out = new FileOutputStream(file);

            byte[] buf = new byte[4096];
            int r;
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
            }

            in.close();
            out.close();

            return file.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
