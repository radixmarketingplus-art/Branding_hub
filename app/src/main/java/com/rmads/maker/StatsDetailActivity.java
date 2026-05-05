package com.rmads.maker;

import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rmads.maker.adapters.StatUserAdapter;
import com.rmads.maker.models.StatUserItem;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class StatsDetailActivity extends BaseActivity {

    RecyclerView recycler;
    StatUserAdapter adapter;
    ArrayList<StatUserItem> list = new ArrayList<>();

    TextView tabLike, tabEdit, tabSave, tabShare;

    String templateKey;
    String currentType = "likes";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_stats_detail);

        String rawPath = getIntent().getStringExtra("path");
        String passedId = getIntent().getStringExtra("id");
        
        if (passedId != null && !passedId.isEmpty()) {
            templateKey = passedId;
        } else {
            templateKey = encodeKey(rawPath);
        }
        String defaultTab = getIntent().getStringExtra("defaultTab");

        recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StatUserAdapter(list);
        recycler.setAdapter(adapter);

        tabLike = findViewById(R.id.tabLike);
        tabEdit = findViewById(R.id.tabEdit);
        tabSave = findViewById(R.id.tabSave);
        tabShare = findViewById(R.id.tabShare);

        tabLike.setOnClickListener(v -> switchTab("likes"));

        tabEdit.setOnClickListener(v -> switchTab("edits"));
        tabSave.setOnClickListener(v -> switchTab("saves"));
        tabShare.setOnClickListener(v -> switchTab("shares"));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        // default tab
        if (defaultTab != null) {
            switchTab(defaultTab);
        } else {
            switchTab("likes");
        }
    }

    // ---------------- TAB SWITCH ----------------

    void switchTab(String type) {
        currentType = type;
        updateUI();

        list.clear();
        adapter.notifyDataSetChanged();

        loadData();
    }


    void updateUI() {
        reset(tabLike);
        reset(tabEdit);
        reset(tabSave);
        reset(tabShare);

        if (currentType.equals("likes")) select(tabLike);

        else if (currentType.equals("edits")) select(tabEdit);
        else if (currentType.equals("saves")) select(tabSave);
        else if (currentType.equals("shares")) select(tabShare);
    }

    void select(TextView t) {
        t.setTypeface(null, Typeface.BOLD);
        t.setTextColor(getColor(android.R.color.white));
        t.setBackgroundResource(R.drawable.bg_tab_active);
    }

    void reset(TextView t) {
        t.setTypeface(null, Typeface.NORMAL);
        t.setTextColor(getColor(R.color.text_secondary));
        t.setBackground(null);
    }

    // ---------------- DATA ----------------

    void loadData() {

        DatabaseReference ref =
                FirebaseDatabase.getInstance()
                        .getReference("template_activity")
                        .child(templateKey)
                        .child(currentType);

        ref.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot s) {

                        list.clear();

                        for (DataSnapshot d : s.getChildren()) {
                            String uid = d.getKey();
                            if (uid != null) {
                                Object val = d.getValue();
                                long time = 0;
                                if (val instanceof Long) time = (Long) val;
                                else if (val instanceof Integer) time = ((Integer) val).longValue();
                                
                                loadUser(uid, time);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError e) {}
                });
    }

    void loadUser(String uid, long engagementTime) {

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot s) {

                                StatUserItem item = new StatUserItem();
                                item.uid = uid;
                                item.name = s.child("name").getValue(String.class);
                                item.email = s.child("email").getValue(String.class);

                                item.time = engagementTime;

                                list.add(item);
                                adapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onCancelled(DatabaseError e) {}
                        });
    }

    private String encodeKey(String raw) {
        return android.util.Base64.encodeToString(
                raw.getBytes(),
                android.util.Base64.NO_WRAP
        );
    }

}
