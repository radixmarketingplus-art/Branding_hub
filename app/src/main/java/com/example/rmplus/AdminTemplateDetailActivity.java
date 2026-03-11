package com.example.rmplus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class AdminTemplateDetailActivity extends BaseActivity {

    ImageView imgPreview;
    TextView txtCategory;

    TextView txtLikeCount, txtFavCount, txtEditCount, txtSaveCount;
    TextView txtExpiryStatus;
    long currentExpiry = 0;

    String templatePath;
    String category;
    String templateKey;
    String adLink = null;
    View statsRow, layPlay;


    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_admin_template_detail);

        templatePath = getIntent().getStringExtra("path");
        category = getIntent().getStringExtra("category");
        realTemplateId = getIntent().getStringExtra("id"); 
        
        txtLikeCount  = findViewById(R.id.txtLikeCount);
        txtFavCount   = findViewById(R.id.txtFavCount);
        txtEditCount  = findViewById(R.id.txtEditCount);                         
        txtSaveCount  = findViewById(R.id.txtSaveCount);

        // 🔐 SAME KEY USED EVERYWHERE
        templateKey = Base64.encodeToString(
                templatePath.getBytes(),
                Base64.NO_WRAP
        );
        safeKey = makeSafeKey(templatePath); // Key with _ replaces used in Editor/Preview

        imgPreview = findViewById(R.id.imgPreview);
        txtCategory = findViewById(R.id.txtCategory);
        txtExpiryStatus = findViewById(R.id.txtExpiryStatus);

        // Header
        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.btnSearch).setOnClickListener(v -> {
            startActivity(new Intent(this, SearchActivity.class));
        });
        findViewById(R.id.btnDelete).setOnClickListener(v -> confirmDelete());

        // Actions
        MaterialButton btnEditNow = findViewById(R.id.btnEditNow);
        btnEditNow.setOnClickListener(v -> {
            if (category == null || realTemplateId == null) {
                android.widget.Toast.makeText(this, R.string.msg_fetching_info, android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            android.widget.Toast.makeText(this, R.string.msg_preparing_edit, android.widget.Toast.LENGTH_SHORT).show();
            
            // Fetch the very latest data snapshot for this template
            FirebaseDatabase.getInstance().getReference("templates")
                    .child(category)
                    .child(realTemplateId)
                    .get().addOnSuccessListener(snapshot -> {
                        Intent i = new Intent(this, UploadManagerActivity.class);
                        i.putExtra("edit_url", templatePath);
                        i.putExtra("category", category);
                        i.putExtra("realId", realTemplateId); 
                        
                        if (snapshot.exists()) {
                            // Pass Expiry
                            if (snapshot.hasChild("expiryDate")) {
                                Object exp = snapshot.child("expiryDate").getValue();
                                if (exp instanceof Long) i.putExtra("expiryDate", (Long) exp);
                                else if (exp instanceof Integer) i.putExtra("expiryDate", ((Integer) exp).longValue());
                            }
                            
                            // Pass Festival Date
                            if (snapshot.hasChild("date")) {
                                i.putExtra("date", snapshot.child("date").getValue(String.class));
                            }
                            
                            // Pass Ad Link
                            if (snapshot.hasChild("link")) {
                                i.putExtra("link", snapshot.child("link").getValue(String.class));
                            }
                        }
                        startActivity(i);
                    }).addOnFailureListener(e -> {
                        Intent i = new Intent(this, UploadManagerActivity.class);
                        i.putExtra("edit_url", templatePath);
                        i.putExtra("category", category);
                        i.putExtra("realId", realTemplateId);
                        startActivity(i);
                    });
        });

        findViewById(R.id.btnOpenAd).setOnClickListener(v -> {
            if (adLink != null && !adLink.isEmpty()) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(adLink));
                startActivity(i);
            }
        });

        // 🌐 LOAD FROM VPS URL
        Glide.with(this)
                .load(templatePath)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imgPreview);

        statsRow = findViewById(R.id.statsRow);
        layPlay = findViewById(R.id.layPlay);

        if (templatePath != null) {
            discoverCategoryAndLoad();
        }

        imgPreview.setOnClickListener(v -> {
            if (templatePath != null) {
                Intent i = new Intent(this, ImagePreviewActivity.class);
                i.putExtra("img", templatePath);
                startActivity(i);
            }
        });

        // 🟢 PREMIUM SCROLL BEHAVIOR
        com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBar);
        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        appBarLayout.addOnOffsetChangedListener((layout, offset) -> {
            float alpha = Math.abs(offset) / (float) layout.getTotalScrollRange();
            toolbarTitle.setAlpha(alpha);
            if (category != null) {
                toolbarTitle.setText(getLocalizedCategory(category));
            }
        });
    }

    private String realTemplateId = null; 

    private void discoverCategoryAndLoad() {
        if (realTemplateId != null && category != null && !category.isEmpty()) {
            // Already have enough info to load directly
            txtCategory.setText(getLocalizedCategory(category));
            FirebaseDatabase.getInstance().getReference("templates")
                    .child(category).child(realTemplateId)
                    .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        processTemplateData(snapshot);
                    } else {
                        // fallback if specific path doesn't exist (maybe category was broad or old)
                        performFullDiscovery();
                    }
                }
                @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError e) {}
            });
            return;
        }
        performFullDiscovery();
    }

    private void performFullDiscovery() {
        FirebaseDatabase.getInstance().getReference("templates").addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                boolean found = false;
                com.google.firebase.database.DataSnapshot finalTemplateSnap = null;
                String foundCat = "";
                String foundId = "";

                // 🔄 SEARCH ALL CATEGORIES
                for (com.google.firebase.database.DataSnapshot catSnap : snapshot.getChildren()) {
                    for (com.google.firebase.database.DataSnapshot itemSnap : catSnap.getChildren()) {
                        
                        if (itemSnap.hasChild("url") || itemSnap.hasChild("imagePath")) {
                            String path = itemSnap.hasChild("url") ? 
                                          itemSnap.child("url").getValue(String.class) : 
                                          itemSnap.child("imagePath").getValue(String.class);
                                          
                            if (templatePath.equals(path)) {
                                found = true; foundCat = catSnap.getKey(); foundId = itemSnap.getKey();
                                finalTemplateSnap = itemSnap;
                                break;
                            }
                        } else {
                            for (com.google.firebase.database.DataSnapshot subItemSnap : itemSnap.getChildren()) {
                                String path = subItemSnap.hasChild("url") ? 
                                              subItemSnap.child("url").getValue(String.class) : 
                                              subItemSnap.child("imagePath").getValue(String.class);
                                              
                                if (templatePath.equals(path)) {
                                    found = true; foundCat = catSnap.getKey() + "/" + itemSnap.getKey(); foundId = subItemSnap.getKey();
                                    finalTemplateSnap = subItemSnap;
                                    break;
                                }
                            }
                        }
                        if (found) break;
                    }
                    if (found) break;
                }

                if (found) {
                    category = foundCat;
                    realTemplateId = foundId;
                    txtCategory.setText(getLocalizedCategory(category));
                    processTemplateData(finalTemplateSnap);
                } else {
                    txtCategory.setText(R.string.label_external_content);
                    realTemplateId = templateKey; // Fallback to Base64
                    loadStats();
                }
            }

            @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
        });
    }

    private void processTemplateData(com.google.firebase.database.DataSnapshot snap) {
        // 📅 LOAD DATES & LINKS FROM DB SNAPSHOT
        if (snap.hasChild("expiryDate")) {
            Object exp = snap.child("expiryDate").getValue();
            currentExpiry = (exp instanceof Long) ? (Long) exp : ((Integer) exp).longValue();
            String dateStr = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.US)
                    .format(new java.util.Date(currentExpiry));
            txtExpiryStatus.setText(getString(R.string.label_expires, dateStr));
        } else {
            txtExpiryStatus.setText(R.string.msg_no_expiry);
        }

        if (snap.hasChild("link")) {
            adLink = snap.child("link").getValue(String.class);
        }

        if (category.contains("Advertisement")) {
            if (statsRow != null) statsRow.setVisibility(View.GONE);
            findViewById(R.id.btnOpenAd).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.btnOpenAd).setVisibility(View.GONE);
            if (statsRow != null) statsRow.setVisibility(View.VISIBLE);
            setupClicks();
            loadStats();
        }

        // 🎬 SHOW VIDEO INDICATOR
        if (layPlay != null) {
            String type = snap.child("type").getValue(String.class);
            if (isVideo(type)) layPlay.setVisibility(View.VISIBLE);
            else layPlay.setVisibility(View.GONE);
        }
    }

    private boolean isVideo(String type) {
        if ("video".equalsIgnoreCase(type)) return true;
        if (category != null && category.equalsIgnoreCase("Reel Maker")) return true;
        
        // Final check based on URL extension
        if (templatePath != null) {
            String lower = templatePath.toLowerCase().split("\\?")[0];
            return lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.contains("/reel maker/") || lower.contains("/reel%20maker/");
        }
        return false;
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


    // ---------------- BUTTON CLICKS (STATS DETAILS) ----------------

    void setupClicks() {
        View btnLike = findViewById(R.id.statLike);
        View btnFav = findViewById(R.id.statFav);
        View btnEdit = findViewById(R.id.statEdit);
        View btnSave = findViewById(R.id.statSave);

        if (btnLike != null) btnLike.setOnClickListener(v -> openStats("likes"));
        if (btnFav != null) btnFav.setOnClickListener(v -> openStats("favorites"));
        if (btnEdit != null) btnEdit.setOnClickListener(v -> openStats("edits"));
        if (btnSave != null) btnSave.setOnClickListener(v -> openStats("saves"));
    }

    void openStats(String tab) {
        if (category == null || category.equalsIgnoreCase(getString(R.string.label_unknown_cat))) {
            android.widget.Toast.makeText(this, R.string.msg_fetching_info, android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(this, StatsDetailActivity.class);
        i.putExtra("path", templatePath);
        i.putExtra("id", realTemplateId); // ✅ Pass the true template ID
        i.putExtra("category", category); 
        i.putExtra("defaultTab", tab);
        startActivity(i);
    }

    String safeKey; // Global safe key (replaces ., $, #)

    // ---------------- LOAD COUNTS ----------------

    void loadStats() {
        if (realTemplateId == null) return;

        DatabaseReference activityRef = FirebaseDatabase.getInstance().getReference("template_activity");
        
        // We check THREE potential keys to combine results
        // 1. Real ID, 2. Raw Base64, 3. SafeKey (Base64 with replaces)
        
        String[] keysToCheck = {realTemplateId, templateKey, safeKey};
        String[] activityTypes = {"likes", "favorites", "edits", "saves"};
        TextView[] views = {txtLikeCount, txtFavCount, txtEditCount, txtSaveCount};

        for (int i = 0; i < activityTypes.length; i++) {
            final String type = activityTypes[i];
            final TextView targetView = views[i];
            
            final long[] combinedTotal = {0};
            final int[] pending = {keysToCheck.length};

            for (String key : keysToCheck) {
                if (key == null) {
                    pending[0]--;
                    continue;
                }
                activityRef.child(key).child(type).get().addOnSuccessListener(snap -> {
                    combinedTotal[0] += snap.getChildrenCount();
                    pending[0]--;
                    if (pending[0] == 0) {
                        targetView.setText(String.valueOf(combinedTotal[0]));
                    }
                }).addOnFailureListener(e -> {
                    pending[0]--;
                    if (pending[0] == 0) {
                        targetView.setText(String.valueOf(combinedTotal[0]));
                    }
                });
            }
        }
    }


    // ---------------- DELETION LOGIC ----------------

    private void confirmDelete() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.title_delete_template)
                .setMessage(R.string.msg_confirm_delete)
                .setPositiveButton(R.string.btn_delete, (d, w) -> startDeletionProcess())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void startDeletionProcess() {
        if (category == null || realTemplateId == null) {
            android.widget.Toast.makeText(this, R.string.msg_cannot_delete, android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        android.widget.Toast.makeText(this, R.string.msg_cleaning_nodes, android.widget.Toast.LENGTH_SHORT).show();

        // 1. Remove from Local SharedPreferences
        removeFromLocal();

        // 2. Remove from Firebase templates node (Direct Deletion)
        FirebaseDatabase.getInstance().getReference("templates")
                .child(category)
                .child(realTemplateId)
                .removeValue();

        // 📢 NEW: Also delete the corresponding broadcast notification
        NotificationHelper.deleteBroadcast(realTemplateId);

        // 3. Clean up user_activity AND template_activity for ALL known keys
        cleanUpActivityNodes(realTemplateId);
        if (templateKey != null && !templateKey.equals(realTemplateId)) {
            cleanUpActivityNodes(templateKey);
        }
        if (safeKey != null && !safeKey.equals(realTemplateId) && !safeKey.equals(templateKey)) {
            cleanUpActivityNodes(safeKey);
        }

        // 4. Delete from VPS
        deleteFromVPS(templatePath);
    }

    private void cleanUpActivityNodes(String tid) {
        DatabaseReference activityRef = FirebaseDatabase.getInstance().getReference("template_activity").child(tid);
        DatabaseReference userActivityRef = FirebaseDatabase.getInstance().getReference("user_activity");
        
        String[] types = {"likes", "favorites", "edits", "saves", "shares"};
        
        // 🔥 FIRST: READ the list of users who interacted with this template
        activityRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (String type : types) {
                        if (snapshot.hasChild(type)) {
                            for (com.google.firebase.database.DataSnapshot userSnap : snapshot.child(type).getChildren()) {
                                String userId = userSnap.getKey();
                                if (userId != null) {
                                    // Remove template from user's history
                                    userActivityRef.child(userId).child(type).child(tid).removeValue();
                                }
                            }
                        }
                    }
                }
                
                // 🔥 SECOND: NOW that we have read the users, delete the stats node itself
                activityRef.removeValue().addOnCompleteListener(task -> {
                    if (tid.equals(realTemplateId)) {
                        android.widget.Toast.makeText(AdminTemplateDetailActivity.this, R.string.msg_deleted_everywhere, android.widget.Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }
            @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
        });
    }

    private void removeFromLocal() {
        SharedPreferences sp = getSharedPreferences("HOME_DATA", MODE_PRIVATE);
        Gson gson = new Gson();
        
        // Check both direct list and special lists (Ads/Festivals)
        String json = sp.getString(category, null);
        if (json == null) return;

        SharedPreferences.Editor editor = sp.edit();

        if ("Festival Cards".equalsIgnoreCase(category)) {
            Type t = new TypeToken<ArrayList<FestivalCardItem>>(){}.getType();
            ArrayList<FestivalCardItem> list = gson.fromJson(json, t);
            if (list != null) {
                list.removeIf(item -> item.imagePath.equals(templatePath));
                editor.putString(category, gson.toJson(list));
            }
        } else if ("Advertisement".equalsIgnoreCase(category)) {
            Type t = new TypeToken<ArrayList<AdvertisementItem>>(){}.getType();
            ArrayList<AdvertisementItem> list = gson.fromJson(json, t);
            if (list != null) {
                list.removeIf(item -> item.imagePath.equals(templatePath));
                editor.putString(category, gson.toJson(list));
            }
        } else {
            Type t = new TypeToken<ArrayList<String>>(){}.getType();
            ArrayList<String> list = gson.fromJson(json, t);
            if (list != null) {
                list.remove(templatePath);
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
                java.io.OutputStream out = conn.getOutputStream();
                out.write(data.getBytes());
                out.flush();
                out.close();

                int code = conn.getResponseCode();
                runOnUiThread(() -> {
                    if (code == 200) {
                        android.widget.Toast.makeText(this, R.string.msg_deleted_success, android.widget.Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        android.widget.Toast.makeText(this, getString(R.string.msg_vps_delete_failed, code), android.widget.Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String getLocalizedSubCatName(String key) {
        if (key == null) return "";
        if (key.equalsIgnoreCase("Political")) return getString(R.string.cat_political);
        if (key.equalsIgnoreCase("NGO")) return getString(R.string.cat_ngo);
        if (key.equalsIgnoreCase("Business")) return getString(R.string.cat_business);
        return key;
    }

    private String getLocalizedCategory(String key) {
        if (key == null) return "";
        if (key.contains("/")) {
            String[] parts = key.split("/");
            if (parts.length > 1) {
                return getLocalizedSectionName(parts[0]) + " / " + getLocalizedSubCatName(parts[1]);
            }
        }
        return getLocalizedSectionName(key);
    }
}
