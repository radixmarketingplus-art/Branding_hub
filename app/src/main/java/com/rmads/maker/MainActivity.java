package com.rmads.maker;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;


import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.MobileAds;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;   // ✅ ADDED
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends BaseActivity {

    ImageView logoImg;
    // TextView appName;

    DatabaseReference db;

    // ✅ ADDED
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 🔥 Initialize Google Ads (Fixes missing QPL/Tigon background logs)
        new Thread(() -> {
            MobileAds.initialize(this, initializationStatus -> {});
        }).start();

        setContentView(R.layout.activity_main);

        logoImg = findViewById(R.id.logoImg);
        android.widget.ProgressBar progressBar = findViewById(R.id.progressBar);
//        appName = findViewById(R.id.appName);


        // ✅ Initialize Auth
        auth = FirebaseAuth.getInstance();

        // Firebase Test
//        db = FirebaseDatabase.getInstance().getReference("Test");
//        db.setValue("Hello Firebase");

        // Load Animations
//        Animation zoomAnim = AnimationUtils.loadAnimation(this, R.anim.zoom_in);
//        Animation fadeAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in);
//
//        logoImg.startAnimation(zoomAnim);
//        appName.startAnimation(fadeAnim);
//        tagline.startAnimation(fadeAnim);
        // Animations
        Animation logoAnim = AnimationUtils.loadAnimation(this, R.anim.logo_intro);
        Animation fadeAnim = android.view.animation.AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        
        // Staggered start times
        fadeAnim.setDuration(1000);
        fadeAnim.setStartOffset(600); // Progress bar fades in last

        logoImg.startAnimation(logoAnim);
        
        progressBar.setVisibility(android.view.View.VISIBLE);
        progressBar.startAnimation(fadeAnim);



        // ❌ OLD CODE — always opened Login
        /*
        new Handler().postDelayed(() -> {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }, 2500);
        */

        // ===================================================
        // ✅ NEW CODE — AUTO LOGIN CHECK
        // ===================================================

        new Handler().postDelayed(() -> {

            if (auth.getCurrentUser() != null) {
                // ✅ User already logged in
                startActivity(new Intent(MainActivity.this, HomeActivity.class));
            } else {
                // ❌ User not logged in
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            }

            finish();

        }, 2500);

    }
}
