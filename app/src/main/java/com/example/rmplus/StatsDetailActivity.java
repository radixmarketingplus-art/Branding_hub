package com.example.rmplus;

import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rmplus.adapters.StatUserAdapter;
import com.example.rmplus.models.StatUserItem;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class StatsDetailActivity extends AppCompatActivity {

    RecyclerView recycler;
    StatUserAdapter adapter;
    ArrayList<StatUserItem> list = new ArrayList<>();

    TextView tabLike, tabFav, tabEdit, tabSave;

    String templateKey;
    String currentType = "likes";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_stats_detail);

        String rawPath = getIntent().getStringExtra("path");
        templateKey = encodeKey(rawPath);
        String defaultTab = getIntent().getStringExtra("defaultTab");

        recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StatUserAdapter(list);
        recycler.setAdapter(adapter);

        tabLike = findViewById(R.id.tabLike);
        tabFav  = findViewById(R.id.tabFav);
        tabEdit = findViewById(R.id.tabEdit);
        tabSave = findViewById(R.id.tabSave);

        tabLike.setOnClickListener(v -> switchTab("likes"));
        tabFav.setOnClickListener(v -> switchTab("favorites"));
        tabEdit.setOnClickListener(v -> switchTab("edits"));
        tabSave.setOnClickListener(v -> switchTab("saves"));

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
        reset(tabFav);
        reset(tabEdit);
        reset(tabSave);

        if (currentType.equals("likes")) select(tabLike);
        else if (currentType.equals("favorites")) select(tabFav);
        else if (currentType.equals("edits")) select(tabEdit);
        else if (currentType.equals("saves")) select(tabSave);
    }

    void select(TextView t) {
        t.setTypeface(null, Typeface.BOLD);
        t.setTextColor(getColor(android.R.color.white));
    }

    void reset(TextView t) {
        t.setTypeface(null, Typeface.NORMAL);
        t.setTextColor(0x80FFFFFF);
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
                            if (uid != null) loadUser(uid);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError e) {}
                });
    }

    void loadUser(String uid) {

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

                                Long t = s.child("lastLogin").getValue(Long.class);
                                item.time = t == null ? 0 : t;

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
