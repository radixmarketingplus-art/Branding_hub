package com.example.rmplus;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;   // ✅ ADDED
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    ImageView logoImg;
    TextView appName, tagline;

    DatabaseReference db;

    // ✅ ADDED
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);   // 🔥 ADD THIS
        setContentView(R.layout.activity_main);

        logoImg = findViewById(R.id.logoImg);
//        appName = findViewById(R.id.appName);
        tagline = findViewById(R.id.tagline);

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
        Animation logoAnim =
                AnimationUtils.loadAnimation(this, R.anim.logo_intro);

        Animation textAnim =
                AnimationUtils.loadAnimation(this, R.anim.text_slide_fade);

        logoImg.startAnimation(logoAnim);
        tagline.postDelayed(() -> tagline.startAnimation(textAnim), 900);

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
