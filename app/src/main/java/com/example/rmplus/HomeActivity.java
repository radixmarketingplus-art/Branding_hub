package com.example.rmplus;

import android.Manifest;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
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

    TextView  btnAll;

    RecyclerView rvTrending, rvFestivalCards;

    Handler trendingHandler;
    Runnable trendingRunnable;

    int trendingOriginalSize = 0;
    int trendingPos = 0;
    int trendingResetPoint = 0;

    ArrayList<Integer> fallback;

    // ==================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

//        String role = getIntent().getStringExtra("role");
//        setupBase(role);
        SharedPreferences sp =
                getSharedPreferences("APP_DATA", MODE_PRIVATE);

        String role = sp.getString("role", "user");

        setupBase(role, R.id.home);

        rvTrending = findViewById(R.id.rvTrending);
        rvFestivalCards = findViewById(R.id.rvFestivalCards);
        btnAll = findViewById(R.id.btnAll);

//        wherenever i will have need to remove the data of trending now section then just remove that below comments
//        SharedPreferences sp =
//                getSharedPreferences("HOME_DATA", MODE_PRIVATE);
//
//        sp.edit().remove("Trending Now").apply();


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

        // ================= PERMISSION =================
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != getPackageManager().PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        101
                );
            }
        }

        // ================= HORIZONTAL =================
        setupHorizontal(R.id.rvTrending);
        setupHorizontal(R.id.rvFestivalCards);
        setupHorizontal(R.id.rvLatest);
        setupHorizontal(R.id.rvBusinessSpecial);
        setupHorizontal(R.id.rvReelMaker);
        setupHorizontal(R.id.rvBusinessFrame);
        setupHorizontal(R.id.rvMotivation);
        setupHorizontal(R.id.rvGoodMorning);
        setupHorizontal(R.id.rvBusinessEthics);

        // ================= FESTIVAL DATES =================
        RecyclerView rvDates = findViewById(R.id.rvFestivalDates);
        rvDates.setLayoutManager(
                new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        );

        rvDates.setAdapter(
                new FestivalDateAdapter(
                        getNext7Days(),
                        this::filterFestivalByDate
                )
        );

        // ================= FALLBACK =================
        fallback = new ArrayList<>();
        fallback.add(R.drawable.ic_launcher_foreground);
        fallback.add(R.drawable.ic_launcher_foreground);

        // ================= DATA LOAD =================
        setTrending();
        loadFestivalCards();

        setSmall(R.id.rvLatest, "Latest Update");
        setSmall(R.id.rvBusinessSpecial, "Business Special");
        setSmall(R.id.rvReelMaker, "Reel Maker");
        setSmall(R.id.rvBusinessFrame, "Business Frame");
        setSmall(R.id.rvMotivation, "Motivation");
        setSmall(R.id.rvGoodMorning, "Good Morning");
        setSmall(R.id.rvBusinessEthics, "Business Ethics");

        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(rvTrending);

        trendingHandler = new Handler();
        trendingRunnable = () -> autoScrollTrending();
        trendingHandler.postDelayed(trendingRunnable, 2500);

        btnAll.setVisibility(View.GONE);
        btnAll.setOnClickListener(v ->
                filterFestivalByDate("CLEAR"));

