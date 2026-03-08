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
import android.widget.TextView;
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
    View btnLike, btnShare, btnSave, btnFav;
    com.google.android.material.button.MaterialButton btnEdit;
    ImageView btnSearch, btnBack;
    ImageView imgLike, imgFav;
    View layPlay, previewBottomShadow;
    android.widget.VideoView videoView;
    RecyclerView rvSimilar;
    TextView txtCategory;
    ViewGroup.LayoutParams params;

    FrameLayout progressOverlay;
    TextView txtProgressTitle, txtProgressSub;

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
        layPlay = findViewById(R.id.layPlay);
        previewBottomShadow = findViewById(R.id.previewBottomShadow);
        imgLike = findViewById(R.id.imgLike);
        imgFav = findViewById(R.id.imgFav);
        videoView = findViewById(R.id.vPreview);
        rvSimilar = findViewById(R.id.rvSimilar);
        previewContainer = findViewById(R.id.previewContainer);
        txtCategory = findViewById(R.id.txtCategory);
        btnBack = findViewById(R.id.btnBack);
        params = previewContainer.getLayoutParams();

        progressOverlay = findViewById(R.id.progressOverlay);
        txtProgressTitle = findViewById(R.id.txtProgressTitle);
        txtProgressSub = findViewById(R.id.txtProgressSub);

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

        // If path is missing or category is "MyDesign", fetch details or find true
        // category
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
                                path = item.hasChild("imagePath") ? item.child("imagePath").getValue(String.class)
                                        : item.child("url").getValue(String.class);
                            }
                        } else {
                            // Search sub-categories
                            for (DataSnapshot subSnap : catSnap.getChildren()) {
                                if (subSnap.hasChild(templateId)) {
                                    category = catSnap.getKey() + "/" + subSnap.getKey();
                                    found = true;
                                    DataSnapshot item = subSnap.child(templateId);
                                    String type = item.child("type").getValue(String.class);
                                    if (path == null) {
                                        path = item.hasChild("imagePath")
                                                ? item.child("imagePath").getValue(String.class)
                                                : item.child("url").getValue(String.class);
                                    }
                                    break;
                                }
                            }
                        }
                        if (found)
                            break;
                    }

                    if (found || path != null) {
                        initUI();
                    } else {
                        // 🧹 SELF-CLEANING: If template is deleted from main nodes,
                        // remove it from user's history too to avoid "blank" items.
                        if (uid != null) {
                            String[] types = {"likes", "favorites", "edits", "saves"};
                            for (String type : types) {
                                rootRef.child("user_activity").child(uid).child(type).child(templateId).removeValue();
                            }
                        }
                        Toast.makeText(TemplatePreviewActivity.this, R.string.msg_template_deleted, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (path != null)
                        initUI();
                    else
                        finish();
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
                                    path = s.hasChild("imagePath") ? s.child("imagePath").getValue(String.class)
                                            : s.child("url").getValue(String.class);
                                }
                                initUI();
                            } else {
                                finish();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError e) {
                            if (path != null)
                                initUI();
                            else
                                finish();
                        }
                    });
        }
    }

    void initUI() {
        // Detect Video
        boolean isVideo = "Reel Maker".equalsIgnoreCase(category) ||
                (path != null && (path.toLowerCase().endsWith(".mp4") || path.toLowerCase().endsWith(".webm")));

        if (isVideo) {
            layPlay.setVisibility(android.view.View.VISIBLE);
            previewBottomShadow.setVisibility(android.view.View.VISIBLE);
//            btnEdit.setVisibility(android.view.View.GONE); // 🚫 HIDE EDIT OPTION FOR VIDEOS
            btnShare.setVisibility(android.view.View.GONE); // 🚫 HIDE SHARE OPTION FOR VIDEOS
            // 🎬 Fixed height for Video
            params.height = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 380, getResources().getDisplayMetrics());
        } else {
            layPlay.setVisibility(android.view.View.GONE);
            previewBottomShadow.setVisibility(android.view.View.GONE);
            btnEdit.setVisibility(android.view.View.VISIBLE); // ✅ SHOW EDIT OPTION FOR IMAGES
            btnShare.setVisibility(android.view.View.VISIBLE); // ✅ SHOW SHARE OPTION FOR IMAGES
            // 🖼️ Dynamic height for Image
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        previewContainer.setLayoutParams(params);

        // MAIN IMAGE
        Glide.with(this)
                .load(path)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(img);

        btnBack.setOnClickListener(v -> finish());
        txtCategory.setText(category != null ? category : "Visual Design");

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

        layPlay.setOnClickListener(v -> playVideo());

        loadLikeStatus();
        loadFavoriteStatus();
        loadSimilarTemplates();

        // SEARCH
        btnSearch.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));

        // LIKE
        btnLike.setOnClickListener(v -> toggleLike());

        // FAVORITE
        btnFav.setOnClickListener(v -> toggleFavorite());

        // EDIT
        btnEdit.setOnClickListener(v -> {
            rootRef.child("template_activity")
                    .child(templateId)
                    .child("edits")
                    .push()
                    .setValue(uid);

            rootRef.child("user_activity")
                    .child(uid)
                    .child("edits")
                    .child(templateId)
                    .setValue(path);

            Intent i = new Intent(this, ManageTemplatesActivity.class);
            i.putExtra("uri", path);
            i.putExtra("category", category);
            i.putExtra("isVideo", isVideo());
            startActivity(i);
        });

        // SHARE
        btnShare.setOnClickListener(v -> shareImage());

        // SAVE
        btnSave.setOnClickListener(v -> new AlertDialog.Builder(this)
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

        if (category == null)
            return;

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
                                String type = d.child("type").getValue(String.class);
                                list.add(new TemplateModel(d.getKey(), itemPath, category, null, type));
                            }
                        }

                        rvSimilar.setLayoutManager(new GridLayoutManager(TemplatePreviewActivity.this, 3));
                        TemplateGridAdapter adapter = new TemplateGridAdapter(list, R.layout.item_grid_square, t -> {
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
                    public void onCancelled(DatabaseError error) {
                    }
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
                                    imgLike.setColorFilter(Color.RED);
                                }
                            }

                            public void onCancelled(DatabaseError e) {
                            }
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
                                    imgFav.setColorFilter(Color.YELLOW);
                                }
                            }

                            public void onCancelled(DatabaseError e) {
                            }
                        });
    }

    // =====================================================

    void toggleLike() {

        isLiked = !isLiked;

        if (isLiked) {
            imgLike.setColorFilter(Color.RED);

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
            imgLike.clearColorFilter();

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
            imgFav.setColorFilter(Color.YELLOW);

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
            imgFav.clearColorFilter();

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
        // Record Share Activity
        rootRef.child("template_activity")
                .child(templateId)
                .child("shares")
                .push()
                .setValue(uid);
        if (isVideo()) {
            toast(getString(R.string.msg_sharing_video_link));
            Intent s = new Intent(Intent.ACTION_SEND);
            s.setType("text/plain");
            s.putExtra(Intent.EXTRA_TEXT,
                    getString(R.string.share_msg_body, getString(R.string.app_name), getPackageName()) + "\n\n" + path);
            startActivity(Intent.createChooser(s, getString(R.string.title_share_video)));
            return;
        }

        Bitmap bitmap = Bitmap.createBitmap(previewContainer.getWidth(), previewContainer.getHeight(),
                Bitmap.Config.ARGB_8888);
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // =====================================================

    void saveImage() {
        if (isVideo()) {
            handleVideoAction(false);
            return;
        }

        Bitmap bitmap = Bitmap.createBitmap(previewContainer.getWidth(), previewContainer.getHeight(),
                Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        previewContainer.draw(canvas);

        new Thread(() -> {
            try {
                String saved = MediaStore.Images.Media.insertImage(
                        getContentResolver(), bitmap, "RMPlus_" + System.currentTimeMillis(), "Template");

                runOnUiThread(() -> {
                    if (saved != null) {
                        toast(getString(R.string.msg_saved_to_gallery));
                        rootRef.child("template_activity")
                                .child(templateId)
                                .child("saves")
                                .push()
                                .setValue(uid);
                        rootRef.child("user_activity").child(uid).child("saves").child(templateId).setValue(path);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // =====================================================

    private String makeSafeKey(String path) {
        return Base64.encodeToString(
                path.getBytes(),
                Base64.NO_WRAP);
    }

    private void playVideo() {
        if (path == null)
            return;
        videoView.setVisibility(android.view.View.VISIBLE);
        img.setVisibility(android.view.View.GONE);
        layPlay.setVisibility(android.view.View.GONE);
        previewBottomShadow.setVisibility(android.view.View.GONE);

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

    void handleVideoAction(boolean isShare) {
        if (path == null) return;

        progressOverlay.setVisibility(View.VISIBLE);
        txtProgressTitle.setText(isShare ? "Preparing Video..." : "Downloading Video...");
        txtProgressSub.setText("Please wait a moment");

        new Thread(() -> {
            try {
                File tempFile = new File(getCacheDir(), "video_" + System.currentTimeMillis() + ".mp4");
                java.net.URL url = new java.net.URL(path);
                java.io.InputStream is = url.openStream();
                java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
                byte[] buffer = new byte[16384];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
                fos.close();
                is.close();

                runOnUiThread(() -> {
                    progressOverlay.setVisibility(View.GONE);
                    if (isShare) {
                        shareVideoFile(tempFile);
                    } else {
                        saveVideoToGallery(tempFile);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressOverlay.setVisibility(View.GONE);
                    Toast.makeText(this, "Action failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    void shareVideoFile(File file) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        Intent s = new Intent(Intent.ACTION_SEND);
        s.setType("video/*");
        s.putExtra(Intent.EXTRA_STREAM, uri);
        s.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(s, getString(R.string.title_share_video)));
    }

    void saveVideoToGallery(File file) {
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, "RMPlus_Video_" + System.currentTimeMillis() + ".mp4");
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            values.put(MediaStore.Video.Media.IS_PENDING, 1);
        }

        Uri collection = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q ?
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) :
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        Uri itemUri = getContentResolver().insert(collection, values);
        if (itemUri != null) {
            try (java.io.OutputStream os = getContentResolver().openOutputStream(itemUri);
                 java.io.InputStream isInput = new java.io.FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = isInput.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.content.ContentValues updateValues = new android.content.ContentValues();
                updateValues.put(MediaStore.Video.Media.IS_PENDING, 0);
                getContentResolver().update(itemUri, updateValues, null, null);
            }
            Toast.makeText(this, R.string.msg_saved_to_gallery, Toast.LENGTH_SHORT).show();
        }
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }
}
