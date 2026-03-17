package com.example.rmplus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
        if (key == null)
            return "";
        if (key.contains("/")) {
            String[] parts = key.split("/");
            if (parts.length > 1) {
                return getLocalizedName(parts[0]) + " / " + getLocalizedSubCatName(parts[1]);
            }
        }
        if (key.equalsIgnoreCase("All"))
            return getString(R.string.filter_all);
        if (key.equalsIgnoreCase("Advertisement"))
            return getString(R.string.section_advertisement);
        if (key.equalsIgnoreCase("Festival Cards"))
            return getString(R.string.section_festival_cards);
        if (key.equalsIgnoreCase("Latest Update"))
            return getString(R.string.section_latest_update);
        if (key.equalsIgnoreCase("Business Special"))
            return getString(R.string.section_business_special);
        if (key.equalsIgnoreCase("Reel Maker"))
            return getString(R.string.section_reel_maker);
        if (key.equalsIgnoreCase("Business Frame"))
            return getString(R.string.section_business_frame);
        if (key.equalsIgnoreCase("Motivation"))
            return getString(R.string.section_motivation);
        if (key.equalsIgnoreCase("Greetings"))
            return getString(R.string.section_greetings);
        if (key.equalsIgnoreCase("Business Ethics"))
            return getString(R.string.section_business_ethics);
        return key;
    }

    String getLocalizedSubCatName(String key) {
        if (key == null)
            return "";
        if (key.equalsIgnoreCase("Political"))
            return getString(R.string.cat_political);
        if (key.equalsIgnoreCase("NGO"))
            return getString(R.string.cat_ngo);
        if (key.equalsIgnoreCase("Business"))
            return getString(R.string.cat_business);
        return key;
    }

    String currentCategory = "All";

    public CheckUploadsFragment() {
        super(R.layout.fragment_check_uploads);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        super.onViewCreated(v, b);

        recycler = v.findViewById(R.id.recyclerTemplates);
        filterContainer = v.findViewById(R.id.filterContainer);
        tabScroll = v.findViewById(R.id.tabScroll);

        recycler.setLayoutManager(new GridLayoutManager(requireContext(), 3));

        adapter = new TemplateGridAdapter(allTemplates, R.layout.item_grid_square, t -> {
            Intent i = new Intent(requireContext(), AdminTemplateDetailActivity.class);
            i.putExtra("id", t.id);
            i.putExtra("path", t.url);
            i.putExtra("category", t.category);
            startActivity(i);
        });

        recycler.setAdapter(adapter);

        createFilterButtons();
        loadCategory("All");
    }

    void createFilterButtons() {
        filterContainer.removeAllViews();
        String[] categories = { "All", "Advertisement", "Festival Cards", "Latest Update", "Business Special",
                "Reel Maker", "Business Frame", "Motivation", "Greetings", "Business Ethics" };

        for (String c : categories) {
            TextView chip = new TextView(requireContext());
            chip.setText(getLocalizedName(c));
            chip.setTag(c);
            chip.setTextSize(14);
            chip.setPadding(36, 16, 36, 16);
            chip.setBackgroundResource(R.drawable.bg_filter_chip);
            chip.setTextColor(getResources().getColor(android.R.color.darker_gray, requireContext().getTheme()));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
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
                chip.setTextColor(getColorFromAttr(com.google.android.material.R.attr.colorOnSurface));
                chip.setBackgroundResource(R.drawable.bg_filter_chip_selected);
            } else {
                chip.setTypeface(null, android.graphics.Typeface.NORMAL);
                chip.setTextColor(getResources().getColor(android.R.color.darker_gray, requireContext().getTheme()));
                chip.setBackgroundResource(R.drawable.bg_filter_chip);
            }
        }
    }

    // ---------------- DATA (FIREBASE LIVE) ----------------

    void loadCategory(String key) {
        currentCategory = key;
        adapter.setData(new ArrayList<>());
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("templates");

        if (key.equalsIgnoreCase("All")) {
            dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    ArrayList<TemplateModel> result = new ArrayList<>();
                    for (DataSnapshot catSnap : snapshot.getChildren()) {
                        String catName = catSnap.getKey();
                        if (catName == null || catName.equalsIgnoreCase("Frame"))
                            continue;
                        collectTemplatesRecursive(catSnap, catName, result);
                    }
                    adapter.setData(result);
                }
                @Override public void onCancelled(@NonNull DatabaseError error) { toast(error.getMessage()); }
            });
        } else {
            dbRef.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    ArrayList<TemplateModel> result = new ArrayList<>();
                    collectTemplatesRecursive(snapshot, key, result);
                    adapter.setData(result);
                }
                @Override public void onCancelled(@NonNull DatabaseError error) { toast(error.getMessage()); }
            });
        }
    }

    private void collectTemplatesRecursive(DataSnapshot snapshot, String rootCategory, ArrayList<TemplateModel> result) {
        if (snapshot == null) return;
        if (snapshot.hasChild("url") || snapshot.hasChild("imagePath")) {
            String url = snapshot.hasChild("url") ? snapshot.child("url").getValue(String.class)
                    : snapshot.child("imagePath").getValue(String.class);
            String type = snapshot.child("type").getValue(String.class);
            
            // Get the full category path relative to "templates"
            String fullPath = snapshot.getRef().getPath().toString(); 
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

            if (url != null) {
                result.add(0, new TemplateModel(snapshot.getKey(), url, cat, date, type));
            }
        } else {
            for (DataSnapshot child : snapshot.getChildren()) {
                collectTemplatesRecursive(child, rootCategory, result);
            }
        }
    }



    private void toast(String msg) {
        if (isAdded())
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private int getColorFromAttr(int attr) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        requireContext().getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }
}