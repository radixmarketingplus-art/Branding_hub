package com.example.rmplus;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import androidx.drawerlayout.widget.DrawerLayout;
import android.view.Gravity;


public class DashboardActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        headerBadge = findViewById(R.id.headerBadge);
        ImageView btnMenu = findViewById(R.id.btnMenu);

        String role = getIntent().getStringExtra("role");
        setupBase(role, R.id.home);

        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                DrawerLayout drawer = findViewById(R.id.drawerLayout);
                if (drawer != null) {
                    drawer.openDrawer(Gravity.START);
                }
            });
        }

        String uid = FirebaseAuth.getInstance().getUid();

    }
}
