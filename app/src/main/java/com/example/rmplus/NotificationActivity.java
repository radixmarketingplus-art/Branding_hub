package com.example.rmplus;

import android.os.Bundle;
import android.widget.*;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class NotificationActivity extends AppCompatActivity {

    ListView notiList;
    LinearLayout newTab, readTab;
    View redDot;

    View tabUnderline;
    ArrayList<NotificationModel> list = new ArrayList<>();
    ArrayList<String> keyList = new ArrayList<>();

    DatabaseReference userRef, broadcastRef, readBroadcastRef;
    String mode = "new"; // new or read
    TextView txtNew, txtRead;
    Set<String> readBroadcastIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_notification);

        notiList = findViewById(R.id.notiList);
        newTab = findViewById(R.id.newTab);
        readTab = findViewById(R.id.readTab);
        redDot = findViewById(R.id.redDot);
        txtNew = findViewById(R.id.txtNew);
        txtRead = findViewById(R.id.txtRead);
        tabUnderline = findViewById(R.id.tabUnderline);

        // SAFETY CHECK
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(FirebaseAuth.getInstance().getUid());

        broadcastRef = FirebaseDatabase.getInstance()
                .getReference("broadcast_notifications");

        readBroadcastRef = FirebaseDatabase.getInstance()
                .getReference("read_broadcasts")
                .child(FirebaseAuth.getInstance().getUid());

        loadNotifications();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        newTab.setOnClickListener(v -> {
            mode = "new";
            updateTabUI();
            loadNotifications();
        });

        readTab.setOnClickListener(v -> {
            mode = "read";
            updateTabUI();
            loadNotifications();
        });

        notiList.setOnItemClickListener((a, v, pos, id) -> {
            if (pos >= list.size()) return;

            NotificationModel model = list.get(pos);

            // 1. Mark as read in Firebase
            if (model.notificationId != null) {
                if (keyList.contains(model.notificationId)) {
                    // It's a personal notification
                    userRef.child(model.notificationId).child("read").setValue(true);
                } else {
                    // It's a broadcast notification
                    readBroadcastRef.child(model.notificationId).setValue(true);
                }
            }

            // 2. Handle redirection link
            if (model.action != null && !model.action.isEmpty()) {
                handleAction(model.action, model.extraData);
            }
        });

        updateTabUI();
        newTab.post(() -> moveUnderline(newTab));
    }

    // --------------------------------------
    void updateTabUI() {

        int activeColor = getColor(R.color.tab_active);
        int inactiveColor = getColor(R.color.tab_inactive);

        if (mode.equals("new")) {

            txtNew.setTypeface(null, android.graphics.Typeface.BOLD);
            txtNew.setTextColor(activeColor);

            txtRead.setTypeface(null, android.graphics.Typeface.NORMAL);
            txtRead.setTextColor(inactiveColor);

            moveUnderline(newTab);

        } else {

            txtRead.setTypeface(null, android.graphics.Typeface.BOLD);
            txtRead.setTextColor(activeColor);

            txtNew.setTypeface(null, android.graphics.Typeface.NORMAL);
            txtNew.setTextColor(inactiveColor);

            moveUnderline(readTab);
        }
    }

    // --------------------------------------

    void loadNotifications() {

        // Listen for list of read broadcast IDs first
        readBroadcastRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot readShot) {
                readBroadcastIds.clear();
                for (DataSnapshot d : readShot.getChildren()) {
                    readBroadcastIds.add(d.getKey());
                }

                // Now load notifications (Personal + Broadcast)
                loadAllNodes();
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    void loadAllNodes() {
        // Fetch personal
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot userShot) {
                // Fetch public
                broadcastRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot broadcastShot) {
                        processNotifications(userShot, broadcastShot);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    void processNotifications(DataSnapshot userShot, DataSnapshot broadcastShot) {
        list.clear();
        keyList.clear();
        ArrayList<NotificationModel> tempAll = new ArrayList<>();

        boolean hasUnread = false;

        // Process Personal
        for (DataSnapshot d : userShot.getChildren()) {
            NotificationModel m = parseNotification(d, false);
            if (m != null) {
                tempAll.add(m);
                if (!m.read) hasUnread = true;
                keyList.add(m.notificationId); // to distinguish personal
            }
        }

        // Process Broadcast
        for (DataSnapshot d : broadcastShot.getChildren()) {
            NotificationModel m = parseNotification(d, true);
            if (m != null) {
                tempAll.add(m);
                if (!m.read) hasUnread = true;
            }
        }

        // Sort by time (Newest First)
        Collections.sort(tempAll, (o1, o2) -> Long.compare(o2.time, o1.time));

        // Filter based on mode (new or read)
        for (NotificationModel m : tempAll) {
            if (mode.equals("new") && !m.read) {
                list.add(m);
            } else if (mode.equals("read") && m.read) {
                list.add(m);
            }
        }

        redDot.setVisibility(hasUnread ? View.VISIBLE : View.GONE);

        if (list.size() == 0) {
            list.add(new NotificationModel(
                    null,
                    getString(R.string.msg_no_notifications),
                    getString(R.string.msg_no_notifications_desc),
                    0,
                    true,
                    null,
                    null,
                    0L
            ));
        }

        notiList.setAdapter(new NotificationAdapter(this, list));
    }

    private NotificationModel parseNotification(DataSnapshot d, boolean isBroadcast) {
        String title = d.child("title").getValue(String.class);
        String message = d.child("message").getValue(String.class);
        Long time = d.child("time").getValue(Long.class);
        String action = d.child("action").getValue(String.class);
        String extraData = d.child("extraData").getValue(String.class);
        Long expiry = d.child("expiryDate").getValue(Long.class);

        if (title == null) title = "";
        if (message == null) message = "";
        if (time == null) time = 0L;
        if (expiry == null) expiry = 0L;

        // ⏰ EXPIRY CHECK: If notification has expired, don't show it
        if (expiry > 0 && expiry < System.currentTimeMillis()) {
            return null;
        }

        boolean read;
        if (isBroadcast) {
            read = readBroadcastIds.contains(d.getKey());
        } else {
            Boolean r = d.child("read").getValue(Boolean.class);
            read = (r != null && r);
        }

        return new NotificationModel(
                d.getKey(),
                getLocalized(title),
                getLocalized(message),
                time,
                read,
                action,
                extraData,
                expiry
        );
    }

    private String getLocalized(String text) {
        return NotificationHelper.getLocalized(this, text);
    }

    private void handleAction(String action, String extraData) {
        if (action == null || action.isEmpty()) return;

        try {
            if (action.equals("WEB_LINK")) {
                if (extraData != null && !extraData.isEmpty()) {
                    String finalLink = extraData.trim();
                    if (!finalLink.startsWith("http://") && !finalLink.startsWith("https://")) {
                        finalLink = "https://" + finalLink;
                    }
                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(finalLink));
                    startActivity(intent);
                }
            } else if (action.equals("OPEN_TEMPLATE")) {
                if (extraData != null && !extraData.isEmpty()) {
                    android.content.Intent intent = new android.content.Intent(this, HomeActivity.class);
                    intent.putExtra("target_template_id", extraData);
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            } else if (action.equals("OPEN_AD")) {
                if (extraData != null && !extraData.isEmpty()) {
                    android.content.Intent intent = new android.content.Intent(this, HomeActivity.class);
                    intent.putExtra("target_ad_id", extraData);
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            } else if (action.equals("OPEN_PROFILE")) {
                android.content.Intent intent = new android.content.Intent(this, ProfileActivity.class);
                startActivity(intent);
                finish();
            } else if (action.equals("OPEN_SUBSCRIPTION_REQUESTS")) {
                android.content.Intent intent = new android.content.Intent(this, SubscriptionRequestsActivity.class);
                startActivity(intent);
                finish();
            } else if (action.equals("OPEN_AD_REQUESTS")) {
                android.content.Intent intent = new android.content.Intent(this, AdminAdvertisementRequestsActivity.class);
                startActivity(intent);
                finish();
            } else if (action.equals("OPEN_SUPPORT_REQUESTS")) {
                android.content.Intent intent = new android.content.Intent(this, AdminContactRequestsActivity.class);
                startActivity(intent);
                finish();
            } else if (action.equals("OPEN_CHAT")) {
                if (extraData != null && !extraData.isEmpty()) {
                    android.content.Intent intent = new android.content.Intent(this, RequestChatActivity.class);
                    intent.putExtra("id", extraData);
                    // For chat, we might need more data like title, but let's stick to id for now if that's enough
                    startActivity(intent);
                    finish();
                }
            } else if (action.equals("OPEN_ACTIVITY")) {
                // Future use: Open specific activity based on extraData class name
                // Class<?> cls = Class.forName("com.example.rmplus." + extraData);
                // startActivity(new Intent(this, cls));
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.widget.Toast.makeText(this, "Could not open link", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void moveUnderline(View tab) {
        tab.post(() -> {

            int tabWidth = tab.getWidth();
            int tabLeft = tab.getLeft();

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) tabUnderline.getLayoutParams();

            params.width = tabWidth;
            params.leftMargin = tabLeft;

            tabUnderline.setLayoutParams(params);

            tabUnderline.animate()
                    .x(tabLeft)
                    .setDuration(200)
                    .start();
        });
    }
}
