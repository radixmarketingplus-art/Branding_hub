package com.example.rmplus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.materialswitch.MaterialSwitch;
import androidx.drawerlayout.widget.DrawerLayout;
import android.view.ViewGroup;
import android.view.Gravity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import android.net.Uri;
import android.widget.Toast;
import android.os.Bundle;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Security: Prevent screenshots and screen recording across the app
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        
        // 🔥 Real-time Account Existence Check (for Global Logout on Deletion)
        startAccountExistenceObserver();
    }

    private void startAccountExistenceObserver() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        // Skip observer for login/register screens to avoid redundant logic
        if (this instanceof LoginActivity || this instanceof RegisterActivity || this instanceof MainActivity) {
            return;
        }

        accountExistenceRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        accountExistenceListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // If user node is removed from DB, it means account is deleted or disabled
                if (!snapshot.exists() && !isFinishing()) {
                    handleGlobalLogout();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // If permission is lost, treat it as account disabled/deleted
                if (error.getCode() == DatabaseError.PERMISSION_DENIED) {
                    handleGlobalLogout();
                }
            }
        };
        accountExistenceRef.addValueEventListener(accountExistenceListener);
    }

    private void handleGlobalLogout() {
        // Clear local session and redirect
        FirebaseAuth.getInstance().signOut();
        getSharedPreferences("APP_DATA", MODE_PRIVATE).edit()
                .remove("isLoggedIn")
                .remove("role")
                .remove("user_name")
                .apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishAffinity();
    }

    protected BottomNavigationView bottomNav;
    private int currentNavItemId = -1;
    TextView headerBadge;

    private ValueEventListener accountExistenceListener;
    private DatabaseReference accountExistenceRef;

    protected void setupBase(String role, int selectedItemId) {

        // ================= HEADER =================
        TextView txtGreeting = findViewById(R.id.txtGreeting);
        ImageView btnSearch = findViewById(R.id.btnSearch);
        ImageView btnNotification = findViewById(R.id.btnNotification);
        ImageView btnMenu = findViewById(R.id.btnMenu);
        headerBadge = findViewById(R.id.headerBadge);

        // Initialize Drawer if layout supports it
        setupDrawer();

        // USER NAME
        String uid = FirebaseAuth.getInstance().getUid();

        // 👉 Only execute if header exists in layout
        if (txtGreeting != null) {

            // SEARCH BAR LOGIC
            View searchContainer = findViewById(R.id.searchContainer);
            EditText searchBox = findViewById(R.id.searchBox);
//            ImageView btnMic = findViewById(R.id.btnMic);

            if (searchBox != null) {
                searchBox.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
                searchBox.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) {
                        startActivity(new Intent(this, SearchActivity.class));
                        v.clearFocus();
                    }
                });
            }

