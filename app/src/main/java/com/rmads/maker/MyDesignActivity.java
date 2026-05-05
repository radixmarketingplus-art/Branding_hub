package com.rmads.maker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.TypedValue;
import android.view.View;
import android.widget.*;
import android.widget.TextView;

import androidx.annotation.NonNull;
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

    TextView txtLikes, txtEdits, txtSaved;
    View tabUnderline;
    ProgressBar progress;

    TemplateGridAdapter adapter;
    ArrayList<TemplateModel> list = new ArrayList<>();

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

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        txtTitle = findViewById(R.id.txtTitle);
        recycler = findViewById(R.id.recycler);

        txtLikes = findViewById(R.id.txtLikes);
        txtEdits = findViewById(R.id.txtEdits);
        txtSaved = findViewById(R.id.txtSaved);

        tabUnderline = findViewById(R.id.tabUnderline);
        progress = findViewById(R.id.progress);

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

        recycler.setLayoutManager(new GridLayoutManager(this, 3));

        adapter = new TemplateGridAdapter(list, R.layout.item_grid_square, t -> {
            if ("edits".equals(currentType)) {
                // Open Editor Directly for Drafts/Edits
                Intent i = new Intent(this, ManageTemplatesActivity.class);
                i.putExtra("id", t.id);
                i.putExtra("uri", t.url);
                i.putExtra("category", t.category);
                i.putExtra("isVideo", "VIDEO".equalsIgnoreCase(t.type));
                startActivity(i);
            } else {
                Intent i = new Intent(this, TemplatePreviewActivity.class);
                i.putExtra("id", t.id);
                i.putExtra("path", t.url);
                i.putExtra("category", t.category);
                startActivity(i);
            }
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
        txtEdits.setTextColor(inactive);
        txtSaved.setTextColor(inactive);

        switch (currentType) {
            case "likes": txtLikes.setTextColor(0xFF4A6CF7); break;

            case "edits": txtEdits.setTextColor(0xFF4A6CF7); break;
            case "saves": txtSaved.setTextColor(0xFF4A6CF7); break;
        }
    }

    /* ---------------- DATA ---------------- */

    void setTitle() {
        switch (currentType) {
            case "likes": txtTitle.setText(R.string.title_my_likes); break;

            case "edits": txtTitle.setText(R.string.title_my_edits); break;
            case "saves": txtTitle.setText(R.string.title_my_saved); break;
        }
    }

    void loadTemplates() {
        if (progress != null) progress.setVisibility(View.VISIBLE);

        // 🟢 STEP 1: LOAD INSTANTLY (Very Fast)
        rootRef.child("user_activity").child(uid).child(currentType)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String templateId = snap.getKey();
                    String url = null;
                    
                    Object val = snap.getValue();
                    if (val instanceof String) {
                        url = (String) val;
                    } else if (val instanceof java.util.Map) {
                        url = (String) ((java.util.Map) val).get("url");
                    }
                    
                    if (templateId != null && url != null) {
                        String type = "IMAGE";
                        String lowUrl = url.toLowerCase();
                        if (lowUrl.endsWith(".mp4") || lowUrl.endsWith(".mkv") || lowUrl.endsWith(".mov")) {
                            type = "VIDEO";
                        }
                        list.add(new TemplateModel(templateId, url, "MyDesign", null, type));
                    }
                }
                
                // Show items immediately
                adapter.setData(list);
                if (progress != null) progress.setVisibility(View.GONE);

                // 🟢 STEP 2: BACKGROUND CLEANUP
                validateAndClean(new ArrayList<>(list));
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                if (progress != null) progress.setVisibility(View.GONE);
            }
        });
    }

    private void validateAndClean(ArrayList<TemplateModel> currentList) {
        DatabaseReference templatesRef = rootRef.child("templates");
        DatabaseReference userRef = rootRef.child("user_activity").child(uid).child(currentType);

        for (TemplateModel model : currentList) {
            // Check each ID asynchronously in the background
            rootRef.child("template_activity").child(model.id).get().addOnSuccessListener(snapshot -> {
                if (!snapshot.exists()) {
                    // 🧹 Delete silently from UI and DB if missing
                    userRef.child(model.id).removeValue();
                    list.removeIf(item -> item.id.equals(model.id));
                    adapter.notifyDataSetChanged();
                }
            });
        }
    }

    private int getColorFromAttr(int attr) {
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}
