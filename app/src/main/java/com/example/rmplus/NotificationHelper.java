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

        // ✅ Localize BEFORE showing tray/phone notification
        String localizedTitle = getLocalized(context, title);
        String localizedMessage = getLocalized(context, message);

        showPhoneNotification(context, localizedTitle, localizedMessage);
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
                            context.getString(R.string.app_name) + " " + context.getString(R.string.btn_notifications),
                            NotificationManager.IMPORTANCE_HIGH
                    );

            manager.createNotificationChannel(channel);
        }

        Intent intent =
                new Intent(context,
                        NotificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

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

    public static String getLocalized(android.content.Context context, String text) {
        if (text == null || text.isEmpty()) return "";

        // Subscription
        if (text.equalsIgnoreCase("Subscription Approved")) return context.getString(R.string.title_sub_approved);
        if (text.equalsIgnoreCase("Your plan has been approved!")) return context.getString(R.string.desc_sub_approved);
        if (text.equalsIgnoreCase("Subscription Rejected")) return context.getString(R.string.title_sub_rejected);
        if (text.equalsIgnoreCase("Your plan request was rejected.")) return context.getString(R.string.desc_sub_rejected);
        if (text.equalsIgnoreCase("Subscription Request Sent")) return context.getString(R.string.msg_sub_request_sent);
        if (text.equalsIgnoreCase("Your subscription request has been sent for review.")) return context.getString(R.string.msg_sub_request_sent_desc);

        // Profile
        if (text.equalsIgnoreCase("Profile Updated")) return context.getString(R.string.msg_profile_updated);
        if (text.equalsIgnoreCase("Your profile information has been successfully updated.") || 
            text.equalsIgnoreCase("Your profile updated successfully")) 
            return context.getString(R.string.msg_profile_updated_desc);

        // Support Request
        if (text.equalsIgnoreCase("New Support Request")) return context.getString(R.string.notif_new_request);

        // Advertisement
        if (text.startsWith("Advertisement Status: ")) {
            String s = text.replace("Advertisement Status: ", "").trim();
            String localizedS = s;
            if (s.equalsIgnoreCase("pending")) localizedS = context.getString(R.string.tab_pending);
            else if (s.equalsIgnoreCase("accepted")) localizedS = context.getString(R.string.tab_accepted);
            else if (s.equalsIgnoreCase("rejected")) localizedS = context.getString(R.string.tab_rejected);
            return context.getString(R.string.msg_adv_notif_title, localizedS);
        }

        // Customer Request
        if (text.startsWith("Request Status: ")) {
            String s = text.replace("Request Status: ", "").trim();
            String localizedS = s;
            if (s.equalsIgnoreCase("pending")) localizedS = context.getString(R.string.tab_pending);
            else if (s.equalsIgnoreCase("accepted")) localizedS = context.getString(R.string.tab_accepted);
            else if (s.equalsIgnoreCase("rejected")) localizedS = context.getString(R.string.tab_rejected);
            return context.getString(R.string.msg_request_status_format, localizedS);
        }

        return text;
    }
}