/*            if (btnMic != null) {
                btnMic.setOnClickListener(v -> {
                    Toast.makeText(this, getString(R.string.msg_voice_search_soon), Toast.LENGTH_SHORT).show();
                    // Implement SpeechRecognizer here if needed
                });
            }*/

            if (uid != null) {
                // 🚀 LAG REDUCTION: Use cached name first
                SharedPreferences sp = getSharedPreferences("APP_DATA", MODE_PRIVATE);
                String cachedName = sp.getString("user_name", "");
                if (!cachedName.isEmpty()) {
                    txtGreeting.setText(cachedName);
                }

                FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(uid)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                String name = snapshot.child("name").getValue(String.class);
                                String displayName = (name != null && !name.isEmpty()) ? name
                                        : getString(R.string.default_user);
                                
                                if (!displayName.equals(cachedName)) {
                                    txtGreeting.setText(displayName);
                                    sp.edit().putString("user_name", displayName).apply();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {}
                        });
            }

            // HEADER BUTTONS
            if (btnSearch != null)
                btnSearch.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
            if (btnNotification != null)
                btnNotification.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
            if (btnMenu != null)
                btnMenu.setOnClickListener(v -> {
                    DrawerLayout drawer = findViewById(R.id.drawerLayout);
                    if (drawer != null) {
                        drawer.openDrawer(Gravity.START);
                    } else {
                        showMenuSheet();
                    }
                });

            // ================= HEADER BADGE =================
            if (uid != null && headerBadge != null) {
                refreshNotificationBadge(uid);
                
                // ✅ Ensure user is subscribed for background broadcast push
                com.google.firebase.messaging.FirebaseMessaging.getInstance()
                        .subscribeToTopic("all_users");
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

            currentNavItemId = selectedItemId;
            bottomNav.setSelectedItemId(selectedItemId);

            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == selectedItemId)
                    return true;

                if (id == R.id.home) {
                    startActivity(new Intent(this, HomeActivity.class));
                    applyTransition();
                } else if (id == R.id.contact) {
                    startActivity(new Intent(this, ContactActivity.class));
                    applyTransition();
                } else if (id == R.id.admin) {
                    startActivity(new Intent(this, AdminPanelActivity.class));
                    applyTransition();
                } else if (id == R.id.upload) {
                    startActivity(new Intent(this, UploadTemplatesActivity.class));
                    applyTransition();
                } else if (id == R.id.template) {
                    startActivity(new Intent(this, TemplateGalleryActivity.class));
                    applyTransition();
                } else if (id == R.id.ad_request) {
                    startActivity(new Intent(this, AdRequestActivity.class));
                    applyTransition();
                } else if (id == R.id.myDesign) {
                    startActivity(new Intent(this, MyDesignActivity.class));
                    applyTransition();
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

    private void setupDrawer() {
        DrawerLayout drawer = findViewById(R.id.drawerLayout);
        if (drawer == null)
            return;

        ShapeableImageView profileImg = findViewById(R.id.drawerProfileImage);
        TextView nameTxt = findViewById(R.id.drawerName);
        TextView emailTxt = findViewById(R.id.drawerEmail);
        TextView mobileTxt = findViewById(R.id.drawerMobile);
        MaterialSwitch darkSwitch = findViewById(R.id.drawerDarkSwitch);

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseDatabase.getInstance().getReference("users").child(uid)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot s) {
                            if (nameTxt != null)
                                nameTxt.setText(s.child("name").getValue(String.class));
                            if (emailTxt != null)
                                emailTxt.setText(s.child("email").getValue(String.class));
                            if (mobileTxt != null)
                                mobileTxt.setText(s.child("mobile").getValue(String.class));
                            String url = s.child("profileImage").getValue(String.class);
                            if (url != null && !url.isEmpty() && profileImg != null) {
                                Glide.with(BaseActivity.this)
                                        .load(url)
                                        .placeholder(R.drawable.ic_profile)
                                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                                        .into(profileImg);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError e) {
                        }
                    });
        }

        if (darkSwitch != null) {
            SharedPreferences sp = getSharedPreferences("APP_DATA", MODE_PRIVATE);
            int currentMode = sp.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_NO);
            darkSwitch.setChecked(currentMode == AppCompatDelegate.MODE_NIGHT_YES);
            darkSwitch.setOnCheckedChangeListener((v, isChecked) -> {
                int newMode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
                sp.edit().putInt("night_mode", newMode).apply();
                AppCompatDelegate.setDefaultNightMode(newMode);
                drawer.closeDrawer(Gravity.START);
            });
        }

        // Drawer Item Listeners
        View edit = findViewById(R.id.drawerEdit);
        if (edit != null)
            edit.setOnClickListener(v -> {
                drawer.closeDrawer(Gravity.START);
                startActivity(new Intent(this, EditProfileActivity.class));
            });
        View sub = findViewById(R.id.drawerSubscription);
        if (sub != null)
            sub.setOnClickListener(v -> {
                drawer.closeDrawer(Gravity.START);
                startActivity(new Intent(this, SubscriptionActivity.class));
            });
        View lang = findViewById(R.id.drawerLanguage);
        if (lang != null)
            lang.setOnClickListener(v -> {
                drawer.closeDrawer(Gravity.START);
                showLanguageDialog();
            });
        View settings = findViewById(R.id.drawerSettings);
        if (settings != null)
            settings.setOnClickListener(v -> {
                drawer.closeDrawer(Gravity.START);
                startActivity(new Intent(this, SettingsActivity.class));
            });
        View rate = findViewById(R.id.drawerRate);
        if (rate != null)
            rate.setOnClickListener(v -> {
                drawer.closeDrawer(Gravity.START);
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
                } catch (Exception e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
                }
            });

        View share = findViewById(R.id.drawerShare);
        if (share != null)
            share.setOnClickListener(v -> {
                drawer.closeDrawer(Gravity.START);
                shareApp();
            });
        View logout = findViewById(R.id.drawerLogout);
        if (logout != null)
            logout.setOnClickListener(v -> {
                drawer.closeDrawer(Gravity.START);
                FirebaseAuth.getInstance().signOut();
                getSharedPreferences("APP_DATA", MODE_PRIVATE).edit()
                        .remove("isLoggedIn")
                        .remove("role")
                        .apply();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finishAffinity();
            });
        // Drawer Item Listeners removed: drawerDelete handled in Settings
    }

    private void showMenuSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(
                this);
        View view = getLayoutInflater().inflate(R.layout.sheet_menu, null);
        sheet.setContentView(view);

        ShapeableImageView profileImg = view.findViewById(R.id.menuProfileImage);
        TextView nameTxt = view.findViewById(R.id.menuName);
        TextView emailTxt = view.findViewById(R.id.menuEmail);
        MaterialSwitch darkSwitch = view.findViewById(R.id.menuDarkSwitch);

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseDatabase.getInstance().getReference("users").child(uid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot s) {
                            nameTxt.setText(s.child("name").getValue(String.class));
                            emailTxt.setText(s.child("email").getValue(String.class));
                            String url = s.child("profileImage").getValue(String.class);
                            if (url != null && !url.isEmpty()) {
                                Glide.with(BaseActivity.this)
                                        .load(url)
                                        .placeholder(R.drawable.ic_profile)
                                        .into(profileImg);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError e) {
                        }
                    });
        }

        SharedPreferences sp = getSharedPreferences("APP_DATA", MODE_PRIVATE);
        int currentMode = sp.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_NO);
        darkSwitch.setChecked(currentMode == AppCompatDelegate.MODE_NIGHT_YES);
        darkSwitch.setOnCheckedChangeListener((v, isChecked) -> {
            int newMode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            sp.edit().putInt("night_mode", newMode).apply();
            AppCompatDelegate.setDefaultNightMode(newMode);
            sheet.dismiss();
        });

        view.findViewById(R.id.menuEdit).setOnClickListener(v -> {
            sheet.dismiss();
            startActivity(new Intent(this, EditProfileActivity.class));
        });
        view.findViewById(R.id.menuSubscription).setOnClickListener(v -> {
            sheet.dismiss();
            startActivity(new Intent(this, SubscriptionActivity.class));
        });
        view.findViewById(R.id.menuLanguage).setOnClickListener(v -> {
            sheet.dismiss();
            showLanguageDialog();
        });
        view.findViewById(R.id.menuSettings).setOnClickListener(v -> {
            sheet.dismiss();
            startActivity(new Intent(this, SettingsActivity.class));
        });
        view.findViewById(R.id.menuRate).setOnClickListener(v -> {
            sheet.dismiss();
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
            }
        });

        view.findViewById(R.id.menuShare).setOnClickListener(v -> {
            sheet.dismiss();
            shareApp();
        });
        view.findViewById(R.id.menuLogout).setOnClickListener(v -> {
            sheet.dismiss();
            FirebaseAuth.getInstance().signOut();
            getSharedPreferences("APP_DATA", MODE_PRIVATE).edit()
                    .remove("isLoggedIn")
                    .remove("role")
                    .apply();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finishAffinity();
        });

        sheet.show();
    }

    private void showLanguageDialog() {
        String[] lang = { getString(R.string.lang_english), getString(R.string.lang_hindi) };
        new AlertDialog.Builder(this)
                .setTitle(R.string.select_language)
                .setItems(lang, (d, i) -> {
                    if (i == 0)
                        setLocale("en");
                    else
                        setLocale("hi");
                }).show();
    }

    private void setLocale(String code) {
        LocaleListCompat appLocales = LocaleListCompat.forLanguageTags(code);
        AppCompatDelegate.setApplicationLocales(appLocales);
    }

    private void shareApp() {
        String shareMessage = getString(R.string.share_msg_body, getString(R.string.app_name), getPackageName());
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, shareMessage);
        startActivity(Intent.createChooser(i, getString(R.string.title_share_using)));
    }

    protected void deleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_delete_account)
                .setMessage(R.string.msg_delete_confirm)
                .setPositiveButton(R.string.yes, (d, w) -> {
                    String uid = FirebaseAuth.getInstance().getUid();
                    if (uid != null) {
                        // 1. Trigger deletion in DB (this will notify other devices)
                        FirebaseDatabase.getInstance().getReference("users").child(uid).removeValue();
                        
                        // 2. Delete Auth account
                        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                            FirebaseAuth.getInstance().getCurrentUser().delete();
                        }

                        // 3. Clear local session
                        getSharedPreferences("APP_DATA", MODE_PRIVATE).edit()
                                .remove("isLoggedIn")
                                .remove("role")
                                .remove("user_name")
                                .apply();

                        // 4. Redirect to login or register (Register seems better after full delete)
                        startActivity(new Intent(this, RegisterActivity.class));
                        finishAffinity();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void showContactSupportDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_contact_support, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        android.widget.TextView tvCall = dialogView.findViewById(R.id.tvCallNumber);
        android.widget.TextView tvEmail = dialogView.findViewById(R.id.tvEmailAddress);

        final String[] finalPhone = {"+917089927270"};
        final String[] finalEmail = {"prikhush332@gmail.com"};

        FirebaseDatabase.getInstance().getReference("admin_settings").child("support_contact")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot s) {
                        if(s.exists()) {
                            String dbPhone = s.child("phone").getValue(String.class);
                            String dbEmail = s.child("email").getValue(String.class);
                            if(dbPhone != null && !dbPhone.isEmpty()) {
                                finalPhone[0] = dbPhone;
                                tvCall.setText(dbPhone);
                            }
                            if(dbEmail != null && !dbEmail.isEmpty()) {
                                finalEmail[0] = dbEmail;
                                tvEmail.setText(dbEmail);
                            }
                        }
                    }
                    @Override public void onCancelled(DatabaseError e) {}
                });

        dialogView.findViewById(R.id.btnCallSupport).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + finalPhone[0])));
        });

        dialogView.findViewById(R.id.btnEmailSupport).setOnClickListener(v -> {
            dialog.dismiss();
            Intent email = new Intent(Intent.ACTION_SENDTO);
            email.setData(Uri.parse("mailto:" + finalEmail[0]));
            email.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.support_email_subject));
            startActivity(email);
        });

        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null && currentNavItemId != -1) {
            bottomNav.setSelectedItemId(currentNavItemId);
        }
    }

    @SuppressWarnings("deprecation")
    protected void applyTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.nav_fade_in, R.anim.nav_fade_out);
        } else {
            overridePendingTransition(R.anim.nav_fade_in, R.anim.nav_fade_out);
        }
    }

    private void refreshNotificationBadge(String uid) {
        DatabaseReference personalRef = FirebaseDatabase.getInstance().getReference("notifications").child(uid);
        DatabaseReference broadcastRef = FirebaseDatabase.getInstance().getReference("broadcast_notifications");
        DatabaseReference readBroadcastRef = FirebaseDatabase.getInstance().getReference("read_broadcasts").child(uid);

        ValueEventListener badgeListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                readBroadcastRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot readShot) {
                        java.util.HashSet<String> readIds = new java.util.HashSet<>();
                        for (DataSnapshot d : readShot.getChildren()) readIds.add(d.getKey());

                        broadcastRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot broadcastShot) {
                                int count = 0;
                                long now = System.currentTimeMillis();
                                for (DataSnapshot d : broadcastShot.getChildren()) {
                                    if (!readIds.contains(d.getKey())) {
                                        Long expiry = d.child("expiryDate").getValue(Long.class);
                                        if (expiry == null || expiry == 0 || expiry > now) count++;
                                    }
                                }
                                final int broadcastCount = count;
                                personalRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot personalShot) {
                                        int totalCount = broadcastCount;
                                        for (DataSnapshot d : personalShot.getChildren()) {
                                            Boolean r = d.child("read").getValue(Boolean.class);
                                            Long expiry = d.child("expiryDate").getValue(Long.class);
                                            if (r != null && !r) {
                                                if (expiry == null || expiry == 0 || expiry > now) totalCount++;
                                            }
                                        }
                                        updateBadgeUI(totalCount);
                                    }
                                    @Override public void onCancelled(DatabaseError error) {}
                                });
                            }
                            @Override public void onCancelled(DatabaseError error) {}
                        });
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
            }
            @Override public void onCancelled(DatabaseError error) {}
        };
        personalRef.addValueEventListener(badgeListener);
        broadcastRef.addValueEventListener(badgeListener);
        readBroadcastRef.addValueEventListener(badgeListener);
    }

    private void updateBadgeUI(int count) {
        if (headerBadge == null) return;
        if (count == 0) {
            headerBadge.setVisibility(View.GONE);
            me.leolin.shortcutbadger.ShortcutBadger.removeCount(this);
        } else {
            headerBadge.setText(String.valueOf(count));
            headerBadge.setVisibility(View.VISIBLE);
            me.leolin.shortcutbadger.ShortcutBadger.applyCount(this, count);
        }
    }

    protected void checkSubscription(Runnable onSubscribed) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        // Admins bypass subscription check
        SharedPreferences sp = getSharedPreferences("APP_DATA", MODE_PRIVATE);
        String role = sp.getString("role", "user");
        if ("admin".equalsIgnoreCase(role)) {
            onSubscribed.run();
            return;
        }

        FirebaseDatabase.getInstance().getReference("users").child(uid)
                .child("subscribed")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Boolean isSubscribed = snapshot.getValue(Boolean.class);
                        if (isSubscribed != null && isSubscribed) {
                            onSubscribed.run();
                        } else {
                            showSubscriptionRequiredPopup();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(BaseActivity.this, "Check failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showSubscriptionRequiredPopup() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_subscription_required)
                .setMessage(R.string.msg_subscription_required)
                .setPositiveButton(R.string.btn_subscribe_now, (d, w) -> {
                    startActivity(new Intent(this, SubscriptionActivity.class));
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    protected void checkDownloadLimit(Runnable onAllowed) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        // Admins bypass limit
        SharedPreferences sp = getSharedPreferences("APP_DATA", MODE_PRIVATE);
        if ("admin".equalsIgnoreCase(sp.getString("role", "user"))) {
            onAllowed.run();
            return;
        }

        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("user_activity")
                .child(uid).child("saves_count").child(today);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.exists() ? snapshot.getValue(Long.class) : 0;
                if (count < 10) {
                    onAllowed.run();
                } else {
                    Toast.makeText(BaseActivity.this, R.string.msg_download_limit_reached, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    protected void incrementDownloadCount() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        // Admins bypass limit tracking and UI
        SharedPreferences sp = getSharedPreferences("APP_DATA", MODE_PRIVATE);
        if ("admin".equalsIgnoreCase(sp.getString("role", "user"))) {
            return;
        }

        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("user_activity")
                .child(uid).child("saves_count").child(today);

        ref.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                long count = currentData.getValue() == null ? 0 : currentData.getValue(Long.class);
                currentData.setValue(count + 1);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (committed && currentData != null) {
                    Long count = currentData.getValue(Long.class);
                    if (count != null) {
                        runOnUiThread(() -> {
                            Toast.makeText(BaseActivity.this, getString(R.string.msg_downloads_remaining, count.intValue()), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
        });
    }

    protected String makeSafeKey(String value) {
        if (value == null) return "Unknown";
        return android.util.Base64.encodeToString(value.getBytes(), android.util.Base64.NO_WRAP)
                .replace(".", "_")
                .replace("$", "_")
                .replace("#", "_");
    }
    @Override
    protected void onDestroy() {
        if (accountExistenceRef != null && accountExistenceListener != null) {
            accountExistenceRef.removeEventListener(accountExistenceListener);
        }
        super.onDestroy();
    }
}
