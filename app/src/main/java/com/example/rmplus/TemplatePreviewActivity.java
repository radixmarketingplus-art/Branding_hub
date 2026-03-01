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

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import android.widget.FrameLayout;

public class TemplatePreviewActivity extends AppCompatActivity {

    ImageView img;
    ImageButton btnLike, btnShare, btnEdit, btnSave;
    ImageButton btnSearch, btnFav;
    ImageView imgPlayIcon;
    android.widget.VideoView videoView;
    RecyclerView rvSimilar;

    String path;
    String category;
    String templateId;
    String uid;
    FrameLayout previewContainer;

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
        imgPlayIcon = findViewById(R.id.imgPlayIcon);
        videoView = findViewById(R.id.vPreview);
        rvSimilar = findViewById(R.id.rvSimilar);
        previewContainer = findViewById(R.id.previewContainer);

        uid = FirebaseAuth.getInstance().getUid();

        templateId = getIntent().getStringExtra("id");
        category = getIntent().getStringExtra("category");
        path = getIntent().getStringExtra("path");

        if (templateId == null && path != null) {
            templateId = makeSafeKey(path); // fallback for legacy references
        }

        if (templateId == null) {
            finish();
            return;
        }

        rootRef = FirebaseDatabase.getInstance().getReference();

        // If path is missing or category is "MyDesign", fetch details or find true category
        if (path == null || "MyDesign".equalsIgnoreCase(category)) {
            fetchTemplateDetails();
        } else {
            initUI();
        }
    }

    void fetchTemplateDetails() {
        if ("MyDesign".equalsIgnoreCase(category) || category == null) {
            // SEARCH all categories for the template to find its true category
            rootRef.child("templates").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean found = false;
                    for (DataSnapshot catSnap : snapshot.getChildren()) {
                        if (catSnap.hasChild(templateId)) {
                            category = catSnap.getKey();
                            found = true;
                            DataSnapshot item = catSnap.child(templateId);
                            if (path == null) {
                                path = item.hasChild("imagePath") ? item.child("imagePath").getValue(String.class) : item.child("url").getValue(String.class);
                            }
                        } else {
                            // Search sub-categories
                            for (DataSnapshot subSnap : catSnap.getChildren()) {
                                if (subSnap.hasChild(templateId)) {
                                    category = catSnap.getKey() + "/" + subSnap.getKey();
                                    found = true;
                                    DataSnapshot item = subSnap.child(templateId);
                                    if (path == null) {
                                        path = item.hasChild("imagePath") ? item.child("imagePath").getValue(String.class) : item.child("url").getValue(String.class);
                                    }
                                    break;
                                }
                            }
                        }
                        if (found) break;
                    }

                    if (found || path != null) {
                        initUI();
                    } else {
                        finish();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (path != null) initUI(); else finish();
                }
            });
        } else {
            // Direct fetch if category is known
            rootRef.child("templates").child(category).child(templateId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot s) {
                            if (s.exists()) {
                                if (path == null) {
                                    path = s.hasChild("imagePath") ? s.child("imagePath").getValue(String.class) : s.child("url").getValue(String.class);
                                }
                                initUI();
                            } else {
                                finish();
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError e) {
                            if (path != null) initUI(); else finish();
                        }
                    });
        }
    }

    void initUI() {
        // Detect Video
        boolean isVideo = "Reel Maker".equalsIgnoreCase(category) || 
                          (path != null && (path.toLowerCase().endsWith(".mp4") || path.toLowerCase().endsWith(".webm")));

        if (isVideo) {
            imgPlayIcon.setVisibility(android.view.View.VISIBLE);
        } else {
            imgPlayIcon.setVisibility(android.view.View.GONE);
        }

        // MAIN IMAGE
        Glide.with(this)
                .load(path)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(img);

        // FULL SCREEN PREVIEW / VIDEO PLAY
        img.setOnClickListener(v -> {
            if (isVideo) {
                playVideo();
            } else {
                Intent i = new Intent(this, ImagePreviewActivity.class);
                i.putExtra("img", path);
                startActivity(i);
            }
        });

        imgPlayIcon.setOnClickListener(v -> playVideo());

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

            if (isVideo) {
                toast(getString(R.string.msg_video_edit_not_supported));
                return;
            }

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
                        .setTitle(R.string.title_save_template)
                        .setMessage(R.string.msg_save_confirm)
                        .setPositiveButton(R.string.yes, (d, w) -> saveImage())
                        .setNegativeButton(R.string.no, null)
                        .show());
    }

    // =====================================================
    // SIMILAR TEMPLATES
    // =====================================================

    void loadSimilarTemplates() {

        if (category == null) return;

        // Fetch directly from Firebase as requested
        rootRef.child("templates").child(category)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        ArrayList<TemplateModel> list = new ArrayList<>();
                        for (DataSnapshot d : snapshot.getChildren()) {
                            String itemPath = null;
                            if (d.hasChild("imagePath")) {
                                itemPath = d.child("imagePath").getValue(String.class);
                            } else if (d.hasChild("url")) {
                                itemPath = d.child("url").getValue(String.class);
                            }

                            if (itemPath != null && !d.getKey().equals(templateId)) {
                                list.add(new TemplateModel(d.getKey(), itemPath, category));
                            }
                        }

                        rvSimilar.setLayoutManager(new GridLayoutManager(TemplatePreviewActivity.this, 3));
                        TemplateGridAdapter adapter = new TemplateGridAdapter(list, t -> {
                            Intent i = new Intent(TemplatePreviewActivity.this, TemplatePreviewActivity.class);
                            i.putExtra("id", t.id);
                            i.putExtra("path", t.url);
                            i.putExtra("category", category);
                            startActivity(i);
                            finish();
                        });
                        rvSimilar.setAdapter(adapter);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
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
 
            rootRef.child("user_activity")
                    .child(uid)
                    .child("likes")
                    .child(templateId)
                    .setValue(path);

        } else {
            btnLike.clearColorFilter();

            rootRef.child("template_activity")
                    .child(templateId)
                    .child("likes")
                    .child(uid)
                    .removeValue();
 
            rootRef.child("user_activity")
                    .child(uid)
                    .child("likes")
                    .child(templateId)
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
 
            rootRef.child("user_activity")
                    .child(uid)
                    .child("favorites")
                    .child(templateId)
                    .setValue(path);

        } else {
            btnFav.clearColorFilter();

            rootRef.child("template_activity")
                    .child(templateId)
                    .child("favorites")
                    .child(uid)
                    .removeValue();
 
            rootRef.child("user_activity")
                    .child(uid)
                    .child("favorites")
                    .child(templateId)
                    .removeValue();
        }
    }

    // =====================================================

    void shareImage() {
        if (isVideo()) {
            toast(getString(R.string.msg_sharing_video_link));
            Intent s = new Intent(Intent.ACTION_SEND);
            s.setType("text/plain");
            s.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_msg_body, getString(R.string.app_name), getPackageName()) + "\n\n" + path);
            startActivity(Intent.createChooser(s, getString(R.string.title_share_video)));
            return;
        }

        Bitmap bitmap = Bitmap.createBitmap(previewContainer.getWidth(), previewContainer.getHeight(), Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        previewContainer.draw(canvas);

        new Thread(() -> {
            try {
                File file = new File(getCacheDir(), "share_" + System.currentTimeMillis() + ".jpg");
                java.io.FileOutputStream out = new java.io.FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                out.close();

                Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);

                runOnUiThread(() -> {
                    Intent s = new Intent(Intent.ACTION_SEND);
                    s.setType("image/*");
                    s.putExtra(Intent.EXTRA_STREAM, uri);
                    s.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(s, getString(R.string.content_desc_share)));
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    // =====================================================

    void saveImage() {
        if (isVideo()) {
            toast(getString(R.string.msg_video_download_not_supported));
            return;
        }

        Bitmap bitmap = Bitmap.createBitmap(previewContainer.getWidth(), previewContainer.getHeight(), Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        previewContainer.draw(canvas);

        new Thread(() -> {
            try {
                String saved = MediaStore.Images.Media.insertImage(
                        getContentResolver(), bitmap, "RMPlus_" + System.currentTimeMillis(), "Template");

                runOnUiThread(() -> {
                    if (saved != null) {
                        toast(getString(R.string.msg_saved_to_gallery));
                        rootRef.child("template_activity").child(templateId).child("saves").child(uid).setValue(true);
                        rootRef.child("user_activity").child(uid).child("saves").child(templateId).setValue(path);
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    // =====================================================

    private String makeSafeKey(String path) {
        return Base64.encodeToString(
                path.getBytes(),
                Base64.NO_WRAP);
    }

    private void playVideo() {
        if (path == null) return;
        videoView.setVisibility(android.view.View.VISIBLE);
        img.setVisibility(android.view.View.GONE);
        imgPlayIcon.setVisibility(android.view.View.GONE);

        android.widget.MediaController mc = new android.widget.MediaController(this);
        mc.setAnchorView(videoView);
        videoView.setMediaController(mc);
        videoView.setVideoURI(Uri.parse(path));
        videoView.start();
    }

    private boolean isVideo() {
        return "Reel Maker".equalsIgnoreCase(category) || 
               (path != null && (path.toLowerCase().endsWith(".mp4") || path.toLowerCase().endsWith(".webm")));
    }


    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }
}
