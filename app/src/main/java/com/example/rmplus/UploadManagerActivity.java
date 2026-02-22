package com.example.rmplus;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class UploadManagerActivity extends AppCompatActivity {

    LinearLayout tabUpload, tabCheck;
    TextView txtUpload, txtCheck;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_upload_manager);

        tabUpload = findViewById(R.id.tabUpload);
        tabCheck  = findViewById(R.id.tabCheck);
        txtUpload = findViewById(R.id.txtUpload);
        txtCheck  = findViewById(R.id.txtCheck);

        // default
        switchTab(true);

        tabUpload.setOnClickListener(v -> switchTab(true));
        tabCheck.setOnClickListener(v -> switchTab(false));
    }

    void switchTab(boolean upload) {

        int activeText   = getColorFromAttr(
                com.google.android.material.R.attr.colorOnPrimary
        );
        int inactiveText = getColorFromAttr(
                com.google.android.material.R.attr.colorOnSurface
        );

        if (upload) {

            // TEXT
            txtUpload.setTypeface(null, Typeface.BOLD);
            txtUpload.setTextColor(activeText);

            txtCheck.setTypeface(null, Typeface.NORMAL);
            txtCheck.setTextColor(inactiveText);

            // ✅ BACKGROUND (THIS WAS MISSING)
            tabUpload.setBackgroundResource(R.drawable.bg_tab_active);
            tabCheck.setBackground(null);

            loadFragment(new UploadTemplatesFragment());

        } else {

            // TEXT
            txtCheck.setTypeface(null, Typeface.BOLD);
            txtCheck.setTextColor(activeText);

            txtUpload.setTypeface(null, Typeface.NORMAL);
            txtUpload.setTextColor(inactiveText);

            // ✅ BACKGROUND (THIS WAS MISSING)
            tabCheck.setBackgroundResource(R.drawable.bg_tab_active);
            tabUpload.setBackground(null);

            loadFragment(new CheckUploadsFragment());
        }
    }

    void loadFragment(Fragment f) {
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