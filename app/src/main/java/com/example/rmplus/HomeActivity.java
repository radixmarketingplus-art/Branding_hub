package com.example.rmplus;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.content.Intent;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;

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
import com.bumptech.glide.Glide;
import android.widget.Toast;

public class HomeActivity extends BaseActivity {

    private static boolean appOpenOfferShown = false;

    RecyclerView rvTrending, rvFestivalCards;
    androidx.core.widget.NestedScrollView mainScroll;
    TextView btnAll;
    FestivalDateAdapter festivalDateAdapter;
    String targetId = null;
    String targetAdId = null;

    Handler trendingHandler;
    Runnable trendingRunnable;

    int trendingOriginalSize = 0;
    int trendingPos = 0;
    long doubleBackToExitPressedOnce = 0;
    PagerSnapHelper trendingSnapHelper;

    ArrayList<Integer> fallback;
    View skTrending, skFestival;

    // ==================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        targetId = getIntent().getStringExtra("target_template_id");
        targetAdId = getIntent().getStringExtra("target_ad_id");
        // setupBase(role);
        SharedPreferences sp = getSharedPreferences("APP_DATA", MODE_PRIVATE);

        String role = sp.getString("role", "user");

        setupBase(role, R.id.home);

        rvTrending = findViewById(R.id.rvTrending);
        rvFestivalCards = findViewById(R.id.rvFestivalCards);
        mainScroll = findViewById(R.id.mainScroll);
        btnAll = findViewById(R.id.btnAll);
        skTrending = findViewById(R.id.skTrending);
        skFestival = findViewById(R.id.skFestival);

        startPulse(skTrending);
        startPulse(skFestival);

        // ✅ REQUEST NOTIFICATION PERMISSION (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

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

        festivalDateAdapter = new FestivalDateAdapter(
                getNext7Days(),
                this::filterFestivalByDate);
        rvDates.setAdapter(festivalDateAdapter);

        // ================= FALLBACK =================
        fallback = new ArrayList<>();
        fallback.add(R.drawable.ic_launcher_foreground);
        fallback.add(R.drawable.ic_launcher_foreground);

        // ================= DATA LOAD =================
        loadDynamicHomeSections();
        loadAllFestivalCardsLive();
        loadHeroSectionLive();

        trendingSnapHelper = new PagerSnapHelper();
        trendingSnapHelper.attachToRecyclerView(rvTrending);

        trendingHandler = new Handler();
        trendingRunnable = () -> autoScrollTrending();
        // Removed redundant postDelayed here, will start in onResume

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

        if (!appOpenOfferShown) {
            checkAndShowAppOpenOffer();
        }

