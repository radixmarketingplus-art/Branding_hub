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
                    if (category == null) continue;

                    // recursive check for items or sub-categories
                    checkNode(context, sectionSnapshot, category, now);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private static void checkNode(Context context, DataSnapshot node, String path, long now) {
        for (DataSnapshot child : node.getChildren()) {
            if (child.hasChild("expiryDate")) {
                // It's a template item
                long expiry = 0;
                Object val = child.child("expiryDate").getValue();
                if (val instanceof Long) expiry = (Long) val;
                else if (val instanceof Integer) expiry = ((Integer) val).longValue();

                String url = null;
                if (child.hasChild("url")) {
                    url = child.child("url").getValue(String.class);
                } else if (child.hasChild("imagePath")) {
                    url = child.child("imagePath").getValue(String.class);
                }

                if (expiry > 0 && now > expiry && url != null) {
                    performNuclearDelete(context, path, child.getKey(), url);
                }
            } else if (child.getChildrenCount() > 0) {
                // It's likely a sub-category (like 'Political' inside 'Business Frame')
                checkNode(context, child, path + "/" + child.getKey(), now);
            }
        }
    }

    private static void performNuclearDelete(Context context, String path, String key, String url) {
        // 1. Remove from Firebase
        FirebaseDatabase.getInstance().getReference("templates")
                .child(path).child(key).removeValue();

        // 2. Remove stats
        FirebaseDatabase.getInstance().getReference("template_activity")
                .child(key).removeValue();

        // 3. Remove from SharedPreferences
        // category for SP is the first part of the path (e.g., "Business Frame")
        String rootCategory = path.contains("/") ? path.split("/")[0] : path;
        removeFromLocal(context, rootCategory, url);

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
                list.removeIf(item -> item.imagePath != null && item.imagePath.equals(url));
                editor.putString(category, gson.toJson(list));
            }
        } else if ("Advertisement".equalsIgnoreCase(category)) {
            Type t = new TypeToken<ArrayList<AdvertisementItem>>(){}.getType();
            ArrayList<AdvertisementItem> list = gson.fromJson(json, t);
            if (list != null) {
                list.removeIf(item -> item.imagePath != null && item.imagePath.equals(url));
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
