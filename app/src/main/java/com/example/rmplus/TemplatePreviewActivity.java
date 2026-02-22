package com.example.rmplus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class TemplatePreviewActivity extends AppCompatActivity {

    ImageView img;
    ImageButton btnLike, btnShare, btnEdit, btnSave;
    ImageButton btnSearch, btnFav;
    RecyclerView rvSimilar;

    String path;
    String category;
    String templateId;
    String uid;

    DatabaseReference rootRef;

    boolean isLiked = false;
    boolean isFav = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_template_preview);

        img = findViewById(R.id.imgPreview);
        btnLike = findViewById(R.id.btnLike);
        btnShare = findViewById(R.id.btnShare);
        btnEdit = findViewById(R.id.btnEdit);
        btnSearch = findViewById(R.id.btnSearch);
        btnFav = findViewById(R.id.btnFav);
        btnSave = findViewById(R.id.btnSave);
        rvSimilar = findViewById(R.id.rvSimilar);

        uid = FirebaseAuth.getInstance().getUid();

        path = getIntent().getStringExtra("path");

        if(path == null){
            path = getIntent().getStringExtra("uri"); // fallback safety
        }

        if(path == null){
            finish();
            return;
        }

        category = getIntent().getStringExtra("category");

        templateId = makeSafeKey(path);
        rootRef = FirebaseDatabase.getInstance().getReference();

        // MAIN IMAGE
        img.setImageURI(Uri.fromFile(new File(path)));

        // FULL SCREEN PREVIEW
        img.setOnClickListener(v -> {
            Intent i = new Intent(this, ImagePreviewActivity.class);
            i.putExtra("img", path);
            startActivity(i);
        });

        loadLikeStatus();
        loadFavoriteStatus();
        loadSimilarTemplates();

        // SEARCH
        btnSearch.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));

        // LIKE
        btnLike.setOnClickListener(v -> toggleLike());

        // FAVORITE
        btnFav.setOnClickListener(v -> toggleFavorite());

        // EDIT
        btnEdit.setOnClickListener(v -> {

            rootRef.child("template_activity")
                    .child(templateId)
                    .child("edits")
                    .child(uid)
                    .setValue(true);

            rootRef.child("user_activity")
                    .child(uid)
                    .child("edits")
                    .child(templateId)
                    .setValue(path);

            Intent i = new Intent(this, ManageTemplatesActivity.class);
            i.putExtra("uri", path);
            startActivity(i);
        });

        // SHARE
        btnShare.setOnClickListener(v -> shareImage());

        // SAVE
        btnSave.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Save Template")
                        .setMessage("Do you really want to save?")
                        .setPositiveButton("Yes", (d, w) -> saveImage())
                        .setNegativeButton("No", null)
                        .show());
    }

    // =====================================================
    // SIMILAR TEMPLATES
    // =====================================================

    void loadSimilarTemplates() {

        if (category == null) return;

        SharedPreferences sp =
                getSharedPreferences("HOME_DATA", MODE_PRIVATE);

        Gson gson = new Gson();
        ArrayList<String> list = new ArrayList<>();

        if (category.equals("Festival Cards")) {

            String json = sp.getString(category, null);
            if (json == null) return;

            Type t = new TypeToken<ArrayList<FestivalCardItem>>(){}.getType();
            ArrayList<FestivalCardItem> festivalList =
                    gson.fromJson(json, t);

            for (FestivalCardItem f : festivalList) {
                if (!f.imagePath.equals(path))
                    list.add(f.imagePath);
            }

        } else {

            String json = sp.getString(category, null);
            if (json == null) return;

            Type t = new TypeToken<ArrayList<String>>(){}.getType();
            ArrayList<String> images = gson.fromJson(json, t);

            for (String p : images) {
                if (!p.equals(path))
                    list.add(p);
            }
        }

        rvSimilar.setLayoutManager(
                new GridLayoutManager(this, 3)
        );

        TemplateGridAdapter adapter =
                new TemplateGridAdapter(list, p -> {

                    Intent i = new Intent(
                            TemplatePreviewActivity.this,
                            TemplatePreviewActivity.class);

                    i.putExtra("path", p);
                    i.putExtra("category", category);
                    startActivity(i);
                    finish();
                });

        rvSimilar.setAdapter(adapter);
    }

    // =====================================================
    // LIKE STATUS
    // =====================================================

    void loadLikeStatus() {
        rootRef.child("template_activity")
                .child(templateId)
                .child("likes")
                .child(uid)
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            public void onDataChange(DataSnapshot s) {
                                if (s.exists()) {
                                    isLiked = true;
                                    btnLike.setColorFilter(Color.RED);
                                }
                            }
                            public void onCancelled(DatabaseError e) {}
                        });
    }

    void loadFavoriteStatus() {
        rootRef.child("template_activity")
                .child(templateId)
                .child("favorites")
                .child(uid)
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            public void onDataChange(DataSnapshot s) {
                                if (s.exists()) {
                                    isFav = true;
                                    btnFav.setColorFilter(Color.YELLOW);
                                }
                            }
                            public void onCancelled(DatabaseError e) {}
                        });
    }

    // =====================================================

    void toggleLike() {

        isLiked = !isLiked;

        if (isLiked) {
            btnLike.setColorFilter(Color.RED);

            rootRef.child("template_activity")
                    .child(templateId)
                    .child("likes")
                    .child(uid)
                    .setValue(true);

        } else {
            btnLike.clearColorFilter();

            rootRef.child("template_activity")
                    .child(templateId)
                    .child("likes")
                    .child(uid)
                    .removeValue();
        }
    }

    void toggleFavorite() {

        isFav = !isFav;

        if (isFav) {
            btnFav.setColorFilter(Color.YELLOW);

            rootRef.child("template_activity")
                    .child(templateId)
                    .child("favorites")
                    .child(uid)
                    .setValue(true);

        } else {
            btnFav.clearColorFilter();

            rootRef.child("template_activity")
                    .child(templateId)
                    .child("favorites")
                    .child(uid)
                    .removeValue();
        }
    }

    // =====================================================

    void shareImage() {

        File file = new File(path);

        Uri uri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".provider",
                file);

        Intent s = new Intent(Intent.ACTION_SEND);
        s.setType("image/*");
        s.putExtra(Intent.EXTRA_STREAM, uri);
        s.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(s, "Share"));
    }

    // =====================================================

    void saveImage() {

        Bitmap bitmap =
                BitmapFactory.decodeFile(path);

        String url =
                MediaStore.Images.Media.insertImage(
                        getContentResolver(),
                        bitmap,
                        "RMPlus_" + System.currentTimeMillis(),
                        "Template");

        if (url != null) {

            Toast.makeText(this,
                    "Saved to Gallery",
                    Toast.LENGTH_SHORT).show();

            rootRef.child("template_activity")
                    .child(templateId)
                    .child("saves")
                    .child(uid)
                    .setValue(true);
        }
    }

    // =====================================================

    private String makeSafeKey(String path) {
        return Base64.encodeToString(
                path.getBytes(),
                Base64.NO_WRAP);
    }
}
