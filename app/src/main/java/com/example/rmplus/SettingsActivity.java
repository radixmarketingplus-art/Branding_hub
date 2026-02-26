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

public class SettingsActivity extends AppCompatActivity {

    View changePasswordBtn, clearCacheBtn, privacyBtn, termsBtn, supportBtn;
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

        // ================= SUPPORT =================

        supportBtn.setOnClickListener(v -> showContactSupportDialog());
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

        dialogView.findViewById(R.id.btnCallSupport).setOnClickListener(v -> {
            dialog.dismiss();
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:+917089927270"));
            startActivity(callIntent);
        });

        dialogView.findViewById(R.id.btnEmailSupport).setOnClickListener(v -> {
            dialog.dismiss();
            Intent email = new Intent(Intent.ACTION_SENDTO);
            email.setData(Uri.parse("mailto:prikhush332@gmail.com"));
            email.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.support_email_subject));
            startActivity(email);
        });

        dialog.show();
    }
}
