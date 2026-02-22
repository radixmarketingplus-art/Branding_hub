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



import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    View changePasswordBtn, clearCacheBtn, privacyBtn, termsBtn;
    Switch notificationSwitch;
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
        notificationSwitch = findViewById(R.id.notificationSwitch);
        versionTxt = findViewById(R.id.versionTxt);

        auth = FirebaseAuth.getInstance();

        versionTxt.setText("Version 1.0");

//        Temporary cache test
//        try {
//            File test = new File(getCacheDir(), "test.tmp");
//            FileOutputStream fos = new FileOutputStream(test);
//            fos.write(new byte[10 * 1024 * 1024]); // 10 MB
//            fos.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


        // ================= NOTIFICATION PREF =================

        prefs = getSharedPreferences("app_settings", MODE_PRIVATE);

        boolean notifEnabled =
                prefs.getBoolean("notifications", true);

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
                                "Notifications Enabled",
                                Toast.LENGTH_SHORT).show();

                    } else {

                        FirebaseMessaging.getInstance()
                                .unsubscribeFromTopic("all");

                        Toast.makeText(this,
                                "Notifications Disabled",
                                Toast.LENGTH_SHORT).show();
                    }
                });


        // ================= CHANGE PASSWORD =================

        changePasswordBtn.setOnClickListener(v -> {

            if (auth.getCurrentUser() == null) return;

            new AlertDialog.Builder(this)
                    .setTitle("Reset Password")
                    .setMessage("Send password reset link to your email?")
                    .setPositiveButton("Yes", (d, w) -> {

                        auth.sendPasswordResetEmail(
                                auth.getCurrentUser().getEmail());

                        Toast.makeText(this,
                                "Reset link sent to email",
                                Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        // ================= CLEAR CACHE =================

        clearCacheBtn.setOnClickListener(v -> {

            long before = getTotalCacheSize();

            new AlertDialog.Builder(this)
                    .setTitle("Clear Cache")
                    .setMessage("Clear temporary files?")
                    .setPositiveButton("Yes", (d, w) -> {

                        deleteCache();

                        long after = getTotalCacheSize();

                        Toast.makeText(this,
                                "Cleared: " +
                                        formatSize(before - after),
                                Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });


        // ================= PRIVACY =================

        privacyBtn.setOnClickListener(v ->
                openWeb("https://example.com/privacy"));

        // ================= TERMS =================

        termsBtn.setOnClickListener(v ->
                openWeb("https://example.com/terms"));
    }

    // ================= CACHE DELETE =================

    void deleteCache() {
        try {
            deleteDir(getCacheDir());

            File ext = getExternalCacheDir();
            if (ext != null) deleteDir(ext);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                if (!deleteDir(new File(dir, child))) return false;
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
        if (size <= 0) return "0 B";

        final String[] units =
                {"B", "KB", "MB", "GB"};

        int digitGroups =
                (int) (Math.log10(size) / Math.log10(1024));

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
}
