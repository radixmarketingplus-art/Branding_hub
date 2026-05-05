package com.rmads.maker;

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

        showPhoneNotification(context, uid, localizedTitle, localizedMessage, null, null);
        
        // ✅ Also send FCM push so notification arrives even when app is CLOSED/KILLED
        sendFcmPush(context, uid, localizedTitle, localizedMessage);
    }

    // ✅ Send FCM push via VPS PHP (works even when app is CLOSED/KILLED)
    private static void sendFcmPush(Context context, String uid, String title, String message) {
        FirebaseDatabase.getInstance().getReference("users").child(uid).child("fcmToken")
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                        String token = snapshot.getValue(String.class);
                        if (token == null || token.isEmpty()) return;

                        new Thread(() -> {
                            try {
                                java.net.URL url = new java.net.URL("http://187.77.184.84/uploads/send_notification.php");
                                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                                conn.setRequestMethod("POST");
                                conn.setDoOutput(true);
                                conn.setConnectTimeout(10000);
                                conn.setReadTimeout(10000);

                                String params = "secret=rmplus_notif_secret_2024"
                                        + "&token=" + java.net.URLEncoder.encode(token, "UTF-8")
                                        + "&title=" + java.net.URLEncoder.encode(title, "UTF-8")
                                        + "&message=" + java.net.URLEncoder.encode(message, "UTF-8");

                                java.io.OutputStream os = conn.getOutputStream();
                                os.write(params.getBytes("UTF-8"));
                                os.close();

                                int code = conn.getResponseCode();
                                android.util.Log.d("FCMPush", "VPS response: " + code);
                                conn.disconnect();

                            } catch (Exception e) {
                                android.util.Log.e("FCMPush", "Error: " + e.getMessage());
                            }
                        }).start();
                    }
                    @Override
                    public void onCancelled(com.google.firebase.database.DatabaseError e) {}
                });
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
        showPhoneNotification(context, uid, localizedTitle, localizedMessage, action, extraData);
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

        // ✅ Also send FCM Broadcase push so notification arrives even when app is CLOSED/KILLED
        sendBroadcastPush(context, title, message);

        // ✅ Also show tray notification for the current admin (optional but good for feedback)
        String localizedTitle = getLocalized(context, title);
        String localizedMessage = getLocalized(context, message);
        showPhoneNotification(context, null, localizedTitle, localizedMessage, action, extraData);
    }

    // ✅ Send FCM broadcast push via VPS PHP (targeted at 'all_users' topic)
    private static void sendBroadcastPush(Context context, String title, String message) {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://187.77.184.84/uploads/send_notification.php");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                String params = "secret=rmplus_notif_secret_2024"
                        + "&topic=all_users"
                        + "&title=" + java.net.URLEncoder.encode(title, "UTF-8")
                        + "&message=" + java.net.URLEncoder.encode(message, "UTF-8");

                java.io.OutputStream os = conn.getOutputStream();
                os.write(params.getBytes("UTF-8"));
                os.close();

                int code = conn.getResponseCode();
                android.util.Log.d("FCMPush", "VPS Broadcast response: " + code);
                conn.disconnect();

            } catch (Exception e) {
                android.util.Log.e("FCMPush", "Broadcast Error: " + e.getMessage());
            }
        }).start();
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
                                              String uid,
                                              String title,
                                              String message,
                                              String action,
                                              String extraData){

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
            channel.setShowBadge(true); // 🔥 ENSURE BADGES ARE ENABLED
            manager.createNotificationChannel(channel);
        }

        // FETCH CURRENT UNREAD COUNT FROM FIREBASE (For Badge)
        if (uid != null) {
            FirebaseDatabase.getInstance().getReference("notifications").child(uid)
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                        int count = 0;
                        for (com.google.firebase.database.DataSnapshot d : snapshot.getChildren()) {
                            Boolean r = d.child("read").getValue(Boolean.class);
                            if (r != null && !r) count++;
                        }
                        displayNotification(context, manager, title, message, count, action, extraData);
                    }
                    @Override public void onCancelled(com.google.firebase.database.DatabaseError error) {
                        displayNotification(context, manager, title, message, 0, action, extraData);
                    }
                });
        } else {
            // Probably a broadcast, we don't fetch per-user count here for broadcast
            displayNotification(context, manager, title, message, 0, action, extraData);
        }
    }

    private static void displayNotification(Context context, NotificationManager manager, String title, String message, int count, String action, String extraData) {
        
        // 🚀 UPDATE LAUNCHER BADGE (Universal Support)
        if (count > 0) {
            me.leolin.shortcutbadger.ShortcutBadger.applyCount(context, count);
        } else {
            me.leolin.shortcutbadger.ShortcutBadger.removeCount(context);
        }

        Intent intent = new Intent(context, NotificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        if (action != null) intent.putExtra("action", action);
        if (extraData != null) intent.putExtra("extraData", extraData);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                                PendingIntent.FLAG_IMMUTABLE
                );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setLargeIcon(android.graphics.BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))


                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setNumber(count) // 🔥 THIS SETS THE BADGE COUNT
                        .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        manager.notify((int) System.currentTimeMillis(), builder.build());
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
            
        if (text.equalsIgnoreCase("Subscription Request Sent")) return context.getString(R.string.title_sub_request_sent);
        if (text.equalsIgnoreCase("Your subscription request has been sent for review.") || 
            text.equalsIgnoreCase("Your subscription request has been submitted for review.")) return context.getString(R.string.msg_sub_request_sent_user);

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

        if (text.equalsIgnoreCase("New Premium Advertisement")) return context.getString(R.string.title_notif_new_ad);
        if (text.equalsIgnoreCase("Check out our latest sponsored ad!")) return context.getString(R.string.msg_notif_new_ad);

        // Advertisement (Admin)
        if (text.equalsIgnoreCase("New Advertisement Request")) return context.getString(R.string.title_notif_admin_new_adv);
        if (text.equalsIgnoreCase("A user has submitted a new advertisement request.")) return context.getString(R.string.msg_notif_admin_new_adv);

        // Support / Contact Requests (User)
        if (text.equalsIgnoreCase("Support Request Approved")) return context.getString(R.string.title_notif_support_approved);
        if (text.equalsIgnoreCase("Your support request has been approved. You can now chat with our agent.")) return context.getString(R.string.msg_notif_support_approved);

        if (text.equalsIgnoreCase("Support Request Rejected")) return context.getString(R.string.title_notif_support_rejected);
        if (text.equalsIgnoreCase("Your support request has been rejected.")) return context.getString(R.string.msg_notif_support_rejected);

        if (text.equalsIgnoreCase("Support Request Sent")) return context.getString(R.string.title_notif_support_sent);
        if (text.equalsIgnoreCase("Your support request has been submitted successfully.")) return context.getString(R.string.msg_notif_support_sent);

        // Support (Admin)
        if (text.equalsIgnoreCase("New Support Request")) return context.getString(R.string.title_notif_new_support);
        if (text.equalsIgnoreCase("A user has submitted a new support/contact request.")) return context.getString(R.string.msg_notif_new_support);

        // Client Info (Admin)
        if (text.equalsIgnoreCase("New Client Info Request")) return context.getString(R.string.title_notif_client_info);
        if (text.equalsIgnoreCase("A user has submitted a new client info request.")) return context.getString(R.string.msg_notif_client_info);

        // Chat
        if (text.equalsIgnoreCase("New Message")) return context.getString(R.string.title_notif_new_message);
        if (text.equalsIgnoreCase("New message from Support Agent.")) return context.getString(R.string.msg_notif_new_message_user);
        if (text.equalsIgnoreCase("New message from user regarding support request.")) return context.getString(R.string.msg_notif_new_message_admin);

        // Profile
        if (text.equalsIgnoreCase("Profile Updated")) return context.getString(R.string.msg_profile_updated);
        if (text.equalsIgnoreCase("Your profile information has been successfully updated.") || 
            text.equalsIgnoreCase("Your profile updated successfully") ||
            text.equalsIgnoreCase("Your profile has been updated by an administrator.")) 
            return context.getString(R.string.msg_profile_updated_desc);

        // Latest Update
        if (text.equalsIgnoreCase("New Latest Update")) return context.getString(R.string.title_notif_latest_update);
        if (text.equalsIgnoreCase("Check out the new design in Latest Update section!")) return context.getString(R.string.msg_check_latest_update);

        // Business Frame
        if (text.equalsIgnoreCase("New Business Frame")) return context.getString(R.string.title_notif_business_frame);
        if (text.equalsIgnoreCase("New premium frames added to Business section!")) return context.getString(R.string.msg_notif_business_frame);

        // Offer
        if (text.equalsIgnoreCase("New Special Offer!")) return context.getString(R.string.title_notif_new_offer);
        if (text.equalsIgnoreCase("Check out the exclusive offer now!")) return context.getString(R.string.msg_notif_new_offer);

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
