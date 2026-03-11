package com.example.rmplus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;

public class OnboardingActivity extends BaseActivity {

    ViewPager2 viewPager;
    LinearLayout dotContainer;
    TextView btnSkip, btnNext;
    ArrayList<OnboardSlide> slides;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.viewPager);
        dotContainer = findViewById(R.id.dotContainer);
        btnSkip = findViewById(R.id.btnSkip);
        btnNext = findViewById(R.id.btnNext);

        // ===== BUILD SLIDES BASED ON ROLE =====
        String role = getIntent().getStringExtra("role");
        slides = new ArrayList<>();

        // Common slides for all users
        slides.add(new OnboardSlide(
                R.drawable.ic_onboard_explore,
                R.string.onboard_title_1,
                R.string.onboard_desc_1));
        slides.add(new OnboardSlide(
                R.drawable.ic_onboard_download,
                R.string.onboard_title_2,
                R.string.onboard_desc_2));
        slides.add(new OnboardSlide(
                R.drawable.ic_onboard_request,
                R.string.onboard_title_3,
                R.string.onboard_desc_3));
        slides.add(new OnboardSlide(
                R.drawable.ic_onboard_designs,
                R.string.onboard_title_4,
                R.string.onboard_desc_4));

        // Admin-only extra slide
        if (role != null && role.equals("admin")) {
            slides.add(new OnboardSlide(
                    R.drawable.ic_onboard_admin,
                    R.string.onboard_title_admin,
                    R.string.onboard_desc_admin));
        }

        // ===== SETUP VIEWPAGER =====
        viewPager.setAdapter(new OnboardingAdapter());
        setupDots(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                setupDots(position);
                // Change button text on last page
                if (position == slides.size() - 1) {
                    btnNext.setText(R.string.onboard_get_started);
                    btnSkip.setVisibility(View.INVISIBLE);
                } else {
                    btnNext.setText(R.string.onboard_next);
                    btnSkip.setVisibility(View.VISIBLE);
                }
            }
        });

        // ===== NEXT BUTTON =====
        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < slides.size() - 1) {
                viewPager.setCurrentItem(current + 1);
            } else {
                finishOnboarding();
            }
        });

        // ===== SKIP BUTTON =====
        btnSkip.setOnClickListener(v -> finishOnboarding());

        // ===== Entry Animation for buttons =====
        btnNext.setAlpha(0f);
        btnNext.setTranslationY(40f);
        btnNext.animate().alpha(1f).translationY(0f).setDuration(600).setStartDelay(300).start();

        btnSkip.setAlpha(0f);
        btnSkip.animate().alpha(1f).setDuration(400).setStartDelay(200).start();
    }

    // ===== FINISH ONBOARDING & GO TO HOME =====
    private void finishOnboarding() {
        // Mark onboarding done for this user
        SharedPreferences sp = getSharedPreferences("APP_DATA", MODE_PRIVATE);
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            sp.edit().putBoolean("onboarding_done_" + uid, true).apply();
        }

        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra("show_login_success", true);
        startActivity(intent);
        finish();
    }

    // ===== DOT INDICATORS =====
    private void setupDots(int currentPosition) {
        dotContainer.removeAllViews();
        for (int i = 0; i < slides.size(); i++) {
            View dot = new View(this);
            int size;
            int bg;
            if (i == currentPosition) {
                size = dpToPx(10);
                bg = R.drawable.dot_active;
            } else {
                size = dpToPx(8);
                bg = R.drawable.dot_inactive;
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(dpToPx(5), 0, dpToPx(5), 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(bg);

            // Animate the active dot
            if (i == currentPosition) {
                dot.setScaleX(0f);
                dot.setScaleY(0f);
                dot.animate().scaleX(1f).scaleY(1f)
                        .setDuration(300)
                        .setInterpolator(new OvershootInterpolator())
                        .start();
            }

            dotContainer.addView(dot);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // ===== SLIDE DATA CLASS =====
    static class OnboardSlide {
        int iconRes, titleRes, descRes;

        OnboardSlide(int iconRes, int titleRes, int descRes) {
            this.iconRes = iconRes;
            this.titleRes = titleRes;
            this.descRes = descRes;
        }
    }

    // ===== VIEWPAGER ADAPTER =====
    class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.SlideHolder> {

        @NonNull
        @Override
        public SlideHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_onboarding_slide, parent, false);
            return new SlideHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SlideHolder holder, int position) {
            OnboardSlide slide = slides.get(position);
            holder.imgIcon.setImageResource(slide.iconRes);
            holder.txtTitle.setText(slide.titleRes);
            holder.txtDesc.setText(slide.descRes);

            // ===== Animate each slide content =====
            // Icon bg bounce
            holder.iconBg.setScaleX(0f);
            holder.iconBg.setScaleY(0f);
            holder.iconBg.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(500)
                    .setStartDelay(100)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();

            // Icon pop
            holder.imgIcon.setAlpha(0f);
            holder.imgIcon.setScaleX(0f);
            holder.imgIcon.setScaleY(0f);
            holder.imgIcon.animate()
                    .alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(400)
                    .setStartDelay(300)
                    .setInterpolator(new OvershootInterpolator(1.5f))
                    .start();

            // Title slide up
            holder.txtTitle.setAlpha(0f);
            holder.txtTitle.setTranslationY(30f);
            holder.txtTitle.animate()
                    .alpha(1f).translationY(0f)
                    .setDuration(400)
                    .setStartDelay(500)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();

            // Description fade in
            holder.txtDesc.setAlpha(0f);
            holder.txtDesc.setTranslationY(20f);
            holder.txtDesc.animate()
                    .alpha(1f).translationY(0f)
                    .setDuration(400)
                    .setStartDelay(650)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        @Override
        public int getItemCount() {
            return slides.size();
        }

        class SlideHolder extends RecyclerView.ViewHolder {
            ImageView imgIcon;
            TextView txtTitle, txtDesc;
            View iconBg;

            SlideHolder(@NonNull View itemView) {
                super(itemView);
                imgIcon = itemView.findViewById(R.id.imgSlideIcon);
                txtTitle = itemView.findViewById(R.id.txtSlideTitle);
                txtDesc = itemView.findViewById(R.id.txtSlideDesc);
                iconBg = itemView.findViewById(R.id.iconBg);
            }
        }
    }
}
