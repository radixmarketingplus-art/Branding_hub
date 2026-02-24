package com.example.rmplus;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.rmplus.models.AdvertisementRequest;
import com.google.firebase.database.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

public class ExpiryCleanupHelper {

    public static void checkAndClean(Context context) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("templates");
        long now = System.currentTimeMillis();

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot sectionSnapshot : snapshot.getChildren()) {
                    String category = sectionSnapshot.getKey();
                    for (DataSnapshot templateSnapshot : sectionSnapshot.getChildren()) {
                        
                        long expiry = 0;
                        String url = null;
                        
                        if (templateSnapshot.hasChild("expiryDate")) {
                            expiry = (long) templateSnapshot.child("expiryDate").getValue();
                        }
                        
                        if (templateSnapshot.hasChild("url")) {
                            url = (String) templateSnapshot.child("url").getValue();
                        } else if (templateSnapshot.hasChild("imagePath")) {
                            url = (String) templateSnapshot.child("imagePath").getValue();
                        }

                        if (expiry > 0 && now > expiry && url != null) {
                            // 🔥 EXPIRED!
                            performNuclearDelete(context, category, templateSnapshot.getKey(), url);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private static void performNuclearDelete(Context context, String category, String key, String url) {
        // 1. Remove from Firebase templates node
        FirebaseDatabase.getInstance().getReference("templates")
                .child(category).child(key).removeValue();

        // 2. Remove stats/activity
        FirebaseDatabase.getInstance().getReference("template_activity")
                .child(key).removeValue();

        // 3. Remove from local SharedPreferences
        removeFromLocal(context, category, url);

        // 4. Delete from VPS
        deleteFromVPS(url);
    }

    private static void removeFromLocal(Context context, String category, String url) {
        SharedPreferences sp = context.getSharedPreferences("HOME_DATA", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sp.getString(category, null);
        if (json == null) return;

        SharedPreferences.Editor editor = sp.edit();
        if ("Festival Cards".equalsIgnoreCase(category)) {
            Type t = new TypeToken<ArrayList<FestivalCardItem>>(){}.getType();
            ArrayList<FestivalCardItem> list = gson.fromJson(json, t);
            if (list != null) {
                list.removeIf(item -> item.imagePath.equals(url));
                editor.putString(category, gson.toJson(list));
            }
        } else if ("Advertisement".equalsIgnoreCase(category)) {
            Type t = new TypeToken<ArrayList<AdvertisementItem>>(){}.getType();
            ArrayList<AdvertisementItem> list = gson.fromJson(json, t);
            if (list != null) {
                list.removeIf(item -> item.imagePath.equals(url));
                editor.putString(category, gson.toJson(list));
            }
        } else {
            Type t = new TypeToken<ArrayList<String>>(){}.getType();
            ArrayList<String> list = gson.fromJson(json, t);
            if (list != null) {
                list.remove(url);
                editor.putString(category, gson.toJson(list));
            }
        }
        editor.apply();
    }

    private static void deleteFromVPS(String imageUrl) {
        new Thread(() -> {
            try {
                URL deleteUrl = new URL("http://187.77.184.84/delete.php");
                HttpURLConnection conn = (HttpURLConnection) deleteUrl.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                String data = "url=" + URLEncoder.encode(imageUrl, "UTF-8");
                conn.getOutputStream().write(data.getBytes());
                conn.getResponseCode();
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }
}
