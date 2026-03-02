package com.example.rmplus;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class UploadManagerActivity extends BaseActivity {

    LinearLayout tabUpload, tabCheck;
    TextView txtUpload, txtCheck;
    android.view.View selectionIndicator;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_upload_manager);

        tabUpload = findViewById(R.id.tabUpload);
        tabCheck = findViewById(R.id.tabCheck);
        txtUpload = findViewById(R.id.txtUpload);
        txtCheck = findViewById(R.id.txtCheck);
        selectionIndicator = findViewById(R.id.selectionIndicator);

        // Correctly size the indicator
        selectionIndicator.post(() -> {
            android.view.ViewGroup.LayoutParams lp = selectionIndicator.getLayoutParams();
            lp.width = tabUpload.getWidth();
            selectionIndicator.setLayoutParams(lp);
        });

        // default
        switchTab(true);

        tabUpload.setOnClickListener(v -> switchTab(true));
        tabCheck.setOnClickListener(v -> switchTab(false));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    void switchTab(boolean upload) {

        int activeText = android.graphics.Color.WHITE;
        int inactiveText = getColorFromAttr(com.google.android.material.R.attr.colorOnSurfaceVariant);

        float targetX = upload ? 0 : tabUpload.getWidth();

        // SLIDING ANIMATION
        selectionIndicator.animate()
                .translationX(targetX)
                .setDuration(300)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        if (upload) {
            txtUpload.setTypeface(null, Typeface.BOLD);
            txtUpload.setTextColor(activeText);

            txtCheck.setTypeface(null, Typeface.NORMAL);
            txtCheck.setTextColor(inactiveText);

            loadFragment(new UploadTemplatesFragment());
        } else {
            txtCheck.setTypeface(null, Typeface.BOLD);
            txtCheck.setTextColor(activeText);

            txtUpload.setTypeface(null, Typeface.NORMAL);
            txtUpload.setTextColor(inactiveText);

            loadFragment(new CheckUploadsFragment());
        }
    }

    void loadFragment(Fragment f) {
        // Pass intent extras to the fragment if editing
        if (f instanceof UploadTemplatesFragment) {
            Bundle args = new Bundle();
            if (getIntent().hasExtra("edit_url")) {
                args.putString("edit_url", getIntent().getStringExtra("edit_url"));
                args.putString("category", getIntent().getStringExtra("category"));
                args.putString("date", getIntent().getStringExtra("date"));
                args.putString("link", getIntent().getStringExtra("link"));
                args.putLong("expiryDate", getIntent().getLongExtra("expiryDate", 0));
                args.putString("realId", getIntent().getStringExtra("realId"));
            }
            f.setArguments(args);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, f)
                .commit();
    }

    private int getColorFromAttr(int attr) {
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}