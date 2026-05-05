package com.rmads.maker;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import com.google.firebase.FirebaseApp;
import java.io.File;

@UnstableApi
public class RMPlusApp extends Application {
    private static SimpleCache videoCache;

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);

        // 🔥 Initialize ExoPlayer Disk Cache (50MB)
        if (videoCache == null) {
            LeastRecentlyUsedCacheEvictor evictor = new LeastRecentlyUsedCacheEvictor(50 * 1024 * 1024);
            StandaloneDatabaseProvider databaseProvider = new StandaloneDatabaseProvider(this);
            videoCache = new SimpleCache(new File(getCacheDir(), "video_cache"), evictor, databaseProvider);
        }

        // Force Global Theme state once on Startup
        SharedPreferences sp = getSharedPreferences("APP_DATA", MODE_PRIVATE);
        int nightMode = sp.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_NO);
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    public static SimpleCache getVideoCache() {
        return videoCache;
    }
}
