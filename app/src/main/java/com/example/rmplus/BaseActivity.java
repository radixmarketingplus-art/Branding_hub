package com.example.rmplus;

import android.content.Intent;
import android.os.Build;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class BaseActivity extends AppCompatActivity {

    protected BottomNavigationView bottomNav;
    View headerBadge;

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
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                // 1. Set Greeting Name
                                String name = snapshot.child("name").getValue(String.class);
                                String displayName = (name != null && !name.isEmpty()) ? name
                                        : getString(R.string.default_user);
                                txtGreeting.setText(getString(R.string.greeting_format, displayName));

                                // 2. Set Profile Image
                                String profileUrl = snapshot.child("profileImage").getValue(String.class);
                                if (profileUrl != null && !profileUrl.isEmpty()) {
                                    btnProfile.setImageTintList(null);
                                    Glide.with(BaseActivity.this)
                                            .load(profileUrl)
                                            .placeholder(R.drawable.ic_profile)
                                            .error(R.drawable.ic_profile)
                                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                                            .into(btnProfile);
                                } else {
                                    btnProfile.setImageResource(R.drawable.ic_profile);
                                    btnProfile.setImageTintList(android.content.res.ColorStateList
                                            .valueOf(getResources().getColor(R.color.primary)));
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                                txtGreeting
                                        .setText(getString(R.string.greeting_format, getString(R.string.default_user)));
                            }
                        });
            }

            // HEADER BUTTONS
            if (btnSearch != null)
                btnSearch.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
            if (btnNotification != null)
                btnNotification.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
            if (btnProfile != null)
                btnProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

            // ================= HEADER BADGE =================
            if (uid != null && headerBadge != null) {
                DatabaseReference badgeRef = FirebaseDatabase.getInstance()
                        .getReference("notifications")
                        .child(uid);

                badgeRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        int count = 0;
                        long now = System.currentTimeMillis();
                        for (DataSnapshot d : snapshot.getChildren()) {
                            Boolean read = d.child("read").getValue(Boolean.class);
                            Long expiry = d.child("expiryDate").getValue(Long.class);
                            
                            // Only count if not read AND not expired
                            if (read != null && !read) {
                                if (expiry == null || expiry == 0 || expiry > now) {
                                    count++;
                                }
                            }
                        }
                        if (count == 0) {
                            headerBadge.setVisibility(View.GONE);
                        } else {
                            headerBadge.setVisibility(View.VISIBLE);
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
        if (bottomNav != null) {
            bottomNav.getMenu().clear();
            if (role != null && role.equals("admin")) {
                bottomNav.inflateMenu(R.menu.bottom_menu_admin);
            } else {
                bottomNav.inflateMenu(R.menu.bottom_menu_user);
            }

            bottomNav.setSelectedItemId(selectedItemId);

            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == selectedItemId)
                    return true;

                if (id == R.id.home) {
                    startActivity(new Intent(this, HomeActivity.class));
                    overridePendingTransition(R.anim.nav_fade_in, R.anim.nav_fade_out);
                } else if (id == R.id.contact) {
                    startActivity(new Intent(this, ContactActivity.class));
                    overridePendingTransition(R.anim.nav_fade_in, R.anim.nav_fade_out);
                } else if (id == R.id.admin) {
                    startActivity(new Intent(this, AdminPanelActivity.class));
                    overridePendingTransition(R.anim.nav_fade_in, R.anim.nav_fade_out);
                } else if (id == R.id.upload) {
                    startActivity(new Intent(this, UploadTemplatesActivity.class));
                    overridePendingTransition(R.anim.nav_fade_in, R.anim.nav_fade_out);
                } else if (id == R.id.template) {
                    startActivity(new Intent(this, TemplateGalleryActivity.class));
                    overridePendingTransition(R.anim.nav_fade_in, R.anim.nav_fade_out);
                } else if (id == R.id.myDesign) {
                    startActivity(new Intent(this, MyDesignActivity.class));
                    overridePendingTransition(R.anim.nav_fade_in, R.anim.nav_fade_out);
                }
                return true;
            });
        }
    }

    public String getLocalizedSectionName(String key) {
        if (key == null)
            return "";
        if (key.equalsIgnoreCase("Select Section"))
            return getString(R.string.section_select);
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

    public String getLocalizedRequestType(String type) {
        if (type == null)
            return "";
        if (type.equalsIgnoreCase("Issue"))
            return getString(R.string.cat_issue);
        if (type.equalsIgnoreCase("Order"))
            return getString(R.string.cat_order);
        if (type.equalsIgnoreCase("Custom Template"))
            return getString(R.string.cat_custom_template);
        if (type.equalsIgnoreCase("Other"))
            return getString(R.string.cat_other);
        return type;
    }

    public String getLocalizedStatus(String status) {
        if (status == null)
            return "";
        if (status.equalsIgnoreCase("pending"))
            return getString(R.string.tab_pending);
        if (status.equalsIgnoreCase("accepted"))
            return getString(R.string.tab_accepted);
        if (status.equalsIgnoreCase("rejected"))
            return getString(R.string.tab_rejected);
        return status;
    }
}