        // ✅ SHOW LOGIN SUCCESS POPUP (If just logged in)
        boolean showOnboarding = getIntent().getBooleanExtra("show_onboarding", false);
        if (showOnboarding) {
            getIntent().removeExtra("show_onboarding");
        }
        if (getIntent().getBooleanExtra("show_login_success", false)) {
            getIntent().removeExtra("show_login_success");
            showLoginSuccessPopup(showOnboarding);
        } else if (showOnboarding) {
            // If only onboarding (no success popup), start directly
            new Handler().postDelayed(this::startSpotlightOnboarding, 800);
        }
    }

    private void checkAndShowAppOpenOffer() {
        FirebaseDatabase.getInstance().getReference("admin_settings").child("app_open_offer")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        try {
                            if (!isFinishing() && !isDestroyed() && snapshot.exists()) {
                                Boolean isEnabled = snapshot.child("enabled").getValue(Boolean.class);
                                String imgUrl = snapshot.child("imageUrl").getValue(String.class);
                                String actionLink = snapshot.child("actionLink").getValue(String.class);

                                if (isEnabled != null && isEnabled && imgUrl != null && !imgUrl.isEmpty()) {
                                    appOpenOfferShown = true;
                                    showAppOpenOfferDialog(imgUrl, actionLink);
                                }
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                    }
                });
    }

    private void showAppOpenOfferDialog(String imageUrl, String actionLink) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_app_open_offer);
        dialog.setCancelable(true);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0.7f);
        }

        ImageView imgOffer = dialog.findViewById(R.id.imgOffer);
        ImageView btnCloseOffer = dialog.findViewById(R.id.btnCloseOffer);

        Glide.with(this).load(imageUrl).into(imgOffer);

        btnCloseOffer.setOnClickListener(v -> dialog.dismiss());

        if (actionLink != null && !actionLink.trim().isEmpty()) {
            imgOffer.setOnClickListener(v -> {
                dialog.dismiss();
                try {
                    String link = actionLink.trim().toLowerCase();
                    if (link.equals("app://subscription")) {
                        startActivity(new Intent(HomeActivity.this, SubscriptionActivity.class));
                    } else if (link.equals("app://gallery")) {
                        Intent intent = new Intent(HomeActivity.this, TemplateGalleryActivity.class);
                        intent.putExtra("category", "Festival Cards");
                        startActivity(intent);
                    } else if (link.equals("app://contact")) {
                        startActivity(new Intent(HomeActivity.this, CreateRequestActivity.class));
                    } else if (link.equals("app://profile")) {
                        startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
                    } else {
                        Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(actionLink));
                        startActivity(intent);
                    }
                } catch (Exception e) {
                    Toast.makeText(HomeActivity.this, "Invalid Link", Toast.LENGTH_SHORT).show();
                }
            });
        }

        dialog.show();
    }

    private boolean pendingOnboarding = false;

    private void showLoginSuccessPopup(boolean triggerOnboarding) {
        this.pendingOnboarding = triggerOnboarding;
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_login_success);
        dialog.setCancelable(false);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // Heavy dim for premium floating feel (no card background)
            window.setDimAmount(0.85f);
        }

        // ===== Get all views =====
        View container = dialog.findViewById(R.id.dialogContainer);
        ImageView btnClose = dialog.findViewById(R.id.btnCloseDialog);
        View pulseRing = dialog.findViewById(R.id.pulseRing);
        View innerCircle = dialog.findViewById(R.id.innerCircle);
        ImageView checkmark = dialog.findViewById(R.id.imgCheckmark);
        TextView title = dialog.findViewById(R.id.txtSuccessTitle);
        View divider = dialog.findViewById(R.id.dividerLine);
        TextView desc = dialog.findViewById(R.id.txtSuccessDesc);
        ProgressBar progressBar = dialog.findViewById(R.id.progressAutoHide);

        dialog.show();

        // ===== STEP 1: Container slides up with fade =====
        if (container != null) {
            container.setAlpha(0f);
            container.setTranslationY(80f);
            container.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setInterpolator(new DecelerateInterpolator(1.5f))
                    .start();
        }

        // ===== STEP 2: Inner green circle scales in with bounce =====
        if (innerCircle != null) {
            innerCircle.setScaleX(0f);
            innerCircle.setScaleY(0f);
            innerCircle.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setStartDelay(300)
                    .setDuration(600)
                    .setInterpolator(new OvershootInterpolator(1.4f))
                    .start();
        }

        // ===== STEP 3: Pulse ring ripple outward =====
        if (pulseRing != null) {
            pulseRing.setScaleX(0.6f);
            pulseRing.setScaleY(0.6f);
            pulseRing.setAlpha(0f);
            pulseRing.animate()
                    .scaleX(1.3f)
                    .scaleY(1.3f)
                    .alpha(0.6f)
                    .setStartDelay(500)
                    .setDuration(700)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> {
                        // Fade out the pulse ring after expanding
                        pulseRing.animate()
                                .alpha(0f)
                                .scaleX(1.5f)
                                .scaleY(1.5f)
                                .setDuration(600)
                                .start();
                    })
                    .start();
        }

        // ===== STEP 4: Checkmark pops in with overshoot =====
        if (checkmark != null) {
            checkmark.setScaleX(0f);
            checkmark.setScaleY(0f);
            checkmark.setRotation(-45f);
            checkmark.setAlpha(0f);
            checkmark.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .rotation(0f)
                    .alpha(1f)
                    .setStartDelay(700)
                    .setDuration(500)
                    .setInterpolator(new OvershootInterpolator(2.0f))
                    .start();
        }

        // ===== STEP 5: Title slides in =====
        if (title != null) {
            title.setTranslationY(20f);
            title.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(1000)
                    .setDuration(400)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        // ===== STEP 6: Divider expands =====
        if (divider != null) {
            divider.setScaleX(0f);
            divider.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .setStartDelay(1200)
                    .setDuration(400)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        // ===== STEP 7: Description fades in =====
        if (desc != null) {
            desc.setTranslationY(15f);
            desc.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(1400)
                    .setDuration(400)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        // ===== STEP 8: Progress bar countdown (auto-dismiss timer) =====
        if (progressBar != null) {
            progressBar.setProgress(100);
            new Handler().postDelayed(() -> {
                progressBar.setAlpha(1f);
                ObjectAnimator progressAnim = ObjectAnimator.ofInt(progressBar, "progress", 100, 0);
                progressAnim.setDuration(4000);
                progressAnim.setInterpolator(new DecelerateInterpolator(0.5f));
                progressAnim.start();
            }, 1500);
        }

        // ===== AUTO-HIDE AFTER 5.5 SECONDS (animation time + countdown) =====
        new Handler().postDelayed(() -> {
            if (dialog.isShowing()) {
                dismissDialogWithAnim(dialog, container);
            }
        }, 5500);

        // ===== Close button =====
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismissDialogWithAnim(dialog, container));
        }
    }

    private void dismissDialogWithAnim(Dialog dialog, View container) {
        if (container != null) {
            container.animate()
                    .alpha(0f)
                    .scaleX(0.85f)
                    .scaleY(0.85f)
                    .translationY(50f)
                    .setDuration(350)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> {
                        try {
                            if (!isFinishing() && !isDestroyed() && dialog != null && dialog.isShowing()) {
                                dialog.dismiss();
                            }
                        } catch (Exception ignored) {
                        }
                        // ===== START SPOTLIGHT AFTER DIALOG CLOSES =====
                        if (pendingOnboarding && !isFinishing() && !isDestroyed()) {
                            pendingOnboarding = false;
                            new Handler().postDelayed(() -> {
                                if (!isFinishing() && !isDestroyed()) {
                                    startSpotlightOnboarding();
                                }
                            }, 500);
                        }
                    })
                    .start();
        } else {
            try {
                if (!isFinishing() && !isDestroyed() && dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
            } catch (Exception ignored) {
            }
            if (pendingOnboarding && !isFinishing() && !isDestroyed()) {
                pendingOnboarding = false;
                new Handler().postDelayed(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        startSpotlightOnboarding();
                    }
                }, 500);
            }
        }
    }

    // ===== SPOTLIGHT ONBOARDING WALKTHROUGH =====
    private void startSpotlightOnboarding() {
        SpotlightOverlay spotlight = new SpotlightOverlay(this);

        // 1. Profile/Menu Icon
        View btnMenu = findViewById(R.id.btnMenu);
        if (btnMenu != null) {
            spotlight.addTarget(btnMenu,
                    getString(R.string.onboard_spot_profile_title),
                    getString(R.string.onboard_spot_profile_desc));
        }

        // 2. Search Icon
        View btnSearch = findViewById(R.id.btnSearch);
        if (btnSearch != null) {
            spotlight.addTarget(btnSearch,
                    getString(R.string.onboard_spot_search_title),
                    getString(R.string.onboard_spot_search_desc));
        }

        // 3. Notification Icon
        View btnNotif = findViewById(R.id.btnNotification);
        if (btnNotif != null) {
            spotlight.addTarget(btnNotif,
                    getString(R.string.onboard_spot_notif_title),
                    getString(R.string.onboard_spot_notif_desc));
        }

        // 4. Bottom Navigation
        if (bottomNav != null) {
            spotlight.addTarget(bottomNav,
                    getString(R.string.onboard_spot_nav_title),
                    getString(R.string.onboard_spot_nav_desc));
        }

        spotlight.start(this);
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
                                adList.add(new AdvertisementItem(d.getKey(), url, link != null ? link : ""));
                        }

                        if (!adList.isEmpty()) {
                            // USE ADVERTISEMENT ADAPTER
                            ArrayList<AdvertisementItem> infiniteList = new ArrayList<>();
                            int size = adList.size();
                            for (int i = 0; i < 1000; i++)
                                infiniteList.add(adList.get(i % size));
                            trendingOriginalSize = size;
                            rvTrending.setAdapter(new AdvertisementAdapter(infiniteList));
                            
                            // 🚀 START FROM MIDDLE TO SHOW INFINITE CAROUSEL PROPERLY FROM LAUNCH
                            int startPos = (500 / size) * size;
                            rvTrending.scrollToPosition(startPos);
                            rvTrending.post(() -> {
                                View view = rvTrending.getLayoutManager().findViewByPosition(startPos);
                                if (view != null) {
                                    int[] snapDistance = trendingSnapHelper.calculateDistanceToFinalSnap(rvTrending.getLayoutManager(), view);
                                    if (snapDistance != null && (snapDistance[0] != 0 || snapDistance[1] != 0)) {
                                        rvTrending.scrollBy(snapDistance[0], snapDistance[1]);
                                    }
                                }
                            });

                            // ✨ HIGHLIGHT & SCROLL IF TARGET AD ID MATCHES
                            if (targetAdId != null) {
                                // Pause auto-scroller while highlighting
                                if (trendingHandler != null)
                                    trendingHandler.removeCallbacks(trendingRunnable);

                                int searchStart = (500 / size) * size;
                                for (int i = searchStart; i < infiniteList.size(); i++) {
                                    if (infiniteList.get(i).id != null && infiniteList.get(i).id.equals(targetAdId)) {
                                        final int finalPos = i;
                                        if (rvTrending.getAdapter() instanceof AdvertisementAdapter) {
                                            AdvertisementAdapter adp = (AdvertisementAdapter) rvTrending.getAdapter();
                                            adp.setHighlightPos(finalPos);

                                            mainScroll.smoothScrollTo(0, 0); // Ads are at the top
                                            rvTrending.postDelayed(() -> {
                                                rvTrending.scrollToPosition(finalPos);
                                                rvTrending.post(() -> {
                                                    View view = rvTrending.getLayoutManager().findViewByPosition(finalPos);
                                                    if (view != null) {
                                                        int[] snapDistance = trendingSnapHelper.calculateDistanceToFinalSnap(rvTrending.getLayoutManager(), view);
                                                        if (snapDistance != null && (snapDistance[0] != 0 || snapDistance[1] != 0)) {
                                                            rvTrending.scrollBy(snapDistance[0], snapDistance[1]);
                                                        }
                                                    }
                                                });

                                                new Handler().postDelayed(() -> {
                                                    adp.setHighlightPos(-1);
                                                    targetAdId = null;
                                                    // Resume auto-scroller
                                                    if (trendingHandler != null) {
                                                        trendingHandler.removeCallbacks(trendingRunnable);
                                                        trendingHandler.postDelayed(trendingRunnable, 3000);
                                                    }
                                                }, 4000);
                                            }, 400);
                                        }
                                        break;
                                    }
                                }
                            }
                        } else {
                            // FALLBACK TO TRENDING NOW
                            DataSnapshot trending = root.child("Trending Now");
                            ArrayList<TemplateModel> trendList = new ArrayList<>();
                            for (DataSnapshot d : trending.getChildren()) {
                                String url = d.hasChild("imagePath") ? d.child("imagePath").getValue(String.class)
                                        : d.child("url").getValue(String.class);
                                String type = d.child("type").getValue(String.class);
                                if (url != null)
                                    trendList.add(new TemplateModel(d.getKey(), url, "Trending Now", null, type));
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
                        // 🚀 START CAROUSEL FROM MIDDLE WITH PERFECT CENTERING
                        trendingPos = 500;
                        rvTrending.scrollToPosition(trendingPos);
                        rvTrending.post(() -> {
                            View view = rvTrending.getLayoutManager().findViewByPosition(trendingPos);
                            if (view != null) {
                                int[] snapDistance = trendingSnapHelper.calculateDistanceToFinalSnap(rvTrending.getLayoutManager(), view);
                                if (snapDistance != null && (snapDistance[0] != 0 || snapDistance[1] != 0)) {
                                    rvTrending.scrollBy(snapDistance[0], snapDistance[1]);
                                }
                            }
                        });
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



    void filterFestivalByDate(String date) {

        if ("CLEAR".equals(date)) {
            loadAllFestivalCardsLive();
            return;
        }

        btnAll.setVisibility(View.VISIBLE);

        // Fetching live data for the selected date
        FirebaseDatabase.getInstance().getReference("templates")
                .child("Festival Cards")
                .child(date)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        safelyHideSkeleton(skFestival);
                        
                        ArrayList<FestivalCardItem> list = new ArrayList<>();
                        for (DataSnapshot festivalSnap : snapshot.getChildren()) {
                            String festivalName = festivalSnap.getKey();
                            // Take FIRST template under this festival
                            DataSnapshot firstTemplate = null;
                            for (DataSnapshot t : festivalSnap.getChildren()) {
                                firstTemplate = t;
                                break;
                            }
                            if (firstTemplate == null) continue;
                            String url = firstTemplate.hasChild("imagePath")
                                    ? firstTemplate.child("imagePath").getValue(String.class)
                                    : firstTemplate.child("url").getValue(String.class);
                            if (url == null) continue;

                            String templateId = firstTemplate.getKey();

                            FestivalCardItem item = new FestivalCardItem(url, date, festivalName,
                                    System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000));
                            item.templateId = templateId;
                            list.add(item);
                        }

                        if (list.isEmpty()) {
                            rvFestivalCards.setAdapter(null);
                            return;
                        }

                        FestivalCardAdapter adapter = new FestivalCardAdapter(list, (item) -> {
                            String fullCat = "Festival Cards/" + date + "/" + item.festivalName;
                            Intent i = new Intent(HomeActivity.this, TemplatePreviewActivity.class);
                            i.putExtra("id", item.templateId);
                            i.putExtra("path", item.imagePath);
                            i.putExtra("category", fullCat);
                            startActivity(i);
                        });
                        rvFestivalCards.setAdapter(adapter);
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
                                    String type = itemSnapshot.child("type").getValue(String.class);
                                    if (url != null) {
                                        list.add(new TemplateModel(itemSnapshot.getKey(), url, key, null, type));
                                    }
                                }

                                if (!list.isEmpty()) {
                                    safelyHideSkeleton(skSection);
                                    TemplateGridAdapter adapterInstance = new TemplateGridAdapter(list, t -> {
                                        Intent i = new Intent(HomeActivity.this, TemplatePreviewActivity.class);
                                        i.putExtra("id", t.id);
                                        i.putExtra("path", t.url);
                                        i.putExtra("category", t.category);
                                        startActivity(i);
                                    });
                                    rvItems.setAdapter(adapterInstance);
                                    container.addView(sectionView);

                                    // ✨ HIGHLIGHT & SCROLL IF TARGET ID MATCHES
                                    if (key.equalsIgnoreCase("Latest Update") && targetId != null) {
                                        for (int i = 0; i < list.size(); i++) {
                                            if (list.get(i).id != null && list.get(i).id.equals(targetId)) {
                                                adapterInstance.setHighlightId(targetId);
                                                final int scrollPos = i;
                                                final View targetSection = sectionView;

                                                // 1. Scroll vertical page to the section
                                                mainScroll.postDelayed(() -> {
                                                    // Calculate absolute top position in ScrollView
                                                    int verticalPos = targetSection.getTop() + container.getTop();
                                                    mainScroll.smoothScrollTo(0, verticalPos);

                                                    // 2. Scroll horizontal list to the item
                                                    rvItems.postDelayed(() -> {
                                                        rvItems.smoothScrollToPosition(scrollPos);

                                                        // 3. Remove highlight after 4 seconds
                                                        new Handler().postDelayed(() -> {
                                                            adapterInstance.setHighlightId(null);
                                                            targetId = null; // Clear so it doesn't trigger again
                                                        }, 4000);
                                                    }, 600);
                                                }, 800);
                                                break;
                                            }
                                        }
                                    }
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
                            String type = itemSnapshot.child("type").getValue(String.class);
                            if (url != null) {
                                allList.add(new TemplateModel(itemSnapshot.getKey(), url,
                                        parentKey + "/" + subSnapshot.getKey(), null, type));
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
                        String type = d.child("type").getValue(String.class);
                        if (url != null) {
                            list.add(new TemplateModel(d.getKey(), url, parentKey + "/" + subKey, null, type));
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
                            String type = d.child("type").getValue(String.class);
                            if (url != null) {
                                list.add(new TemplateModel(d.getKey(), url, key, null, type));
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
        // Load only TODAY's date for initial/onResume load
        String today = new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());

        FirebaseDatabase.getInstance().getReference("templates")
                .child("Festival Cards")
                .child(today)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        safelyHideSkeleton(skFestival);
                        ArrayList<FestivalCardItem> list = new ArrayList<>();
                        for (DataSnapshot festivalSnap : snapshot.getChildren()) {
                            String festivalName = festivalSnap.getKey();
                            DataSnapshot firstTemplate = null;
                            for (DataSnapshot t : festivalSnap.getChildren()) {
                                firstTemplate = t;
                                break;
                            }
                            if (firstTemplate == null) continue;
                            String url = firstTemplate.hasChild("imagePath")
                                    ? firstTemplate.child("imagePath").getValue(String.class)
                                    : firstTemplate.child("url").getValue(String.class);
                            if (url == null) continue;

                            String templateId = firstTemplate.getKey();
                            FestivalCardItem item = new FestivalCardItem(url, today, festivalName,
                                    System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000));
                            item.templateId = templateId;
                            list.add(item);
                        }

                        if (list.isEmpty()) {
                            // If no festivals today, fall back to loading all dates
                            loadAllFestivalCardsLive();
                            return;
                        }

                        FestivalCardAdapter adapter = new FestivalCardAdapter(list, (item) -> {
                            String fullCat = "Festival Cards/" + today + "/" + item.festivalName;
                            Intent i = new Intent(HomeActivity.this, TemplatePreviewActivity.class);
                            i.putExtra("id", item.templateId);
                            i.putExtra("path", item.imagePath);
                            i.putExtra("category", fullCat);
                            startActivity(i);
                        });
                        rvFestivalCards.setAdapter(adapter);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        safelyHideSkeleton(skFestival);
                    }
                });
    }

    /**
     * Loads festival cards from ALL dates in the next 7 days.
     * Used when "All" date chip is selected — shows one card per festival per date.
     */
    void loadAllFestivalCardsLive() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
        ArrayList<String> next7Dates = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 7; i++) {
            Calendar c = (Calendar) cal.clone();
            c.add(Calendar.DAY_OF_MONTH, i);
            next7Dates.add(sdf.format(c.getTime()));
        }

        // We'll collect results from all dates; use an atomic counter to know when all are done
        ArrayList<FestivalCardItem> combined = new ArrayList<>();
        int[] remaining = {next7Dates.size()};

        DatabaseReference festivalRef = FirebaseDatabase.getInstance()
                .getReference("templates").child("Festival Cards");

        for (String date : next7Dates) {
            festivalRef.child(date).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    for (DataSnapshot festivalSnap : snapshot.getChildren()) {
                        String festivalName = festivalSnap.getKey();
                        DataSnapshot firstTemplate = null;
                        for (DataSnapshot t : festivalSnap.getChildren()) {
                            firstTemplate = t;
                            break;
                        }
                        if (firstTemplate == null) continue;
                        String url = firstTemplate.hasChild("imagePath")
                                ? firstTemplate.child("imagePath").getValue(String.class)
                                : firstTemplate.child("url").getValue(String.class);
                        if (url == null) continue;

                        String templateId = firstTemplate.getKey();
                        FestivalCardItem item = new FestivalCardItem(url, date, festivalName,
                                System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000));
                        item.templateId = templateId;
                        synchronized (combined) {
                            combined.add(item);
                        }
                    }

                    remaining[0]--;
                    if (remaining[0] == 0) {
                        // All dates loaded — update UI on main thread
                        runOnUiThread(() -> {
                            safelyHideSkeleton(skFestival);
                            if (combined.isEmpty()) {
                                rvFestivalCards.setAdapter(null);
                                return;
                            }
                            FestivalCardAdapter adapter = new FestivalCardAdapter(combined, (item) -> {
                                String fullCat = "Festival Cards/" + item.date + "/" + item.festivalName;
                                Intent i = new Intent(HomeActivity.this, TemplatePreviewActivity.class);
                                i.putExtra("id", item.templateId);
                                i.putExtra("path", item.imagePath);
                                i.putExtra("category", fullCat);
                                startActivity(i);
                            });
                            rvFestivalCards.setAdapter(adapter);
                        });
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    remaining[0]--;
                    if (remaining[0] == 0) {
                        runOnUiThread(() -> safelyHideSkeleton(skFestival));
                    }
                }
            });
        }
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

        // Reset date chip to "All" (position 0) so UI matches the data being loaded
        if (festivalDateAdapter != null) {
            festivalDateAdapter.selectedPosition = 0;
            festivalDateAdapter.notifyDataSetChanged();
        }
        loadAllFestivalCardsLive(); // "All" is selected by default — load all upcoming festivals

        loadHeroSectionLive(); // Merged loadAdvertisementLive and loadTrendingLive

        if (trendingHandler != null) {
            trendingHandler.removeCallbacks(trendingRunnable);
            trendingHandler.postDelayed(trendingRunnable, 3000);
        }
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
