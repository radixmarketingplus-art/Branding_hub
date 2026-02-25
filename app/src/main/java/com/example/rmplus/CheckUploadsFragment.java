package com.example.rmplus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class CheckUploadsFragment extends Fragment {

    RecyclerView recycler;
    LinearLayout filterContainer;
    HorizontalScrollView tabScroll;

    TemplateGridAdapter adapter;
    ArrayList<TemplateModel> allTemplates = new ArrayList<>();

    String getLocalizedName(String key) {
        if (key == null) return "";
        if (key.contains("/")) {
            String[] parts = key.split("/");
            if (parts.length > 1) {
                return getLocalizedName(parts[0]) + " / " + getLocalizedSubCatName(parts[1]);
            }
        }
        if (key.equalsIgnoreCase("All")) return getString(R.string.filter_all);
        if (key.equalsIgnoreCase("Advertisement")) return getString(R.string.section_advertisement);
        if (key.equalsIgnoreCase("Festival Cards")) return getString(R.string.section_festival_cards);
        if (key.equalsIgnoreCase("Latest Update")) return getString(R.string.section_latest_update);
        if (key.equalsIgnoreCase("Business Special")) return getString(R.string.section_business_special);
        if (key.equalsIgnoreCase("Reel Maker")) return getString(R.string.section_reel_maker);
        if (key.equalsIgnoreCase("Business Frame")) return getString(R.string.section_business_frame);
        if (key.equalsIgnoreCase("Motivation")) return getString(R.string.section_motivation);
        if (key.equalsIgnoreCase("Greetings")) return getString(R.string.section_greetings);
        if (key.equalsIgnoreCase("Business Ethics")) return getString(R.string.section_business_ethics);
        if (key.equalsIgnoreCase("Frame")) return getString(R.string.section_frame);
        return key;
    }

    String getLocalizedSubCatName(String key) {
        if (key == null) return "";
        if (key.equalsIgnoreCase("Political")) return getString(R.string.cat_political);
        if (key.equalsIgnoreCase("NGO")) return getString(R.string.cat_ngo);
        if (key.equalsIgnoreCase("Business")) return getString(R.string.cat_business);
        return key;
    }

    String currentCategory = "All";

    public CheckUploadsFragment() {
        super(R.layout.fragment_check_uploads);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        super.onViewCreated(v, b);

        // ✅ FIXED IDS
        recycler = v.findViewById(R.id.recyclerTemplates);
        filterContainer = v.findViewById(R.id.filterContainer);
        tabScroll = v.findViewById(R.id.tabScroll);

        recycler.setLayoutManager(
                new GridLayoutManager(requireContext(), 3)
        );

        adapter = new TemplateGridAdapter(allTemplates, t -> {
            Intent i = new Intent(
                    requireContext(),
                    AdminTemplateDetailActivity.class
            );
            i.putExtra("path", t.url);
            i.putExtra("category", currentCategory);
            startActivity(i);
        });

        recycler.setAdapter(adapter);

        createFilterButtons();
        loadCategory("All");
    }

    // ---------------- TABS (MATCHES TemplateGalleryActivity) ----------------

    void createFilterButtons() {

        filterContainer.removeAllViews();

        String[] categories = {
                "All",
                "Advertisement",
                "Festival Cards",
                "Latest Update",
                "Business Special",
                "Reel Maker",
                "Business Frame",
                "Motivation",
                "Greetings",
                "Business Ethics",
                "Frame"
        };
        
        for (String c : categories) {
            TextView chip = new TextView(requireContext());
            chip.setText(getLocalizedName(c));
            chip.setTag(c);
            chip.setTextSize(14);
            chip.setPadding(36, 16, 36, 16);
            chip.setBackgroundResource(R.drawable.bg_filter_chip);
            chip.setTextColor(
                    getResources().getColor(
                            android.R.color.darker_gray,
                            requireContext().getTheme()
                    )
            );

            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
            lp.setMargins(8, 0, 8, 0);
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> {
                currentCategory = (String) v.getTag();
                updateTabUI();
                loadCategory(currentCategory);
            });

            filterContainer.addView(chip);
        }

        updateTabUI();
    }

    void updateTabUI() {

        for (int i = 0; i < filterContainer.getChildCount(); i++) {

            TextView chip = (TextView) filterContainer.getChildAt(i);
            if (chip.getTag().toString().equals(currentCategory)) {
                chip.setTypeface(null, android.graphics.Typeface.BOLD);
                chip.setTextColor(
                        getColorFromAttr(
                                com.google.android.material.R.attr.colorOnSurface
                        )
                );
                chip.setBackgroundResource(
                        R.drawable.bg_filter_chip_selected
                );
            } else {
                chip.setTypeface(null, android.graphics.Typeface.NORMAL);
                chip.setTextColor(
                        getResources().getColor(
                                android.R.color.darker_gray,
                                requireContext().getTheme()
                        )
                );
                chip.setBackgroundResource(
                        R.drawable.bg_filter_chip
                );
            }
        }
    }

    // ---------------- DATA (UNCHANGED) ----------------

    void loadCategory(String key) {

        currentCategory = key;

        SharedPreferences sp =
                requireContext().getSharedPreferences(
                        "HOME_DATA",
                        requireContext().MODE_PRIVATE
                );

        Gson gson = new Gson();
        ArrayList<TemplateModel> result = new ArrayList<>();

        if (key.equals("All")) {

            String[] allKeys = {
                    "Festival Cards",
                    "Latest Update",
                    "Business Special",
                    "Reel Maker",
                    "Business Frame",
                    "Motivation",
                    "Greetings",
                    "Business Ethics",
                    "Frame"
            };

            for (String k : allKeys) {

                String json = sp.getString(k, null);
                if (json == null) continue;

                if (k.equals("Festival Cards")) {

                    Type t =
                            new TypeToken<ArrayList<FestivalCardItem>>(){}.getType();

                    ArrayList<FestivalCardItem> list =
                            gson.fromJson(json, t);

                    if (list != null) {
                        for (FestivalCardItem f : list) {
                            result.add(new TemplateModel(makeSafeKey(f.imagePath), f.imagePath, k));
                        }
                    }
                } else {

                    Type t =
                            new TypeToken<ArrayList<String>>(){}.getType();

                    ArrayList<String> list =
                            gson.fromJson(json, t);

                    if (list != null) {
                        for (String s : list) {
                            result.add(new TemplateModel(makeSafeKey(s), s, k));
                        }
                    }
                }
            }

            // reverse(result);
            adapter.setData(result);
            return;
        }

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

            ArrayList<TemplateModel> paths = new ArrayList<>();
            if (list != null) {
                for (FestivalCardItem f : list) {
                    paths.add(new TemplateModel(makeSafeKey(f.imagePath), f.imagePath, key));
                }
            }
 
            // reverse(paths);
            adapter.setData(paths);
            return;
        }

        String json = sp.getString(key, null);
        if (json == null) {
            adapter.setData(new ArrayList<>());
            return;
        }

        // ==================================================
        // 📢 ADVERTISEMENT SECTION (OBJECT LIST)
        // ==================================================

        if (key.equals("Advertisement")) {

            Type t =
                    new TypeToken<ArrayList<AdvertisementItem>>(){}.getType();

            ArrayList<AdvertisementItem> list =
                    gson.fromJson(json, t);

            ArrayList<TemplateModel> paths = new ArrayList<>();
 
            if (list != null) {
                for (AdvertisementItem ad : list) {
                    paths.add(new TemplateModel(makeSafeKey(ad.imagePath), ad.imagePath, key));
                }
            }
 
            // reverse(paths);
            adapter.setData(paths);
            return;
        }

        // ==================================================
        // 🧩 NORMAL SECTIONS (STRING LIST)
        // ==================================================

        Type type =
                new TypeToken<ArrayList<String>>(){}.getType();

        ArrayList<String> images =
                gson.fromJson(json, type);
 
        ArrayList<TemplateModel> finalResult = new ArrayList<>();
        if (images != null) {
            for (String s : images) {
                finalResult.add(new TemplateModel(makeSafeKey(s), s, key));
            }
        }
 
        // reverse(images);
        adapter.setData(finalResult);
    }

    private String makeSafeKey(String s) {
        return android.util.Base64.encodeToString(s.getBytes(), android.util.Base64.NO_WRAP);
    }

    private int getColorFromAttr(int attr) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        requireContext().getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }


}