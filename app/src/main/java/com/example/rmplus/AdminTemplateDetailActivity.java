package com.example.rmplus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class AdminTemplateDetailActivity extends AppCompatActivity {

    ImageView imgPreview;
    TextView txtCategory;

    TextView txtLikeCount, txtFavCount, txtEditCount, txtSaveCount;

    String templatePath;
    String category;
    String templateKey;
    View statsLayout;        // whole stats section
    String adLink = null;


    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_admin_template_detail);

        templatePath = getIntent().getStringExtra("path");
        category = getIntent().getStringExtra("category");
        txtLikeCount = findViewById(R.id.txtLikeCount);
        txtFavCount  = findViewById(R.id.txtFavCount);
        txtEditCount = findViewById(R.id.txtEditCount);
        txtSaveCount = findViewById(R.id.txtSaveCount);

        // 🔐 SAME KEY USED EVERYWHERE
        templateKey = Base64.encodeToString(
                templatePath.getBytes(),
                Base64.NO_WRAP
        );

        imgPreview = findViewById(R.id.imgPreview);
        txtCategory = findViewById(R.id.txtCategory);

        findViewById(R.id.btnLike).setOnClickListener(v -> openStats("likes"));
        findViewById(R.id.btnFav).setOnClickListener(v -> openStats("favorites"));
        findViewById(R.id.btnEdit).setOnClickListener(v -> openStats("edits"));
        findViewById(R.id.btnSave).setOnClickListener(v -> openStats("saves"));

        statsLayout = findViewById(R.id.layout_admin_stats);
        MaterialButton btnOpenAd = findViewById(R.id.btnOpenAd);


        imgPreview.setImageURI(Uri.fromFile(new File(templatePath)));
        txtCategory.setText(category);

        // ==================================================
        // 📢 ADVERTISEMENT MODE
        // ==================================================

        if ("Advertisement".equalsIgnoreCase(category)) {

            // Hide stats section
            statsLayout.setVisibility(View.GONE);

            // Show open button
            btnOpenAd.setVisibility(View.VISIBLE);

            // Load link from SharedPreferences
            loadAdvertisementLink();

            btnOpenAd.setOnClickListener(v -> {

                if (adLink == null || adLink.isEmpty()) return;

                Intent i = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(adLink)
                );

                startActivity(i);
            });

        } else {

            // Normal templates
            btnOpenAd.setVisibility(View.GONE);

            setupClicks();
            loadStats();
        }


        findViewById(R.id.btnSearch).setOnClickListener(v -> {
            startActivity(new Intent(
                    AdminTemplateDetailActivity.this,
                    SearchActivity.class
            ));
        });


        setupClicks();
        loadStats();
    }

    void loadAdvertisementLink() {

        SharedPreferences sp =
                getSharedPreferences("HOME_DATA", MODE_PRIVATE);

        String json = sp.getString("Advertisement", null);

        if (json == null) return;

        Gson gson = new Gson();

        Type t =
                new TypeToken<ArrayList<AdvertisementItem>>(){}.getType();

        ArrayList<AdvertisementItem> list =
                gson.fromJson(json, t);

        if (list == null) return;

        for (AdvertisementItem ad : list) {

            if (ad.imagePath.equals(templatePath)) {
                adLink = ad.link;
                break;
            }
        }
    }


    // ---------------- BUTTON CLICKS ----------------

    void setupClicks() {

        findViewById(R.id.btnLike).setOnClickListener(v ->
                openStats("likes"));

        findViewById(R.id.btnFav).setOnClickListener(v ->
                openStats("favorites"));

        findViewById(R.id.btnEdit).setOnClickListener(v ->
                openStats("edits"));

        findViewById(R.id.btnSave).setOnClickListener(v ->
                openStats("saves"));
    }

    void openStats(String tab) {
        Intent i = new Intent(this, StatsDetailActivity.class);
        i.putExtra("path", templatePath);
        i.putExtra("defaultTab", tab);
        startActivity(i);
    }

    // ---------------- LOAD COUNTS ----------------

    void loadStats() {

        DatabaseReference baseRef =
                FirebaseDatabase.getInstance()
                        .getReference("template_activity")
                        .child(templateKey);

        baseRef.child("likes").get()
                .addOnSuccessListener(s ->
                        txtLikeCount.setText(
                                String.valueOf(s.getChildrenCount())
                        ));

        baseRef.child("favorites").get()
                .addOnSuccessListener(s ->
                        txtFavCount.setText(
                                String.valueOf(s.getChildrenCount())
                        ));

        baseRef.child("edits").get()
                .addOnSuccessListener(s ->
                        txtEditCount.setText(
                                String.valueOf(s.getChildrenCount())
                        ));

        baseRef.child("saves").get()
                .addOnSuccessListener(s ->
                        txtSaveCount.setText(
                                String.valueOf(s.getChildrenCount())
                        ));
    }

}
