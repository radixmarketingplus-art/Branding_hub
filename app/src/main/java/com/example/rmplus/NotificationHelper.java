package com.example.rmplus;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class NotificationHelper {

    private static final String CHANNEL_ID = "rmplus_channel";

    // Save notification + show phone notification
    public static void send(Context context,
                            String uid,
                            String title,
                            String message){

        // 🔥 CHECK USER NOTIFICATION SETTING FIRST
        SharedPreferences prefs =
                context.getSharedPreferences(
                        "app_settings",
                        Context.MODE_PRIVATE);

        boolean enabled =
                prefs.getBoolean("notifications", true);

        // ❌ If OFF → DO NOT SHOW notification
        if (!enabled) return;

        DatabaseReference ref =
                FirebaseDatabase.getInstance()
                        .getReference("notifications")
                        .child(uid)
                        .push();

        ref.child("title").setValue(title);
        ref.child("message").setValue(message);
        ref.child("read").setValue(false);
        ref.child("time").setValue(System.currentTimeMillis());

        showPhoneNotification(context,title,message);
    }

    private static void showPhoneNotification(Context context,
                                              String title,
                                              String message){

        NotificationManager manager =
                (NotificationManager)
                        context.getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "RM Plus Notifications",
                            NotificationManager.IMPORTANCE_HIGH
                    );

            manager.createNotificationChannel(channel);
        }

        Intent intent =
                new Intent(context,
                        NotificationActivity.class);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                                PendingIntent.FLAG_IMMUTABLE
                );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context,CHANNEL_ID)
                        .setSmallIcon(
                                android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setPriority(
                                NotificationCompat.PRIORITY_HIGH
                        );

        manager.notify(
                (int) System.currentTimeMillis(),
                builder.build()
        );
    }
}
