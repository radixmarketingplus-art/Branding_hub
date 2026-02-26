package com.example.rmplus;

import android.Manifest;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;

public class HomeActivity extends BaseActivity {

    TextView btnAll;

    RecyclerView rvTrending, rvFestivalCards;

    Handler trendingHandler;
    Runnable trendingRunnable;

    int trendingOriginalSize = 0;
    int trendingPos = 0;
    int trendingResetPoint = 0;

    ArrayList<Integer> fallback;
    View skTrending, skFestival;

    // ==================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // String role = getIntent().getStringExtra("role");
        // setupBase(role);
        SharedPreferences sp = getSharedPreferences("APP_DATA", MODE_PRIVATE);

        String role = sp.getString("role", "user");

        setupBase(role, R.id.home);

        rvTrending = findViewById(R.id.rvTrending);
        rvFestivalCards = findViewById(R.id.rvFestivalCards);
        btnAll = findViewById(R.id.btnAll);
        skTrending = findViewById(R.id.skTrending);
        skFestival = findViewById(R.id.skFestival);

        startPulse(skTrending);
        startPulse(skFestival);

        // wherenever i will have need to remove the data of trending now section then
        // just remove that below comments
        // SharedPreferences sp =
        // getSharedPreferences("HOME_DATA", MODE_PRIVATE);
        //
        // sp.edit().remove("Trending Now").apply();

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (view, insets) -> {

            // ❌ bottom inset consume mat karo
            view.setPadding(
                    view.getPaddingLeft(),
                    view.getPaddingTop(),
                    view.getPaddingRight(),
                    0);

            return insets;
        });

        // ================= PERMISSION =================
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != getPackageManager().PERMISSION_GRANTED) {

                requestPermissions(
                        new String[] { Manifest.permission.POST_NOTIFICATIONS },
                        101);
            }
        }

        // ================= HORIZONTAL =================
        setupHorizontal(R.id.rvTrending);
        setupHorizontal(R.id.rvFestivalCards);
        // Dynamic horizontal lists will be setup programmatically

        // ================= FESTIVAL DATES =================
        RecyclerView rvDates = findViewById(R.id.rvFestivalDates);
        rvDates.setLayoutManager(
                new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));

        rvDates.setAdapter(
                new FestivalDateAdapter(
                        getNext7Days(),
                        this::filterFestivalByDate));

        // ================= FALLBACK =================
        fallback = new ArrayList<>();
        fallback.add(R.drawable.ic_launcher_foreground);
        fallback.add(R.drawable.ic_launcher_foreground);

        // ================= DATA LOAD =================
        loadDynamicHomeSections();
        loadFestivalCardsLive();
        loadHeroSectionLive();

        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(rvTrending);

        trendingHandler = new Handler();
        trendingRunnable = () -> autoScrollTrending();
        trendingHandler.postDelayed(trendingRunnable, 2500);

        btnAll.setVisibility(View.VISIBLE); // Show it permanently as a gallery link
        btnAll.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TemplateGalleryActivity.class);
            intent.putExtra("category", "Festival Cards");
            startActivity(intent);
        });

        // rvTrending.addOnScrollListener(new RecyclerView.OnScrollListener() {
        // @Override
        // public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        // if (newState == RecyclerView.SCROLL_STATE_IDLE) {
        //
        // LinearLayoutManager lm =
        // (LinearLayoutManager) recyclerView.getLayoutManager();
        //
        // if (lm == null) return;
        //
        // int firstVisible = lm.findFirstCompletelyVisibleItemPosition();
        // if (firstVisible == RecyclerView.NO_POSITION) {
        // firstVisible = lm.findFirstVisibleItemPosition();
        // }
        //
        // trendingPos = firstVisible;
        // int total = recyclerView.getAdapter().getItemCount();
        //
        // // ===== LEFT END =====
        // if (firstVisible < trendingOriginalSize) {
        // recyclerView.scrollToPosition(
        // firstVisible + trendingOriginalSize
        // );
        // }
        //
        // // ===== RIGHT END =====
        // else if (firstVisible > total - trendingOriginalSize * 2) {
        // recyclerView.scrollToPosition(
        // firstVisible - trendingOriginalSize
        // );
        // }
        // }
        // }
        // });

        // CHECK FOR EXPIRED TEMPLATES
        ExpiryCleanupHelper.checkAndClean(this);
    }

    // ================= ADVERTISEMENT & TRENDING =================

    void loadHeroSectionLive() {
        FirebaseDatabase.getInstance().getReference("templates")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot root) {
                        safelyHideSkeleton(skTrending);
                        DataSnapshot ads = root.child("Advertisement");
                        ArrayList<AdvertisementItem> adList = new ArrayList<>();
                        for (DataSnapshot d : ads.getChildren()) {
                            String url = d.hasChild("imagePath") ? d.child("imagePath").getValue(String.class)
                                    : d.child("url").getValue(String.class);
                            String link = d.child("link").getValue(String.class);
                            if (url != null)
                                adList.add(new AdvertisementItem(url, link != null ? link : ""));
                        }

                        if (!adList.isEmpty()) {
                            // USE ADVERTISEMENT ADAPTER
                            ArrayList<AdvertisementItem> infiniteList = new ArrayList<>();
                            int size = adList.size();
                            for (int i = 0; i < 1000; i++)
                                infiniteList.add(adList.get(i % size));
                            rvTrending.setAdapter(new AdvertisementAdapter(infiniteList));
                            trendingOriginalSize = size;
                        } else {
                            // FALLBACK TO TRENDING NOW
                            DataSnapshot trending = root.child("Trending Now");
                            ArrayList<TemplateModel> trendList = new ArrayList<>();
                            for (DataSnapshot d : trending.getChildren()) {
                                String url = d.hasChild("imagePath") ? d.child("imagePath").getValue(String.class)
                                        : d.child("url").getValue(String.class);
                                if (url != null)
                                    trendList.add(new TemplateModel(d.getKey(), url, "Trending Now"));
                            }
                            if (trendList.isEmpty())
                                return;
                            trendingOriginalSize = trendList.size();
                            ArrayList<TemplateModel> infiniteList = new ArrayList<>();
                            for (int i = 0; i < 1000; i++)
                                infiniteList.add(trendList.get(i % trendingOriginalSize));
                            rvTrending.setAdapter(new TemplateGridAdapter(infiniteList, t -> {
                                Intent i = new Intent(HomeActivity.this, TemplatePreviewActivity.class);
                                i.putExtra("id", t.id);
                                i.putExtra("path", t.url);
                                i.putExtra("category", "Trending Now");
                                startActivity(i);
                            }));
                        }
                        trendingPos = 500; // Start at middle
                        rvTrending.scrollToPosition(trendingPos);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                    }
                });
    }

    // ==================================================

    void setupHorizontal(int id) {
        RecyclerView rv = findViewById(id);
        rv.setLayoutManager(
                new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        rv.setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    // ================= TRENDING =================

    void autoScrollTrending() {

        RecyclerView.LayoutManager lm = rvTrending.getLayoutManager();
        if (!(lm instanceof LinearLayoutManager))
            return;

        LinearLayoutManager layoutManager = (LinearLayoutManager) lm;

        int current = layoutManager.findFirstCompletelyVisibleItemPosition();

        if (current == RecyclerView.NO_POSITION) {
            current = layoutManager.findFirstVisibleItemPosition();
        }

        trendingPos = current + 1;

        rvTrending.smoothScrollToPosition(trendingPos);

        trendingHandler.postDelayed(trendingRunnable, 3000);
    }

    void setTrending() {

        ArrayList<String> original = getImages("Trending Now");
        if (original == null || original.isEmpty())
            return;

        // remove duplicates
        original = new ArrayList<>(new LinkedHashSet<>(original));
        trendingOriginalSize = original.size();

        // ===== HUGE LIST (no jumping needed) =====
        ArrayList<String> infiniteList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            infiniteList.add(original.get(i % trendingOriginalSize));
        }

        rvTrending.setAdapter(
                new ImageAdapter(
                        infiniteList,
                        null,
                        true,
                        R.layout.item_trending_hotstar,
                        "Trending Now"));

        // start near middle
        trendingPos = infiniteList.size() / 2;
        rvTrending.scrollToPosition(trendingPos);
    }

    // ================= FESTIVAL =================

    void loadFestivalCards() {

        rvFestivalCards.setLayoutManager(
                new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));

        SharedPreferences sp = getSharedPreferences("HOME_DATA", MODE_PRIVATE);
        String json = sp.getString("Festival Cards", null);

        if (json == null) {
            setSmall(R.id.rvFestivalCards, "Festival Cards");
            return;
        }

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<FestivalCardItem>>() {
        }.getType();

        ArrayList<FestivalCardItem> list = gson.fromJson(json, type);

        if (list == null)
            list = new ArrayList<>();

        rvFestivalCards.setAdapter(
                new FestivalCardAdapter(list));
    }

    void filterFestivalByDate(String date) {

        if ("CLEAR".equals(date)) {
            if (skFestival != null) {
                skFestival.setVisibility(View.VISIBLE);
                startPulse(skFestival);
            }
            loadFestivalCardsLive();
            btnAll.setVisibility(View.GONE);

            RecyclerView rvDates = findViewById(R.id.rvFestivalDates);
            if (rvDates.getAdapter() instanceof FestivalDateAdapter) {
                ((FestivalDateAdapter) rvDates.getAdapter()).clearSelection();
            }
            return;
        }

        btnAll.setVisibility(View.VISIBLE);

        // Fetching live data for filtering
        FirebaseDatabase.getInstance().getReference("templates").child("Festival Cards")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        safelyHideSkeleton(skFestival);
                        ArrayList<TemplateModel> all = new ArrayList<>();
                        for (DataSnapshot d : snapshot.getChildren()) {
                            String url = d.hasChild("imagePath") ? d.child("imagePath").getValue(String.class)
                                    : d.child("url").getValue(String.class);
                            String itemDate = d.child("date").getValue(String.class);
                            if (url != null) {
                                all.add(new TemplateModel(d.getKey(), url, "Festival Cards", itemDate));
                            }
                        }

                        ArrayList<TemplateModel> filtered = new ArrayList<>();
                        // Standardize to "d-M-yyyy" to handle non-padded dates from Upload (1-2-2026)
                        // and padded dates from Filter (01-02-2026)
                        SimpleDateFormat sdf = new SimpleDateFormat("d-M-yyyy", Locale.US); // ✅ Locale.US: avoid Hindi
                                                                                            // Devanagari digits

                        try {
                            Date selectedDateObj = sdf.parse(date);
                            for (TemplateModel item : all) {
                                if (item.date != null) {
                                    try {
                                        Date itemDateObj = sdf.parse(item.date);
                                        if (selectedDateObj.equals(itemDateObj)) {
                                            filtered.add(item);
                                        }
                                    } catch (Exception e) {
                                        // fallback
                                        if (item.date.equals(date))
                                            filtered.add(item);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        rvFestivalCards.setAdapter(new TemplateGridAdapter(filtered, t -> {
                            Intent i = new Intent(HomeActivity.this, TemplatePreviewActivity.class);
                            i.putExtra("id", t.id);
                            i.putExtra("path", t.url);
                            i.putExtra("category", t.category);
                            startActivity(i);
                        }));
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        toast("Error filtering: " + error.getMessage());
                    }
                });
    }

    // ================= SMALL =================

    void setSmall(int id, String key) {

        RecyclerView rv = findViewById(id);

        ArrayList<String> uris = getImages(key);

        rv.setAdapter(
                new ImageAdapter(
                        uris,
                        null,
                        true,
                        R.layout.item_square,
                        key));
    }

    void loadDynamicHomeSections() {
        android.widget.LinearLayout container = findViewById(R.id.dynamicSectionContainer);
        container.removeAllViews();

        FirebaseDatabase.getInstance().getReference("templates")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                            String key = categorySnapshot.getKey();
                            if (key == null || key.equals("Trending Now") || key.equals("Festival Cards")
                                    || key.equals("Advertisement") || key.equals("Frame")) {
                                continue;
                            }

                            // Inflate section layout
                            View sectionView = getLayoutInflater().inflate(R.layout.item_home_section, container,
                                    false);
                            TextView txtTitle = sectionView.findViewById(R.id.txtSectionTitle);
                            TextView btnSectionSeeAll = sectionView.findViewById(R.id.btnSectionSeeAll);
                            RecyclerView rvItems = sectionView.findViewById(R.id.rvSectionItems);
                            LinearLayout filterContainer = sectionView.findViewById(R.id.filterContainer);
                            View scrollFilters = sectionView.findViewById(R.id.scrollFilters);
                            View skSection = sectionView.findViewById(R.id.skSection);

                            startPulse(skSection);

                            txtTitle.setText(getLocalizedSectionName(key));
                            rvItems.setLayoutManager(
                                    new LinearLayoutManager(HomeActivity.this, RecyclerView.HORIZONTAL, false));

                            btnSectionSeeAll.setOnClickListener(v -> {
                                Intent intent = new Intent(HomeActivity.this, TemplateGalleryActivity.class);
                                intent.putExtra("category", key);
                                startActivity(intent);
                            });

                            if (key.equalsIgnoreCase("Business Frame")) {
                                scrollFilters.setVisibility(View.VISIBLE);
                                ArrayList<String> subcats = new ArrayList<>();
                                subcats.add("All");
                                subcats.add("Political");
                                subcats.add("NGO");
                                subcats.add("Business");

                                for (int i = 0; i < subcats.size(); i++) {
                                    String sub = subcats.get(i);
                                    TextView chip = new TextView(HomeActivity.this);

                                    String displaySub = sub;
                                    if (sub.equalsIgnoreCase("All"))
                                        displaySub = getString(R.string.filter_all);
                                    else if (sub.equalsIgnoreCase("Political"))
                                        displaySub = getString(R.string.cat_political);
                                    else if (sub.equalsIgnoreCase("NGO"))
                                        displaySub = getString(R.string.cat_ngo);
                                    else if (sub.equalsIgnoreCase("Business"))
                                        displaySub = getString(R.string.cat_business);

                                    chip.setText(displaySub);
                                    chip.setPadding(40, 16, 40, 16);
                                    chip.setTextColor(getResources().getColor(R.color.text_secondary));
                                    chip.setTextSize(13);
                                    chip.setAllCaps(false);
                                    chip.setTypeface(android.graphics.Typeface.create("sans-serif-medium",
                                            android.graphics.Typeface.NORMAL));

                                    android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                                    params.setMargins(12, 0, 12, 0);
                                    chip.setLayoutParams(params);
                                    chip.setBackgroundResource(R.drawable.bg_filter_chip);
                                    chip.setOnClickListener(v -> {
                                        // Update UI for chips
                                        for (int j = 0; j < filterContainer.getChildCount(); j++) {
                                            TextView c = (TextView) filterContainer.getChildAt(j);
                                            c.setBackgroundResource(R.drawable.bg_filter_chip);
                                            c.setTextColor(getResources().getColor(R.color.text_secondary));
                                        }
                                        chip.setBackgroundResource(R.drawable.bg_filter_chip_selected);
                                        chip.setTextColor(android.graphics.Color.WHITE);
                                        // Load sub-category data
                                        loadSubCategoryData(key, sub, rvItems, skSection);
                                    });
                                    filterContainer.addView(chip);
                                }
                                // Load 'All' by default
                                ((TextView) filterContainer.getChildAt(0)).performClick();
                                container.addView(sectionView);
                            } else {
                                ArrayList<TemplateModel> list = new ArrayList<>();
                                for (DataSnapshot itemSnapshot : categorySnapshot.getChildren()) {
                                    String url = itemSnapshot.hasChild("imagePath")
                                            ? itemSnapshot.child("imagePath").getValue(String.class)
                                            : itemSnapshot.child("url").getValue(String.class);
                                    if (url != null) {
                                        list.add(new TemplateModel(itemSnapshot.getKey(), url, key));
                                    }
                                }

                                if (!list.isEmpty()) {
                                    safelyHideSkeleton(skSection);
                                    rvItems.setAdapter(new TemplateGridAdapter(list, t -> {
                                        Intent i = new Intent(HomeActivity.this, TemplatePreviewActivity.class);
                                        i.putExtra("id", t.id);
                                        i.putExtra("path", t.url);
                                        i.putExtra("category", t.category);
                                        startActivity(i);
                                    }));
                                    container.addView(sectionView);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                    }
                });
    }

    private void loadSubCategoryData(String parentKey, String subKey, RecyclerView rvItems, View skSection) {
        if (skSection != null) {
            skSection.setVisibility(View.VISIBLE);
            startPulse(skSection);
        }
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("templates").child(parentKey);

        if ("All".equalsIgnoreCase(subKey)) {
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    ArrayList<TemplateModel> allList = new ArrayList<>();
                    for (DataSnapshot subSnapshot : snapshot.getChildren()) {
                        // Skip if it's not a sub-category node (though typically they all are here)
                        for (DataSnapshot itemSnapshot : subSnapshot.getChildren()) {
                            String url = itemSnapshot.hasChild("imagePath")
                                    ? itemSnapshot.child("imagePath").getValue(String.class)
                                    : itemSnapshot.child("url").getValue(String.class);
                            if (url != null) {
                                allList.add(new TemplateModel(itemSnapshot.getKey(), url,
                                        parentKey + "/" + subSnapshot.getKey()));
                            }
                        }
                    }
                    rvItems.setAdapter(new TemplateGridAdapter(allList, t -> {
                        Intent i = new Intent(HomeActivity.this, TemplatePreviewActivity.class);
                        i.putExtra("id", t.id);
                        i.putExtra("path", t.url);
                        i.putExtra("category", t.category);
                        startActivity(i);
                    }));
                    safelyHideSkeleton(skSection);
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    safelyHideSkeleton(skSection);
                }
            });
        } else {
            ref.child(subKey).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    ArrayList<TemplateModel> list = new ArrayList<>();
                    for (DataSnapshot d : snapshot.getChildren()) {
                        String url = d.hasChild("imagePath") ? d.child("imagePath").getValue(String.class)
                                : d.child("url").getValue(String.class);
                        if (url != null) {
                            list.add(new TemplateModel(d.getKey(), url, parentKey + "/" + subKey));
                        }
                    }
                    rvItems.setAdapter(new TemplateGridAdapter(list, t -> {
                        Intent i = new Intent(HomeActivity.this, TemplatePreviewActivity.class);
                        i.putExtra("id", t.id);
                        i.putExtra("path", t.url);
                        i.putExtra("category", t.category);
                        startActivity(i);
                    }));
                    safelyHideSkeleton(skSection);
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    safelyHideSkeleton(skSection);
                }
            });
        }
    }

    void loadSectionLive(int recyclerId, String key) {
        RecyclerView rv = findViewById(recyclerId);
        FirebaseDatabase.getInstance().getReference("templates").child(key)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        ArrayList<TemplateModel> list = new ArrayList<>();
                        for (DataSnapshot d : snapshot.getChildren()) {
                            String url = d.hasChild("imagePath") ? d.child("imagePath").getValue(String.class)
                                    : d.child("url").getValue(String.class);
                            if (url != null) {
                                list.add(new TemplateModel(d.getKey(), url, key));
                            }
                        }
                        // Use a new TemplateModel adapter or update ImageAdapter
                        rv.setAdapter(new TemplateGridAdapter(list, t -> {
                            Intent i = new Intent(HomeActivity.this, TemplatePreviewActivity.class);
                            i.putExtra("id", t.id);
                            i.putExtra("path", t.url);
                            i.putExtra("category", t.category);
                            startActivity(i);
                        }));
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                    }
                });
    }

    void loadFestivalCardsLive() {
        FirebaseDatabase.getInstance().getReference("templates").child("Festival Cards")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        safelyHideSkeleton(skFestival);
                        ArrayList<TemplateModel> list = new ArrayList<>();
                        for (DataSnapshot d : snapshot.getChildren()) {
                            String url = d.hasChild("imagePath") ? d.child("imagePath").getValue(String.class)
                                    : d.child("url").getValue(String.class);
                            String date = d.child("date").getValue(String.class);
                            if (url != null) {
                                list.add(new TemplateModel(d.getKey(), url, "Festival Cards", date));
                            }
                        }
                        rvFestivalCards.setAdapter(new TemplateGridAdapter(list, t -> {
                            Intent i = new Intent(HomeActivity.this, TemplatePreviewActivity.class);
                            i.putExtra("id", t.id);
                            i.putExtra("path", t.url);
                            i.putExtra("category", "Festival Cards");
                            startActivity(i);
                        }));
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                    }
                });
    }

    // ================= DATA =================

    ArrayList<String> getImages(String key) {

        SharedPreferences sp = getSharedPreferences("HOME_DATA", MODE_PRIVATE);

        String json = sp.getString(key, null);
        if (json == null)
            return new ArrayList<>();

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();

        ArrayList<String> list = gson.fromJson(json, type);

        return list == null ? new ArrayList<>() : list;
    }

    ArrayList<Calendar> getNext7Days() {

        ArrayList<Calendar> list = new ArrayList<>();

        // 🌟 Add null at the start for the "All" filter chip
        list.add(null);

        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < 7; i++) {
            Calendar c = (Calendar) cal.clone();
            c.add(Calendar.DAY_OF_MONTH, i);
            list.add(c);
        }

        return list;
    }

    // ================= LIFECYCLE =================

    @Override
    protected void onResume() {
        super.onResume();

        loadFestivalCards(); // ok

        loadHeroSectionLive(); // Merged loadAdvertisementLive and loadTrendingLive

        if (trendingHandler != null)
            trendingHandler.postDelayed(trendingRunnable, 3000);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (trendingHandler != null)
            trendingHandler.removeCallbacks(trendingRunnable);
    }

    void toast(String msg) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
    }

    private void startPulse(View v) {
        if (v == null)
            return;
        android.view.animation.Animation pulse = android.view.animation.AnimationUtils.loadAnimation(this,
                R.anim.skeleton_pulse);
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) v;
            for (int i = 0; i < group.getChildCount(); i++) {
                group.getChildAt(i).startAnimation(pulse);
            }
        } else {
            v.startAnimation(pulse);
        }
    }

    private void safelyHideSkeleton(View v) {
        if (v == null)
            return;
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) v;
            for (int i = 0; i < group.getChildCount(); i++) {
                group.getChildAt(i).clearAnimation();
            }
        }
        v.clearAnimation();
        v.setVisibility(View.GONE);
    }
}
