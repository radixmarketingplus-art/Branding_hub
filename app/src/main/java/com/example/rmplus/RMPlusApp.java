package com.example.rmplus;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.firebase.FirebaseApp;

public class RMPlusApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);

        // Force Global Theme state once on Startup
        SharedPreferences sp = getSharedPreferences("APP_DATA", MODE_PRIVATE);
        int nightMode = sp.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_NO);
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }
}
