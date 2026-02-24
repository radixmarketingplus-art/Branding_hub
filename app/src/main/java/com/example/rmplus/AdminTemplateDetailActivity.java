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

public class AdminTemplateDetailActivity extends AppCompatActivity {

    ImageView imgPreview;
    TextView txtCategory;

    TextView txtLikeCount, txtFavCount, txtEditCount, txtSaveCount;
    TextView txtExpiryStatus;
    long currentExpiry = 0;

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
        txtExpiryStatus = findViewById(R.id.txtExpiryStatus);


        findViewById(R.id.btnLike).setOnClickListener(v -> openStats("likes"));
        findViewById(R.id.btnFav).setOnClickListener(v -> openStats("favorites"));
        findViewById(R.id.btnEdit).setOnClickListener(v -> openStats("edits"));
        findViewById(R.id.btnSave).setOnClickListener(v -> openStats("saves"));

        statsLayout = findViewById(R.id.layout_admin_stats);
        MaterialButton btnOpenAd = findViewById(R.id.btnOpenAd);
        findViewById(R.id.btnDeleteIcon).setOnClickListener(v -> confirmDelete());
        findViewById(R.id.btnEditIcon).setOnClickListener(v -> {
            if (category == null || realTemplateId == null) {
                android.widget.Toast.makeText(this, "Still fetching template info...", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            android.widget.Toast.makeText(this, "Preparing for edit...", android.widget.Toast.LENGTH_SHORT).show();
            
            // Fetch the very latest data snapshot for this template
            FirebaseDatabase.getInstance().getReference("templates")
                    .child(category)
                    .child(realTemplateId)
                    .get().addOnSuccessListener(snapshot -> {
                        Intent i = new Intent(this, UploadManagerActivity.class);
                        i.putExtra("edit_url", templatePath);
                        i.putExtra("category", category);
                        i.putExtra("realId", realTemplateId); // IMPORTANT for deletion
                        
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
                        // Fallback simple edit if DB fetch fails
                        Intent i = new Intent(this, UploadManagerActivity.class);
                        i.putExtra("edit_url", templatePath);
                        i.putExtra("category", category);
                        i.putExtra("realId", realTemplateId);
                        startActivity(i);
                    });
        });

        // 🌐 LOAD FROM VPS URL (Glide handles caching)
        Glide.with(this)
                .load(templatePath)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imgPreview);

        // ==================================================
        // 🔎 FIND TRUE CATEGORY AND LOAD DETAILS
        // ==================================================
        if (templatePath != null) {
            discoverCategoryAndLoad();
        }

        findViewById(R.id.btnSearch).setOnClickListener(v -> {
            startActivity(new Intent(
                    AdminTemplateDetailActivity.this,
                    SearchActivity.class
            ));
        });
    }

    private String realTemplateId = null; 

    private void discoverCategoryAndLoad() {
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
                    txtCategory.setText(category);
                    
                    // 📅 LOAD DATES & LINKS FROM DB SNAPSHOT
                    if (finalTemplateSnap.hasChild("expiryDate")) {
                        Object exp = finalTemplateSnap.child("expiryDate").getValue();
                        currentExpiry = (exp instanceof Long) ? (Long) exp : ((Integer) exp).longValue();
                        String dateStr = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
                                .format(new java.util.Date(currentExpiry));
                        txtExpiryStatus.setText("Expires on: " + dateStr);
                    } else {
                        txtExpiryStatus.setText("No expiry set");
                    }

                    if (finalTemplateSnap.hasChild("link")) {
                        adLink = finalTemplateSnap.child("link").getValue(String.class);
                    }

                    if (category.contains("Advertisement")) {
                        statsLayout.setVisibility(View.GONE);
                        findViewById(R.id.btnOpenAd).setVisibility(View.VISIBLE);
                    } else {
                        findViewById(R.id.btnOpenAd).setVisibility(View.GONE);
                        setupClicks();
                        loadStats();
                    }
                } else {
                    txtCategory.setText("External Content");
                    realTemplateId = templateKey; // Fallback to Base64
                    loadStats();
                }
            }

            @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
        });
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
        if (category == null || category.equalsIgnoreCase("Unknown Category")) {
            android.widget.Toast.makeText(this, "Finding category, please wait...", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(this, StatsDetailActivity.class);
        i.putExtra("path", templatePath);
        i.putExtra("category", category); // Pass the true category
        i.putExtra("defaultTab", tab);
        startActivity(i);
    }

    // ---------------- LOAD COUNTS ----------------

    void loadStats() {
        if (realTemplateId == null) return;

        DatabaseReference activityRef = FirebaseDatabase.getInstance().getReference("template_activity");
        
        // We check BOTH the Real ID and the Base64 key (templateKey) to combine results
        // This ensures no legacy or differently-tagged stats are missed.
        
        String[] keysToCheck = {realTemplateId, templateKey};
        String[] activityTypes = {"likes", "favorites", "edits", "saves"};
        TextView[] views = {txtLikeCount, txtFavCount, txtEditCount, txtSaveCount};

        for (int i = 0; i < activityTypes.length; i++) {
            final String type = activityTypes[i];
            final TextView targetView = views[i];
            
            activityRef.child(realTemplateId).child(type).get().addOnSuccessListener(snap1 -> {
                long count = snap1.getChildrenCount();
                
                // If the real ID is different from Base64, check Base64 too and add
                if (!realTemplateId.equals(templateKey)) {
                    activityRef.child(templateKey).child(type).get().addOnSuccessListener(snap2 -> {
                        long total = count + snap2.getChildrenCount();
                        targetView.setText(String.valueOf(total));
                    });
                } else {
                    targetView.setText(String.valueOf(count));
                }
            });
        }
    }


    // ---------------- DELETION LOGIC ----------------

    private void confirmDelete() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Template")
                .setMessage("This will permanently remove the template from VPS, Firebase, and Local Storage. Continue?")
                .setPositiveButton("Delete", (d, w) -> startDeletionProcess())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startDeletionProcess() {
        if (category == null || realTemplateId == null) {
            android.widget.Toast.makeText(this, "Cannot delete: Category not identified", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        android.widget.Toast.makeText(this, "Cleaning all nodes...", android.widget.Toast.LENGTH_SHORT).show();

        // 1. Remove from Local SharedPreferences
        removeFromLocal();

        // 2. Remove from Firebase templates node (Direct Deletion)
        FirebaseDatabase.getInstance().getReference("templates")
                .child(category)
                .child(realTemplateId)
                .removeValue();

        // 3. Clean up user_activity AND template_activity (Sequenced to avoid race condition)
        cleanUpActivityNodes(realTemplateId);
        if (!realTemplateId.equals(templateKey)) {
            cleanUpActivityNodes(templateKey);
        }

        // 4. Delete from VPS
        deleteFromVPS(templatePath);
    }

    private void cleanUpActivityNodes(String tid) {
        DatabaseReference activityRef = FirebaseDatabase.getInstance().getReference("template_activity").child(tid);
        DatabaseReference userActivityRef = FirebaseDatabase.getInstance().getReference("user_activity");
        
        String[] types = {"likes", "favorites", "edits", "saves"};
        
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
                        android.widget.Toast.makeText(AdminTemplateDetailActivity.this, "Template Deleted Everywhere", android.widget.Toast.LENGTH_SHORT).show();
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
                        android.widget.Toast.makeText(this, "Template Deleted Successfully", android.widget.Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        android.widget.Toast.makeText(this, "VPS Deletion failed: " + code, android.widget.Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
