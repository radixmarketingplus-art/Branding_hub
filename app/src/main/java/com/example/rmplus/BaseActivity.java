package com.example.rmplus;

import android.content.Intent;
import android.os.Build;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class BaseActivity extends AppCompatActivity {

    protected BottomNavigationView bottomNav;
    TextView headerBadge;

    protected void setupBase(String role, int selectedItemId) {

        // ================= HEADER =================
        TextView txtGreeting = findViewById(R.id.txtGreeting);
        ImageView btnSearch = findViewById(R.id.btnSearch);
        ImageView btnNotification = findViewById(R.id.btnNotification);
        ImageView btnProfile = findViewById(R.id.btnProfile);
        headerBadge = findViewById(R.id.headerBadge);

        // USER NAME
        String uid = FirebaseAuth.getInstance().getUid();

        // 👉 Only execute if header exists in layout
        if (txtGreeting != null) {
            if (uid != null) {
                FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(uid)
                        .child("name")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                String name = snapshot.getValue(String.class);
                                txtGreeting.setText(
                                        name != null ? "Hi, " + name : "Hi, User"
                                );
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                                txtGreeting.setText("Hi, User");
                            }
                        });
            }

            // HEADER BUTTONS
            btnSearch.setOnClickListener(v ->
                    startActivity(new Intent(this, SearchActivity.class)));

            btnNotification.setOnClickListener(v ->
                    startActivity(new Intent(this, NotificationActivity.class)));

            btnProfile.setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileActivity.class)));

            // ================= HEADER BADGE =================
            if (uid != null) {

                DatabaseReference badgeRef =
                        FirebaseDatabase.getInstance()
                                .getReference("notifications")
                                .child(uid);

                badgeRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        int count = 0;

                        for (DataSnapshot d : snapshot.getChildren()) {
                            Boolean read = d.child("read").getValue(Boolean.class);
                            if (read != null && !read) count++;
                        }

                        if (count == 0) {
                            headerBadge.setVisibility(View.GONE);
                        } else {
                            headerBadge.setVisibility(View.VISIBLE);
                            headerBadge.setText(String.valueOf(count));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                    }
                });
            }
        }

        // ================= BOTTOM NAV =================
        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.getMenu().clear();

        if (role != null && role.equals("admin")) {
            bottomNav.inflateMenu(R.menu.bottom_menu_admin);
        } else {
            bottomNav.inflateMenu(R.menu.bottom_menu_user);
        }

        bottomNav.setSelectedItemId(selectedItemId);

        bottomNav.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == selectedItemId) return true; // already on same screen

            if (id == R.id.home) {
                startActivity(new Intent(this, HomeActivity.class));
            }

            if (id == R.id.contact) {
                startActivity(new Intent(this, ContactActivity.class));
            }

            if (id == R.id.admin) {
                startActivity(new Intent(this, AdminPanelActivity.class));
            }

            if (id == R.id.upload) {
                startActivity(new Intent(this, UploadTemplatesActivity.class));
            }

            if (id == R.id.template) {
                startActivity(new Intent(this, TemplateGalleryActivity.class));
            }

            if (id == R.id.myDesign) {
                startActivity(new Intent(this, MyDesignActivity.class));
            }

            return true;
        });
    }
}