//        rvTrending.addOnScrollListener(new RecyclerView.OnScrollListener() {
//            @Override
//            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
//                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
//
//                    LinearLayoutManager lm =
//                            (LinearLayoutManager) recyclerView.getLayoutManager();
//
//                    if (lm == null) return;
//
//                    int firstVisible = lm.findFirstCompletelyVisibleItemPosition();
//                    if (firstVisible == RecyclerView.NO_POSITION) {
//                        firstVisible = lm.findFirstVisibleItemPosition();
//                    }
//
//                    trendingPos = firstVisible;
//                    int total = recyclerView.getAdapter().getItemCount();
//
//                    // ===== LEFT END =====
//                    if (firstVisible < trendingOriginalSize) {
//                        recyclerView.scrollToPosition(
//                                firstVisible + trendingOriginalSize
//                        );
//                    }
//
//                    // ===== RIGHT END =====
//                    else if (firstVisible > total - trendingOriginalSize * 2) {
//                        recyclerView.scrollToPosition(
//                                firstVisible - trendingOriginalSize
//                        );
//                    }
//                }
//            }
//        });

    }

    // ================= ADVERTISEMENT =================

    void setAdvertisement() {

        SharedPreferences sp =
                getSharedPreferences("HOME_DATA", MODE_PRIVATE);

        String json = sp.getString("Advertisement", null);

        if (json == null) return;

        Gson gson = new Gson();
        Type type =
                new TypeToken<ArrayList<AdvertisementItem>>(){}.getType();

        ArrayList<AdvertisementItem> list =
                gson.fromJson(json, type);

        if (list == null || list.isEmpty()) return;

        // ===== HUGE LIST for infinite scroll =====
        ArrayList<AdvertisementItem> infiniteList = new ArrayList<>();
        int size = list.size();

        for (int i = 0; i < 1000; i++) {
            infiniteList.add(list.get(i % size));
        }

        rvTrending.setAdapter(
                new AdvertisementAdapter(infiniteList)
        );

        trendingPos = infiniteList.size() / 2;
        rvTrending.scrollToPosition(trendingPos);
    }


    // ==================================================

    void setupHorizontal(int id) {
        RecyclerView rv = findViewById(id);
        rv.setLayoutManager(
                new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        );
        rv.setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    // ================= TRENDING =================

    void autoScrollTrending() {

        RecyclerView.LayoutManager lm = rvTrending.getLayoutManager();
        if (!(lm instanceof LinearLayoutManager)) return;

        LinearLayoutManager layoutManager = (LinearLayoutManager) lm;

        int current =
                layoutManager.findFirstCompletelyVisibleItemPosition();

        if (current == RecyclerView.NO_POSITION) {
            current = layoutManager.findFirstVisibleItemPosition();
        }

        trendingPos = current + 1;

        rvTrending.smoothScrollToPosition(trendingPos);

        trendingHandler.postDelayed(trendingRunnable, 3000);
    }

    void setTrending() {

        ArrayList<String> original = getLocalImages("Trending Now");
        if (original == null || original.isEmpty()) return;

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
                        "Trending Now"
                )
        );

        // start near middle
        trendingPos = infiniteList.size() / 2;
        rvTrending.scrollToPosition(trendingPos);
    }

    // ================= FESTIVAL =================

    void loadFestivalCards() {

        rvFestivalCards.setLayoutManager(
                new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        );

        SharedPreferences sp = getSharedPreferences("HOME_DATA", MODE_PRIVATE);
        String json = sp.getString("Festival Cards", null);

        if (json == null) {
            setSmall(R.id.rvFestivalCards, "Festival Cards");
            return;
        }

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<FestivalCardItem>>(){}.getType();

        ArrayList<FestivalCardItem> list =
                gson.fromJson(json, type);

        if (list == null) list = new ArrayList<>();

        rvFestivalCards.setAdapter(
                new FestivalCardAdapter(list));
    }

    void filterFestivalByDate(String date) {

//        if ("CLEAR".equals(date)) {
//            loadFestivalCards();
//            btnAll.setVisibility(View.GONE);
//            return;
//        }

        if ("CLEAR".equals(date)) {

            loadFestivalCards();
            btnAll.setVisibility(View.GONE);

            RecyclerView rvDates = findViewById(R.id.rvFestivalDates);
            if (rvDates.getAdapter() instanceof FestivalDateAdapter) {
                ((FestivalDateAdapter) rvDates.getAdapter()).clearSelection();
            }
            return;
        }

        btnAll.setVisibility(View.VISIBLE);

        SharedPreferences sp = getSharedPreferences("HOME_DATA", MODE_PRIVATE);
        String json = sp.getString("Festival Cards", null);

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<FestivalCardItem>>(){}.getType();

        ArrayList<FestivalCardItem> all =
                gson.fromJson(json, type);

        ArrayList<FestivalCardItem> filtered =
                new ArrayList<>();

        SimpleDateFormat sdf =
                new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

        try {

            Date selected = sdf.parse(date);

            for (FestivalCardItem item : all) {

                Date d = sdf.parse(item.date);

                if (selected != null && d != null &&
                        sdf.format(selected).equals(sdf.format(d))) {

                    filtered.add(item);
                }
            }

        } catch (Exception e) { }

        rvFestivalCards.setAdapter(
                new FestivalCardAdapter(filtered));
    }

    // ================= SMALL =================

    void setSmall(int id, String key) {

        RecyclerView rv = findViewById(id);

        ArrayList<String> uris =
                getLocalImages(key);

        rv.setAdapter(
                new ImageAdapter(
                        uris,
                        null,
                        true,
                        R.layout.item_square,
                        key
                )
        );
    }

    // ================= DATA =================

    ArrayList<String> getLocalImages(String key) {

        SharedPreferences sp =
                getSharedPreferences("HOME_DATA", MODE_PRIVATE);

        String json = sp.getString(key, null);
        if (json == null) return new ArrayList<>();

        Gson gson = new Gson();
        Type type =
                new TypeToken<ArrayList<String>>(){}.getType();

        return gson.fromJson(json, type);
    }

    ArrayList<Calendar> getNext7Days() {

        ArrayList<Calendar> list =
                new ArrayList<>();

        Calendar cal =
                Calendar.getInstance();

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

        if (rvTrending.getAdapter() == null) {
            setAdvertisement();
        }

        if (trendingHandler != null)
            trendingHandler.postDelayed(trendingRunnable, 3000);
    }


    @Override
    protected void onPause() {
        super.onPause();

        if (trendingHandler != null)
            trendingHandler.removeCallbacks(trendingRunnable);
    }
}
