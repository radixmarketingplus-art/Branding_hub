package com.example.rmplus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.TypedValue;
import android.view.View;
import android.widget.*;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class MyDesignActivity extends BaseActivity {

    RecyclerView recycler;
    TextView txtTitle;

    TextView txtLikes, txtFav, txtEdits, txtSaved;
    View tabUnderline;

    TemplateGridAdapter adapter;
    ArrayList<String> list = new ArrayList<>();

    String currentType = "likes";
    String uid;

    DatabaseReference rootRef;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_my_design);

        SharedPreferences sp =
                getSharedPreferences("APP_DATA", MODE_PRIVATE);

        String role = sp.getString("role", "user");

        setupBase(role, R.id.myDesign);

        txtTitle = findViewById(R.id.txtTitle);
        recycler = findViewById(R.id.recycler);

        txtLikes = findViewById(R.id.txtLikes);
        txtFav   = findViewById(R.id.txtFav);
        txtEdits = findViewById(R.id.txtEdits);
        txtSaved = findViewById(R.id.txtSaved);

        tabUnderline = findViewById(R.id.tabUnderline);

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (view, insets) -> {

            // ❌ bottom inset consume mat karo
            view.setPadding(
                    view.getPaddingLeft(),
                    view.getPaddingTop(),
                    view.getPaddingRight(),
                    0
            );

            return insets;
        });

        recycler.setLayoutManager(new GridLayoutManager(this, 2));

        adapter = new TemplateGridAdapter(list, path -> {
            Intent i = new Intent(this, TemplatePreviewActivity.class);
            i.putExtra("path", path);
            i.putExtra("category", "MyDesign");
            startActivity(i);
        });

        recycler.setAdapter(adapter);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        uid = FirebaseAuth.getInstance().getUid();
        rootRef = FirebaseDatabase.getInstance().getReference();

        // Default underline position
        txtLikes.post(() -> {
            moveUnderline(txtLikes);
            updateTabUI();
        });

        loadTemplates();
        setTitle();

        txtLikes.setOnClickListener(v -> switchTab("likes", txtLikes));
        txtFav.setOnClickListener(v -> switchTab("favorites", txtFav));
        txtEdits.setOnClickListener(v -> switchTab("edits", txtEdits));
        txtSaved.setOnClickListener(v -> switchTab("saves", txtSaved));
    }

    /* ---------------- TAB SWITCH ---------------- */

    void switchTab(String type, TextView tab) {
        currentType = type;
        moveUnderline(tab);
        updateTabUI();
        setTitle();
        loadTemplates();
    }

    void moveUnderline(View tab) {
        tab.post(() -> {
            int left = tab.getLeft();
            int width = tab.getWidth();

            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) tabUnderline.getLayoutParams();

            params.width = width;
            params.leftMargin = left;
            tabUnderline.setLayoutParams(params);
        });
    }

    void updateTabUI() {
        int inactive = getColorFromAttr(
                com.google.android.material.R.attr.colorOnSurface);

        txtLikes.setTextColor(inactive);
        txtFav.setTextColor(inactive);
        txtEdits.setTextColor(inactive);
        txtSaved.setTextColor(inactive);

        switch (currentType) {
            case "likes": txtLikes.setTextColor(0xFF4A6CF7); break;
            case "favorites": txtFav.setTextColor(0xFF4A6CF7); break;
            case "edits": txtEdits.setTextColor(0xFF4A6CF7); break;
            case "saves": txtSaved.setTextColor(0xFF4A6CF7); break;
        }
    }

    /* ---------------- DATA ---------------- */

    void setTitle() {
        switch (currentType) {
            case "likes": txtTitle.setText("My Likes"); break;
            case "favorites": txtTitle.setText("My Favorite"); break;
            case "edits": txtTitle.setText("My Edits"); break;
            case "saves": txtTitle.setText("My Saved Designs"); break;
        }
    }

    void loadTemplates() {
        rootRef.child("template_activity")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        list.clear();

                        for (DataSnapshot snap : snapshot.getChildren()) {
                            String key = snap.getKey();
                            if (key == null) continue;

                            if (snap.child(currentType).child(uid).exists()) {
                                String path = decodeKey(key);
                                if (path != null) list.add(path);
                            }
                        }
                        adapter.setData(list);
                    }

                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    String decodeKey(String key) {
        try {
            return new String(Base64.decode(key, Base64.NO_WRAP));
        } catch (Exception e) {
            return null;
        }
    }

    private int getColorFromAttr(int attr) {
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}