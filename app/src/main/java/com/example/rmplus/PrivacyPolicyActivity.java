package com.example.rmplus;

import android.os.Bundle;
import android.widget.TextView; // Added import for TextView
import androidx.appcompat.app.AppCompatActivity;

public class PrivacyPolicyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}
