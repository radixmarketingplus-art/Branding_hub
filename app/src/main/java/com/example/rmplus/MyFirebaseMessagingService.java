package com.example.rmplus;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashSet;
import java.util.Set;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "rmplus_channel";

    // ✅ Called when a new FCM token is generated (first install or token refresh)
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        saveFcmTokenToFirebase(token);
    }

    // ✅ Called when a message is received (both foreground & background data messages)
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = null;
        String body = null;

        // Notification payload (shown by system in background — but we also handle it manually)
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body  = remoteMessage.getNotification().getBody();
        }

        // Data payload (always delivered to this method regardless of app state)
        if (remoteMessage.getData().size() > 0) {
            if (remoteMessage.getData().containsKey("title"))
                title = remoteMessage.getData().get("title");
            if (remoteMessage.getData().containsKey("message"))
                body  = remoteMessage.getData().get("message");
        }

        if (title != null && body != null) {
            String uid = FirebaseAuth.getInstance().getUid();
            if (uid != null) {
                fetchCountAndShow(title, body, uid);
            } else {
                showNotification(title, body, 0);
            }
        }
    }

    private void fetchCountAndShow(String title, String body, String uid) {
        // 1. Fetch Personal Unread
        FirebaseDatabase.getInstance().getReference("notifications").child(uid)
            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot personalSnapshot) {
                    int pCount = 0;
                    for (com.google.firebase.database.DataSnapshot d : personalSnapshot.getChildren()) {
                        Boolean r = d.child("read").getValue(Boolean.class);
                        if (r != null && !r) pCount++;
                    }

                    final int finalPCount = pCount;
                    // 2. Fetch Broadcast Unread
                    FirebaseDatabase.getInstance().getReference("read_broadcasts").child(uid)
                        .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot readSnapshot) {
                                Set<String> readIds = new HashSet<>();
                                for (com.google.firebase.database.DataSnapshot d : readSnapshot.getChildren()) {
                                    readIds.add(d.getKey());
                                }

                                FirebaseDatabase.getInstance().getReference("broadcast_notifications")
                                    .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot bSnapshot) {
                                            int bCount = 0;
                                            long now = System.currentTimeMillis();
                                            for (com.google.firebase.database.DataSnapshot d : bSnapshot.getChildren()) {
                                                if (readIds.contains(d.getKey())) continue;
                                                Long exp = d.child("expiryDate").getValue(Long.class);
                                                if (exp != null && exp < now) continue;
                                                bCount++;
                                            }
                                            showNotification(title, body, finalPCount + bCount);
                                        }
                                        @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError e) { showNotification(title, body, finalPCount); }
                                    });
                            }
                            @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError e) { showNotification(title, body, finalPCount); }
                        });
                }
                @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError e) { showNotification(title, body, 0); }
            });
    }

    // ✅ Save FCM token... (existing code)
    public static void saveFcmTokenToFirebase(String token) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getUid();
        if (uid == null) return;

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("fcmToken")
                .setValue(token);
    }

    private void showNotification(String title, String message, int count) {
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.app_name) + " Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setShowBadge(true); // 🔥 Important for icon badge
            manager.createNotificationChannel(channel);
        }

        // 🚀 UPDATE LAUNCHER BADGE (ShortcutBadger)
        if (count > 0) {
            me.leolin.shortcutbadger.ShortcutBadger.applyCount(this, count);
        } else {
            me.leolin.shortcutbadger.ShortcutBadger.removeCount(this);
        }

        Intent intent = new Intent(this, NotificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setNumber(count) // 🔥 Tells the OS to show badge number
                        .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
