package com.example.rmplus;

import android.os.Bundle;
import android.widget.*;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class NotificationActivity extends AppCompatActivity {

    ListView notiList;
    LinearLayout newTab, readTab;
    View redDot;

    View tabUnderline;
    ArrayList<String[]> list = new ArrayList<>();
    ArrayList<String> keyList = new ArrayList<>();

    DatabaseReference ref;
    String mode = "new"; // new or read
    TextView txtNew, txtRead;

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

        ref = FirebaseDatabase.getInstance()
                .getReference("notifications")
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

            if (mode.equals("new")) {
                if (pos < keyList.size()) {
                    String key = keyList.get(pos);
                    ref.child(key)
                            .child("read")
                            .setValue(true);
                }
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

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {

                list.clear();
                keyList.clear();

                boolean hasUnread = false;

                for (DataSnapshot d : s.getChildren()) {

                    String title = d.child("title")
                            .getValue(String.class);

                    String message = d.child("message")
                            .getValue(String.class);

                    Boolean read = d.child("read")
                            .getValue(Boolean.class);

                    Long time = d.child("time")
                            .getValue(Long.class);

                    if (title == null)
                        title = "";
                    if (message == null)
                        message = "";
                    if (read == null)
                        read = false;
                    if (time == null)
                        time = 0L;

                    if (!read)
                        hasUnread = true;

                    // FORMAT TIME — Locale.getDefault() OK here (display only)
                    String date = new SimpleDateFormat(
                            "dd MMM, hh:mm a",
                            Locale.getDefault())
                            .format(new Date(time));

                    if (mode.equals("new") && !read) {
                        list.add(new String[] {
                                getLocalized(title),
                                getLocalized(message),
                                date
                        });
                        keyList.add(d.getKey());
                    }

                    if (mode.equals("read") && read) {
                        list.add(new String[] {
                                getLocalized(title),
                                getLocalized(message),
                                date
                        });
                        keyList.add(d.getKey());
                    }
                }

                redDot.setVisibility(
                        hasUnread ? View.VISIBLE : View.GONE);

                // ✅ Localized empty-state strings
                if (list.size() == 0) {
                    list.add(new String[] {
                            getString(R.string.msg_no_notifications),
                            getString(R.string.msg_no_notifications_desc),
                            ""
                    });
                }

                notiList.setAdapter(
                        new NotificationAdapter(
                                NotificationActivity.this,
                                list));
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }

    private String getLocalized(String text) {
        return NotificationHelper.getLocalized(this, text);
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
