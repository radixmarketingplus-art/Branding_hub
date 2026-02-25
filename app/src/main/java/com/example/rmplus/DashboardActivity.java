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


public class DashboardActivity extends AppCompatActivity {

    BottomNavigationView bottomNav;
    TextView badgeTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        bottomNav = findViewById(R.id.bottomNav);
        badgeTextView = findViewById(R.id.badgeText);
        ImageView profileBtn = findViewById(R.id.profileBtn);

        profileBtn.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseDatabase.getInstance().getReference("users").child(uid).child("profileImage")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            String url = snapshot.getValue(String.class);
                            if (url != null && !url.isEmpty()) {
                                Glide.with(DashboardActivity.this)
                                        .load(url)
                                        .placeholder(R.drawable.ic_profile)
                                        .error(R.drawable.ic_profile)
                                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                                        .into(profileBtn);
                            }
                        }
                        @Override public void onCancelled(DatabaseError error) {}
                    });
        }

        // -----------------------------
        // LOAD MENU BASED ON ROLE
        // -----------------------------
        String role = getIntent().getStringExtra("role");

        bottomNav.getMenu().clear();   // IMPORTANT

        if(role != null && role.equals("admin")){
            bottomNav.inflateMenu(R.menu.bottom_menu_admin);
        }else{
            bottomNav.inflateMenu(R.menu.bottom_menu_user);
        }

        // -----------------------------
        // NAVIGATION CLICK HANDLER
        // -----------------------------
        bottomNav.setOnItemSelectedListener(item -> {

            if(item.getItemId()==R.id.home){
                return true;
            }



            if(item.getItemId()==R.id.contact){
                startActivity(new Intent(
                        DashboardActivity.this,
                        ContactActivity.class));
                return true;
            }



            if(item.getItemId()==R.id.admin){
                startActivity(new Intent(
                        DashboardActivity.this,
                        AdminPanelActivity.class));
                return true;
            }

            return false;
        });

        // -----------------------------
        // BADGE COUNT
        // -----------------------------
        DatabaseReference badgeRef =
                FirebaseDatabase.getInstance()
                        .getReference("notifications")
                        .child(FirebaseAuth.getInstance().getUid());

        badgeRef.addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        int count = 0;

                        for (DataSnapshot d : snapshot.getChildren()) {

                            Boolean read =
                                    d.child("read")
                                            .getValue(Boolean.class);

                            if (read != null && !read)
                                count++;
                        }

                        if(count==0){
                            badgeTextView.setVisibility(View.GONE);
                        }else{
                            badgeTextView.setVisibility(View.VISIBLE);
                            badgeTextView.setText(
                                    String.valueOf(count));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) { }
                });

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    != getPackageManager().PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                android.Manifest.permission.POST_NOTIFICATIONS
                        }, 101);
            }
        }

    }
}
