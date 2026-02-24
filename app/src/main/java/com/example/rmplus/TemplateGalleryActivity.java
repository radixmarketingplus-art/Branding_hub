package com.example.rmplus;

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

    String currentCategory = "All";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_template_gallery);

        SharedPreferences sps =
                getSharedPreferences("APP_DATA", MODE_PRIVATE);

        String role = sps.getString("role", "user");

        setupBase(role, R.id.template);

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

        adapter = new TemplateGridAdapter(allTemplatesModel, template -> {

            Intent i = new Intent(
                    TemplateGalleryActivity.this,
                    TemplatePreviewActivity.class
            );
            i.putExtra("id", template.id);
            i.putExtra("path", template.url); // for instant glide load
            i.putExtra("category", template.category);
            startActivity(i);

        });

        recycler.setAdapter(adapter);

        loadDynamicCategories();
        loadCategory("All");
    }

    void loadDynamicCategories() {
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("templates")
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                        categories.clear();
                        categories.add("All");
                        for (com.google.firebase.database.DataSnapshot d : snapshot.getChildren()) {
                            if (d.getKey() != null && !d.getKey().equals("Trending Now") && !d.getKey().equals("Advertisement") && !d.getKey().equals("Frame")) {
                                categories.add(d.getKey());
                            }
                        }
                        createFilterButtons();
                    }

                    @Override
                    public void onCancelled(com.google.firebase.database.DatabaseError error) {}
                });
    }

    void createFilterButtons() {

        filterContainer.removeAllViews();

        for (String c : categories) {

            TextView chip = new TextView(this);
            chip.setText(c);
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
        String[] subs = {"All", "Political", "NGO", "Business"};
        for (String s : subs) {
            TextView chip = new TextView(this);
            chip.setText(s);
            chip.setPadding(30, 10, 30, 10);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMargins(8, 0, 8, 0);
            chip.setLayoutParams(lp);
            chip.setBackgroundResource(R.drawable.bg_filter_chip);
            chip.setOnClickListener(v -> {
                currentSubCategory = s;
                for (int i = 0; i < subFilterContainer.getChildCount(); i++) {
                    subFilterContainer.getChildAt(i).setBackgroundResource(R.drawable.bg_filter_chip);
                }
                chip.setBackgroundResource(R.drawable.bg_filter_chip_selected);
                loadBusinessSubCategory(s);
            });
            subFilterContainer.addView(chip);
        }
        // Select 'All' by default
        ((TextView)subFilterContainer.getChildAt(0)).performClick();
    }

    void loadBusinessSubCategory(String sub) {
        DatabaseReference rootRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("templates").child("Business Frame");

        if ("All".equalsIgnoreCase(sub)) {
            rootRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                    ArrayList<TemplateModel> result = new ArrayList<>();
                    for (com.google.firebase.database.DataSnapshot subSnap : snapshot.getChildren()) {
                        for (com.google.firebase.database.DataSnapshot itemSnap : subSnap.getChildren()) {
                            String url = itemSnap.hasChild("imagePath") ? itemSnap.child("imagePath").getValue(String.class) : itemSnap.child("url").getValue(String.class);
                            if (url != null) result.add(new TemplateModel(itemSnap.getKey(), url, "Business Frame/" + subSnap.getKey()));
                        }
                    }
                    adapter.setData(result);
                }
                @Override public void onCancelled(com.google.firebase.database.DatabaseError error) {}
            });
        } else {
            rootRef.child(sub).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                    ArrayList<TemplateModel> result = new ArrayList<>();
                    for (com.google.firebase.database.DataSnapshot d : snapshot.getChildren()) {
                        String url = d.hasChild("imagePath") ? d.child("imagePath").getValue(String.class) : d.child("url").getValue(String.class);
                        if (url != null) result.add(new TemplateModel(d.getKey(), url, "Business Frame/" + sub));
                    }
                    adapter.setData(result);
                }
                @Override
                public void onCancelled(com.google.firebase.database.DatabaseError error) {}
            });
        }
    }

