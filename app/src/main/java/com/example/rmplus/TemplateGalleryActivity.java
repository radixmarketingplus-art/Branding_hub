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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;


public class TemplateGalleryActivity extends BaseActivity {

    RecyclerView recycler;
    LinearLayout filterContainer;

    TemplateGridAdapter adapter;
    ArrayList<String> allTemplates = new ArrayList<>();

    HorizontalScrollView tabScroll;

    String[] categories = {
            "All",
            "Festival Cards",
            "Latest Update",
            "Business Special",
            "Reel Maker",
            "Business Frame",
            "Motivation",
            "Good Morning",
            "Business Ethics"
    };

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

        adapter = new TemplateGridAdapter(allTemplates, path -> {

            Intent i = new Intent(
                    TemplateGalleryActivity.this,
                    TemplatePreviewActivity.class
            );
            i.putExtra("path", path);
            i.putExtra("category", currentCategory);
            startActivity(i);

        });

        recycler.setAdapter(adapter);

        createFilterButtons();
        loadCategory("All");
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
                loadCategory(c);
            });

            filterContainer.addView(chip);
        }

        updateTabUI();
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

        SharedPreferences sp =
                getSharedPreferences("HOME_DATA", MODE_PRIVATE);

        Gson gson = new Gson();

        ArrayList<String> result = new ArrayList<>();

        // ================= ALL =================
        if (key.equals("All")) {

            String[] allKeys = {
                    "Festival Cards",
                    "Latest Update",
                    "Business Special",
                    "Reel Maker",
                    "Business Frame",
                    "Motivation",
                    "Good Morning",
                    "Business Ethics"
            };

            for (String k : allKeys) {

                String json = sp.getString(k, null);
                if (json == null) continue;

                // Festival objects
                if (k.equals("Festival Cards")) {

                    Type t =
                            new TypeToken<ArrayList<FestivalCardItem>>(){}.getType();

                    ArrayList<FestivalCardItem> list =
                            gson.fromJson(json, t);

                    if (list != null) {
                        for (FestivalCardItem f : list) {
                            result.add(f.imagePath);
                        }
                    }
                }
                else {

                    Type t =
                            new TypeToken<ArrayList<String>>(){}.getType();

                    ArrayList<String> list =
                            gson.fromJson(json, t);

                    if (list != null) {
                        result.addAll(list);
                    }
                }
            }

            // 🔥 Latest first
            reverse(result);

            adapter.setData(result);
            return;
        }

        // ================= FESTIVAL =================
        if (key.equals("Festival Cards")) {

            String json = sp.getString(key, null);

            if (json == null) {
                adapter.setData(new ArrayList<>());
                return;
            }

            Type t =
                    new TypeToken<ArrayList<FestivalCardItem>>(){}.getType();

            ArrayList<FestivalCardItem> list =
                    gson.fromJson(json, t);

            ArrayList<String> paths = new ArrayList<>();

            if (list != null) {
                for (FestivalCardItem f : list) {
                    paths.add(f.imagePath);
                }
            }

            reverse(paths);
            adapter.setData(paths);
            return;
        }

        // ================= NORMAL =================
        String json = sp.getString(key, null);

        if (json == null) {
            adapter.setData(new ArrayList<>());
            return;
        }

        Type type =
                new TypeToken<ArrayList<String>>(){}.getType();

        ArrayList<String> images =
                gson.fromJson(json, type);

        if (images == null)
            images = new ArrayList<>();

        reverse(images);
        adapter.setData(images);
    }

    private int getColorFromAttr(int attr) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }
    void reverse(ArrayList<String> list) {

        int i = 0;
        int j = list.size() - 1;

        while (i < j) {
            String temp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, temp);
            i++;
            j--;
        }
    }


}
