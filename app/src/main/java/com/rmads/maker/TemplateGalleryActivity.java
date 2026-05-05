package com.rmads.maker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
 
import java.lang.reflect.Type;
import java.util.ArrayList;


public class TemplateGalleryActivity extends BaseActivity {

    RecyclerView recycler;
    LinearLayout filterContainer, subFilterContainer;
    View subFilterScroll;
    TemplateGridAdapter adapter;
    ArrayList<TemplateModel> allTemplatesModel = new ArrayList<>();
    HorizontalScrollView tabScroll;
    String currentSubCategory = "";

    ArrayList<String> categories = new ArrayList<>();
    String ALL_KEY = "All";
    String currentCategory = ALL_KEY;
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_template_gallery);

        SharedPreferences sps =
                getSharedPreferences("APP_DATA", MODE_PRIVATE);

        String role = sps.getString("role", "user");

        setupBase(role, R.id.template);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        recycler = findViewById(R.id.recyclerTemplates);
        filterContainer = findViewById(R.id.filterContainer);
        subFilterContainer = findViewById(R.id.subFilterContainer);
        subFilterScroll = findViewById(R.id.subFilterScroll);
        tabScroll = findViewById(R.id.tabScroll);

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

        recycler.setLayoutManager(
                new GridLayoutManager(this, 3)
        );

        adapter = new TemplateGridAdapter(allTemplatesModel, R.layout.item_grid_square, template -> {
            boolean isAd = template.category != null && template.category.equalsIgnoreCase("Advertisement");

            Intent i;
            if (isAd) {
                String path = template.url != null ? template.url.toLowerCase() : "";
                boolean isVideo = "video".equalsIgnoreCase(template.type) ||
                                  path.endsWith(".mp4") || path.endsWith(".mkv") || path.endsWith(".webm") || path.endsWith(".mov") || path.endsWith(".3gp");

                if (isVideo) {
                    i = new Intent(TemplateGalleryActivity.this, TemplatePreviewActivity.class);
                } else {
                    i = new Intent(TemplateGalleryActivity.this, ImagePreviewActivity.class);
                    i.putExtra("img", template.url);
                }
            } else {
                i = new Intent(TemplateGalleryActivity.this, TemplatePreviewActivity.class);
            }

            i.putExtra("id", template.id);
            i.putExtra("path", template.url);
            i.putExtra("category", template.category);
            startActivity(i);
        });
        recycler.setAdapter(adapter);


        loadDynamicCategories();

        String catIntent = getIntent().getStringExtra("category");
        if (catIntent != null && !catIntent.isEmpty()) {
            currentCategory = catIntent;
        }
        
        loadCategory(currentCategory);
    }

    void loadDynamicCategories() {
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("templates")
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                        categories.clear();
                        categories.add(ALL_KEY);
                        for (com.google.firebase.database.DataSnapshot d : snapshot.getChildren()) {
                            String key = d.getKey();
                            if (key != null) {
                                // Normally skip these
                                if (key.equals("Trending Now") || key.equals("Advertisement") || key.equals("Frame")) {
                                    // But if we specifically came for one of these, add it!
                                    if (key.equalsIgnoreCase(currentCategory)) {
                                        categories.add(key);
                                    }
                                    continue;
                                }
                                categories.add(key);
                            }
                        }
                        createFilterButtons();

                        // 🎯 Scroll to the passed category if any
                        if (!currentCategory.equals(ALL_KEY)) {
                            findChipAndClick(currentCategory);
                        }
                    }

                    @Override
                    public void onCancelled(com.google.firebase.database.DatabaseError error) {}
                });
    }

    void createFilterButtons() {

        filterContainer.removeAllViews();

        for (String c : categories) {

            TextView chip = new TextView(this);
            String displayTitle = c.equals(ALL_KEY) ? getString(R.string.filter_all) : getLocalizedSectionName(c);
            chip.setText(displayTitle);
            chip.setTag(c); // Store original key in tag
            chip.setTextSize(14);
            chip.setPadding(36, 16, 36, 16);
            chip.setBackgroundResource(R.drawable.bg_filter_chip);
            chip.setTextColor(
                    getResources().getColor(android.R.color.darker_gray, getTheme())
            );

            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
            lp.setMargins(8, 0, 8, 0);
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> {
                currentCategory = c;
                updateTabUI();
                if (c.equalsIgnoreCase("Business Frame")) {
                    showSubFilters();
                } else {
                    subFilterScroll.setVisibility(View.GONE);
                    loadCategory(c);
                }
            });

            filterContainer.addView(chip);
        }

        updateTabUI();
    }

    void showSubFilters() {
        subFilterScroll.setVisibility(View.VISIBLE);
        subFilterContainer.removeAllViews();
        String[] subs = {ALL_KEY, "Political", "NGO", "Business"};
        int[] subRes = {R.string.filter_all, R.string.cat_political, R.string.cat_ngo, R.string.cat_business};
        
        for (int i = 0; i < subs.length; i++) {
            String s = subs[i];
            TextView chip = new TextView(this);
            chip.setText(getString(subRes[i]));
            chip.setTag(s); // Store original key in tag
            chip.setPadding(30, 10, 30, 10);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMargins(8, 0, 8, 0);
            chip.setLayoutParams(lp);
            chip.setBackgroundResource(R.drawable.bg_filter_chip);
            chip.setOnClickListener(v -> {
                currentSubCategory = s;
                for (int j = 0; j < subFilterContainer.getChildCount(); j++) {
                    subFilterContainer.getChildAt(j).setBackgroundResource(R.drawable.bg_filter_chip);
                }
                chip.setBackgroundResource(R.drawable.bg_filter_chip_selected);
                loadBusinessSubCategory(s);
            });
            subFilterContainer.addView(chip);
        }
        // Select 'All' by default
        ((TextView)subFilterContainer.getChildAt(0)).performClick();
    }
    void updateTabUI() {
        for (int i = 0; i < filterContainer.getChildCount(); i++) {
            TextView chip = (TextView) filterContainer.getChildAt(i);
            String chipKey = (String) chip.getTag();
            if (chipKey != null && chipKey.equals(currentCategory)) {
                chip.setTypeface(null, android.graphics.Typeface.BOLD);
                chip.setTextColor(getColorFromAttr(com.google.android.material.R.attr.colorOnSurface));
                chip.setBackgroundResource(R.drawable.bg_filter_chip_selected);
            } else {
                chip.setTypeface(null, android.graphics.Typeface.NORMAL);
                chip.setTextColor(getResources().getColor(android.R.color.darker_gray, getTheme()));
                chip.setBackgroundResource(R.drawable.bg_filter_chip);
            }
        }
    }


    private java.util.Set<String> seenCategories = new java.util.HashSet<>();

    private void collectTemplates(DataSnapshot snapshot, String rootCategory, ArrayList<TemplateModel> result) {
        if (snapshot == null) return;
        if (snapshot.hasChild("imagePath") || snapshot.hasChild("url")) {
            String path = snapshot.hasChild("imagePath") ? 
                    snapshot.child("imagePath").getValue(String.class) : 
                    snapshot.child("url").getValue(String.class);
            String type = snapshot.child("type").getValue(String.class);
            
            // Get the full category path relative to "templates"
            String fullPath = snapshot.getRef().getPath().toString(); 
            // e.g., /templates/Festival Cards/17-03-2026/Holi/ID -> we want "Festival Cards/17-03-2026/Holi"
            String cat = rootCategory; 
            String date = null;
            if (fullPath.contains("/templates/")) {
                String relative = fullPath.substring(fullPath.indexOf("/templates/") + 11);
                if (relative.contains("/")) {
                    cat = relative.substring(0, relative.lastIndexOf("/"));
                    
                    // Specific logic for Festival Cards: extract Date
                    if (cat.startsWith("Festival Cards")) {
                        String[] parts = relative.split("/");
                        if (parts.length > 1) {
                            date = parts[1]; // Festival Cards/DD-MM-YYYY/AppName
                        }
                    }
                }
            }

            if (path != null) {
                // 🛑 USER REQUEST: Show only one item per category for Festival Cards
                if (cat.startsWith("Festival Cards")) {
                    if (seenCategories.contains(cat)) return;
                    seenCategories.add(cat);
                }

                // Add to start to show newest items first
                result.add(0, new TemplateModel(snapshot.getKey(), path, cat, date, type));
            }
        } else {
            for (DataSnapshot child : snapshot.getChildren()) {
                collectTemplates(child, rootCategory, result);
            }
        }
    }



    void loadCategory(String key) {
        currentCategory = key;
        seenCategories.clear(); // 🔄 Reset filter for each load
        DatabaseReference templatesRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("templates");

        if (key.equals(ALL_KEY)) {
            templatesRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                    ArrayList<TemplateModel> result = new ArrayList<>();
                    for (com.google.firebase.database.DataSnapshot categorySnapshot : snapshot.getChildren()) {
                        String cat = categorySnapshot.getKey();
                        if (cat == null || cat.equals("Advertisement") || cat.equals("Frame") || cat.equals("Trending Now")) {
                            continue;
                        }
                        collectTemplates(categorySnapshot, cat, result);
                    }
                    adapter.setData(result);
                }
                @Override public void onCancelled(com.google.firebase.database.DatabaseError error) {}
            });
        } else {
            templatesRef.child(key).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                    ArrayList<TemplateModel> result = new ArrayList<>();
                    collectTemplates(snapshot, key, result);
                    adapter.setData(result);
                }
                @Override public void onCancelled(com.google.firebase.database.DatabaseError error) {}
            });
        }
    }

    void loadBusinessSubCategory(String sub) {
        seenCategories.clear(); // 🔄 Reset filter for each load
        DatabaseReference rootRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("templates").child("Business Frame");
        if (ALL_KEY.equalsIgnoreCase(sub)) {
            loadCategory("Business Frame");
        } else {
            rootRef.child(sub).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                    ArrayList<TemplateModel> result = new ArrayList<>();
                    collectTemplates(snapshot, "Business Frame/" + sub, result);
                    adapter.setData(result);
                }
                @Override public void onCancelled(com.google.firebase.database.DatabaseError error) {}
            });
        }
    }


    private void findChipAndClick(String target) {
        for (int i = 0; i < filterContainer.getChildCount(); i++) {
            TextView chip = (TextView) filterContainer.getChildAt(i);
            String chipKey = (String) chip.getTag();
            if (chipKey != null && chipKey.equalsIgnoreCase(target)) {
                chip.performClick();
                // Smooth scroll to this chip
                int finalI = i;
                tabScroll.post(() -> tabScroll.smoothScrollTo(filterContainer.getChildAt(finalI).getLeft() - 50, 0));
                break;
            }
        }
    }

    private int getColorFromAttr(int attr) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }



}