//    void animateUnderline(View tab) {
//
//        if (underline == null || tab == null) return;
//
//        TextView txt = (TextView) ((LinearLayout) tab).getChildAt(0);
//
//        tab.post(() -> {
//
//            int tabLeft = tab.getLeft();
//            int tabWidth = tab.getWidth();
//
//            txt.measure(
//                    View.MeasureSpec.UNSPECIFIED,
//                    View.MeasureSpec.UNSPECIFIED
//            );
//
//            int textWidth = txt.getMeasuredWidth();
//
//            int newLeftMargin =
//                    tabLeft + (tabWidth - textWidth) / 2;
//
//            // ✅ FIX: FrameLayout.LayoutParams (NOT LinearLayout)
//            FrameLayout.LayoutParams params =
//                    (FrameLayout.LayoutParams) underline.getLayoutParams();
//
//            params.width = textWidth;
//            params.leftMargin = newLeftMargin;
//
//            underline.setLayoutParams(params);
//
//            // Center tab smoothly
//            tabScroll.smoothScrollTo(
//                    tabLeft - tabScroll.getWidth() / 2 + tabWidth / 2,
//                    0
//            );
//        });
//    }


    void updateTabUI() {

        for (int i = 0; i < filterContainer.getChildCount(); i++) {

            TextView chip =
                    (TextView) filterContainer.getChildAt(i);

            if (chip.getText().toString().equals(currentCategory)) {
                chip.setTypeface(null, android.graphics.Typeface.BOLD);
                chip.setTextColor(getColorFromAttr(
                        com.google.android.material.R.attr.colorOnSurface
                ));
                chip.setBackgroundResource(R.drawable.bg_filter_chip_selected);
            } else {
                chip.setTypeface(null, android.graphics.Typeface.NORMAL);
                chip.setTextColor(
                        getResources().getColor(
                                android.R.color.darker_gray, getTheme()
                        )
                );
                chip.setBackgroundResource(R.drawable.bg_filter_chip);
            }
        }
    }


    void loadCategory(String key) {
        currentCategory = key;
        DatabaseReference templatesRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("templates");

        if (key.equals("All")) {
            templatesRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                    ArrayList<TemplateModel> result = new ArrayList<>();
                    for (com.google.firebase.database.DataSnapshot categorySnapshot : snapshot.getChildren()) {
                        String cat = categorySnapshot.getKey();
                        for (com.google.firebase.database.DataSnapshot itemSnapshot : categorySnapshot.getChildren()) {
                            String path = null;
                            if (itemSnapshot.hasChild("imagePath")) {
                                path = itemSnapshot.child("imagePath").getValue(String.class);
                            } else if (itemSnapshot.hasChild("url")) {
                                path = itemSnapshot.child("url").getValue(String.class);
                            }
                            if (path != null && !cat.equals("Frame")) {
                                result.add(new TemplateModel(itemSnapshot.getKey(), path, cat));
                            }
                        }
                    }
                    adapter.setData(result);
                }

                @Override
                public void onCancelled(com.google.firebase.database.DatabaseError error) {}
            });
        } else {
            templatesRef.child(key).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                    ArrayList<TemplateModel> result = new ArrayList<>();
                    for (com.google.firebase.database.DataSnapshot itemSnapshot : snapshot.getChildren()) {
                        String path = null;
                        if (itemSnapshot.hasChild("imagePath")) {
                            path = itemSnapshot.child("imagePath").getValue(String.class);
                        } else if (itemSnapshot.hasChild("url")) {
                            path = itemSnapshot.child("url").getValue(String.class);
                        }
                        if (path != null) {
                            result.add(new TemplateModel(itemSnapshot.getKey(), path, key));
                        }
                    }
                    adapter.setData(result);
                }

                @Override
                public void onCancelled(com.google.firebase.database.DatabaseError error) {}
            });
        }
    }

    private int getColorFromAttr(int attr) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }



}
