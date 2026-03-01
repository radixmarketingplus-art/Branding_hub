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

    // NEW: Overloaded send with action and expiry support
    public static void send(Context context,
                            String uid,
                            String title,
                            String message,
                            String action,
                            String extraData,
                            long expiryDate){

        SharedPreferences prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("notifications", true)) return;

        DatabaseReference ref = FirebaseDatabase.getInstance()
                        .getReference("notifications")
                        .child(uid)
                        .push();

        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("title", title);
        map.put("message", message);
        map.put("read", false);
        map.put("time", System.currentTimeMillis());
        map.put("action", action);
        map.put("extraData", extraData);
        map.put("expiryDate", expiryDate);

        ref.setValue(map);

        String localizedTitle = getLocalized(context, title);
        String localizedMessage = getLocalized(context, message);
        showPhoneNotification(context, localizedTitle, localizedMessage);
    }

    // NEW: Send broadcast notification to ALL users
    public static void sendBroadcast(Context context,
                                     String customKey,
                                     String title,
                                     String message,
                                     String action,
                                     String extraData,
                                     long expiryDate){

        // FIREBASE BROADCAST
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference("broadcast_notifications");
        DatabaseReference ref = (customKey != null) ? rootRef.child(customKey) : rootRef.push();

        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("title", title);
        map.put("message", message);
        map.put("time", System.currentTimeMillis());
        map.put("action", action);
        map.put("extraData", extraData);
        map.put("expiryDate", expiryDate);

        ref.setValue(map);

        // ✅ Also show tray notification for the current admin (optional but good for feedback)
        String localizedTitle = getLocalized(context, title);
        String localizedMessage = getLocalized(context, message);
        showPhoneNotification(context, localizedTitle, localizedMessage);
    }

    // NEW: Delete broadcast notification from ALL users (when item is deleted)
    public static void deleteBroadcast(String customKey) {
        if (customKey == null) return;
        FirebaseDatabase.getInstance().getReference("broadcast_notifications")
                .child(customKey).removeValue();
    }

    // NEW: Notify all admins
    public static void notifyAdmins(Context context, String title, String message, String action, String extraData) {
        FirebaseDatabase.getInstance().getReference("users")
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                for (com.google.firebase.database.DataSnapshot d : snapshot.getChildren()) {
                    String role = d.child("role").getValue(String.class);
                    if ("admin".equalsIgnoreCase(role)) {
                        send(context, d.getKey(), title, message, action, extraData, 0);
                    }
                }
            }
            @Override public void onCancelled(com.google.firebase.database.DatabaseError error) {}
        });
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

        // Subscription (User)
        if (text.equalsIgnoreCase("Subscription Approved")) return context.getString(R.string.title_sub_approved);
        if (text.equalsIgnoreCase("Your plan has been approved!") || 
            text.equalsIgnoreCase("Your subscription has been approved")) return context.getString(R.string.desc_sub_approved);
            
        if (text.equalsIgnoreCase("Subscription Rejected")) return context.getString(R.string.title_sub_rejected);
        if (text.equalsIgnoreCase("Your plan request was rejected.") || 
            text.equalsIgnoreCase("Your subscription has been rejected")) return context.getString(R.string.desc_sub_rejected);
            
        if (text.equalsIgnoreCase("Subscription Request Sent")) return context.getString(R.string.msg_sub_request_sent);
        if (text.equalsIgnoreCase("Your subscription request has been sent for review.") || 
            text.equalsIgnoreCase("Your subscription request has been submitted for review.")) return context.getString(R.string.msg_sub_request_sent_desc);

        if (text.equalsIgnoreCase("Subscription Expired")) return context.getString(R.string.title_notif_sub_expired);
        if (text.equalsIgnoreCase("Your premium subscription has expired. Renew now to continue enjoying benefits.")) return context.getString(R.string.msg_notif_sub_expired);

        // Subscription (Admin)
        if (text.equalsIgnoreCase("New Subscription Request")) return context.getString(R.string.title_notif_admin_new_sub);
        if (text.equalsIgnoreCase("A user has submitted a new subscription request.")) return context.getString(R.string.msg_notif_admin_new_sub);

        // Advertisement (User)
        if (text.equalsIgnoreCase("Advertisement Approved")) return context.getString(R.string.title_notif_adv_approved);
        if (text.equalsIgnoreCase("Your advertisement has been approved and is now live.")) return context.getString(R.string.msg_notif_adv_approved);

        if (text.equalsIgnoreCase("Advertisement Rejected")) return context.getString(R.string.title_notif_adv_rejected);
        if (text.equalsIgnoreCase("Your advertisement request has been rejected.")) return context.getString(R.string.msg_notif_adv_rejected);

        if (text.equalsIgnoreCase("Advertisement Request Sent")) return context.getString(R.string.msg_adv_request_sent);
        if (text.equalsIgnoreCase("Your advertisement request has been submitted for review.")) return context.getString(R.string.msg_adv_request_sent_desc);

        if (text.equalsIgnoreCase("Advertisement Expired")) return context.getString(R.string.title_notif_adv_expired);
        if (text.equalsIgnoreCase("Your advertisement has expired. You can submit a new request to go live again.")) return context.getString(R.string.msg_notif_adv_expired);

        // Advertisement (Admin)
        if (text.equalsIgnoreCase("New Advertisement Request")) return context.getString(R.string.title_notif_admin_new_adv);
        if (text.equalsIgnoreCase("A user has submitted a new advertisement request.")) return context.getString(R.string.msg_notif_admin_new_adv);

        // Support / Contact Requests (User)
        if (text.equalsIgnoreCase("Support Request Approved")) return context.getString(R.string.title_notif_support_approved);
        if (text.equalsIgnoreCase("Your support request has been approved. You can now chat with our agent.")) return context.getString(R.string.msg_notif_support_approved);

        if (text.equalsIgnoreCase("Support Request Rejected")) return context.getString(R.string.title_notif_support_rejected);
        if (text.equalsIgnoreCase("Your support request has been rejected.")) return context.getString(R.string.msg_notif_support_rejected);

        if (text.equalsIgnoreCase("Support Request Sent")) return context.getString(R.string.msg_request_sent);
        if (text.equalsIgnoreCase("Your support request has been submitted successfully.")) return context.getString(R.string.msg_support_request_sent_desc);

        // Support (Admin)
        if (text.equalsIgnoreCase("New Support Request")) return context.getString(R.string.title_notif_admin_new_support);
        if (text.equalsIgnoreCase("A user has submitted a new support/contact request.")) return context.getString(R.string.msg_notif_admin_new_support);

        // Chat
        if (text.equalsIgnoreCase("New Message")) return context.getString(R.string.title_notif_new_message);
        if (text.equalsIgnoreCase("New message from Support Agent.")) return context.getString(R.string.msg_notif_new_message_user);
        if (text.equalsIgnoreCase("New message from user regarding support request.")) return context.getString(R.string.msg_notif_new_message_admin);

        // Profile
        if (text.equalsIgnoreCase("Profile Updated")) return context.getString(R.string.msg_profile_updated);
        if (text.equalsIgnoreCase("Your profile information has been successfully updated.") || 
            text.equalsIgnoreCase("Your profile updated successfully")) 
            return context.getString(R.string.msg_profile_updated_desc);

        // Latest Update
        if (text.equalsIgnoreCase("New Latest Update")) return context.getString(R.string.section_latest_update);
        if (text.equalsIgnoreCase("Check out the new design in Latest Update section!")) return context.getString(R.string.msg_check_latest_update);

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
