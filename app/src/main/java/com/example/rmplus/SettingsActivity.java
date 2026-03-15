package com.example.rmplus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import com.google.firebase.messaging.FirebaseMessaging;
import androidx.appcompat.widget.SwitchCompat;

import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class SettingsActivity extends BaseActivity {

    View changePasswordBtn, clearCacheBtn, privacyBtn, termsBtn, supportBtn;
    View whatsappBtn, instagramBtn, facebookBtn, twitterBtn;
    SwitchCompat notificationSwitch;
    TextView versionTxt;

    FirebaseAuth auth;

    // ⭐ For notification toggle
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        changePasswordBtn = findViewById(R.id.changePasswordBtn);
        clearCacheBtn = findViewById(R.id.clearCacheBtn);
        privacyBtn = findViewById(R.id.privacyBtn);
        termsBtn = findViewById(R.id.termsBtn);
        supportBtn = findViewById(R.id.supportBtn);
        notificationSwitch = findViewById(R.id.notificationSwitch);
        versionTxt = findViewById(R.id.versionTxt);

        auth = FirebaseAuth.getInstance();

        try {
            android.content.pm.PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionTxt.setText("Version " + pInfo.versionName);
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            versionTxt.setText("Version 1.0");
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // LISTENERS
        // Temporary cache test
        // try {
        // File test = new File(getCacheDir(), "test.tmp");
        // FileOutputStream fos = new FileOutputStream(test);
        // fos.write(new byte[10 * 1024 * 1024]); // 10 MB
        // fos.close();
        // } catch (Exception e) {
        // e.printStackTrace();
        // }

        // ================= NOTIFICATION PREF =================

        prefs = getSharedPreferences("app_settings", MODE_PRIVATE);

        boolean notifEnabled = prefs.getBoolean("notifications", true);

        notificationSwitch.setChecked(notifEnabled);
        // Apply subscription on app start
        if (notifEnabled) {
            FirebaseMessaging.getInstance()
                    .subscribeToTopic("all");
        } else {
            FirebaseMessaging.getInstance()
                    .unsubscribeFromTopic("all");
        }

        notificationSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    prefs.edit()
                            .putBoolean("notifications", isChecked)
                            .apply();

                    if (isChecked) {

                        FirebaseMessaging.getInstance()
                                .subscribeToTopic("all");

                        Toast.makeText(this,
                                R.string.msg_notif_enabled,
                                Toast.LENGTH_SHORT).show();

                    } else {

                        FirebaseMessaging.getInstance()
                                .unsubscribeFromTopic("all");

                        Toast.makeText(this,
                                R.string.msg_notif_disabled,
                                Toast.LENGTH_SHORT).show();
                    }
                });

        // ================= CHANGE PASSWORD =================

        changePasswordBtn.setOnClickListener(v -> {

            if (auth.getCurrentUser() == null)
                return;

            new AlertDialog.Builder(this)
                    .setTitle(R.string.title_reset_password)
                    .setMessage(R.string.msg_reset_password)
                    .setPositiveButton(R.string.yes, (d, w) -> {

                        auth.sendPasswordResetEmail(
                                auth.getCurrentUser().getEmail());

                        Toast.makeText(this,
                                R.string.msg_reset_link_sent,
                                Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        });

        // ================= CLEAR CACHE =================

        clearCacheBtn.setOnClickListener(v -> {

            long before = getTotalCacheSize();

            new AlertDialog.Builder(this)
                    .setTitle(R.string.title_clear_cache)
                    .setMessage(R.string.msg_clear_cache)
                    .setPositiveButton(R.string.yes, (d, w) -> {

                        deleteCache();

                        long after = getTotalCacheSize();

                        Toast.makeText(this,
                                getString(R.string.msg_cleared, formatSize(before - after)),
                                Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        });

        // ================= PRIVACY =================

        // privacyBtn.setOnClickListener(v ->
        // openWeb("https://example.com/privacy"));

        // termsBtn.setOnClickListener(v ->
        // openWeb("https://example.com/terms"));

        privacyBtn.setOnClickListener(v->
                startActivity(new Intent(this,
                        PrivacyPolicyActivity.class)));

        termsBtn.setOnClickListener(v->
                startActivity(new Intent(this,
                        TermsActivity.class)));

        // ================= SUPPORT =================
        supportBtn.setOnClickListener(v -> showContactSupportDialog());

        // ================= DELETE ACCOUNT =================
        findViewById(R.id.btnDeleteAccount).setOnClickListener(v -> deleteAccount());

        // ================= SOCIAL MEDIA =================
        whatsappBtn = findViewById(R.id.whatsappBtn);
        instagramBtn = findViewById(R.id.instagramBtn);
        facebookBtn = findViewById(R.id.facebookBtn);
        twitterBtn = findViewById(R.id.twitterBtn);

        whatsappBtn.setOnClickListener(v -> {
            String url = "https://wa.me/919981681068";
            openWeb(url);
        });

        instagramBtn.setOnClickListener(v -> {
            String url = "https://www.instagram.com/radixmarketingplus?igsh=OGsxbWxhdnNiZzhp&utm_source=ig_contact_invite";
            openWeb(url);
        });

        facebookBtn.setOnClickListener(v -> {
            String url = "https://www.facebook.com/share/1GasbS6E4P/?mibextid=wwXIfr";
            openWeb(url);
        });

        twitterBtn.setOnClickListener(v -> {
            String url = "https://x.com/radix_marketing?s=21";
            openWeb(url);
        });
    }

    // ================= CACHE DELETE =================

    void deleteCache() {
        try {
            deleteDir(getCacheDir());

            File ext = getExternalCacheDir();
            if (ext != null)
                deleteDir(ext);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                if (!deleteDir(new File(dir, child)))
                    return false;
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    long getTotalCacheSize() {

        long size = getCacheSize(getCacheDir());

        File ext = getExternalCacheDir();

        if (ext != null) {
            size += getCacheSize(ext);
        }

        return size;
    }

    long getCacheSize(File dir) {
        long size = 0;

        if (dir != null && dir.listFiles() != null) {
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    size += getCacheSize(file);
                } else {
                    size += file.length();
                }
            }
        }

        return size;
    }

    String formatSize(long size) {
        if (size <= 0)
            return "0 B";

        final String[] units = { "B", "KB", "MB", "GB" };

        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

        return String.format("%.2f %s",
                size / Math.pow(1024, digitGroups),
                units[digitGroups]);
    }

    // ================= OPEN WEB =================

    void openWeb(String url) {
        startActivity(new Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)));
    }

    void showContactSupportDialog() {
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
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:" + finalPhone[0]));
            startActivity(callIntent);
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
}
