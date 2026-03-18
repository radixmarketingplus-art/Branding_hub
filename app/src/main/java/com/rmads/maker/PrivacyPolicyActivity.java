package com.rmads.maker;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class PrivacyPolicyActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Apply animation to content
        View content = findViewById(android.R.id.content);
        content.setAlpha(0f);
        content.animate().alpha(1f).setDuration(500).start();
    }
}
