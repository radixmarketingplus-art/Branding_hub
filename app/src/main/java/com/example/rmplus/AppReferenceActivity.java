package com.example.rmplus;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AppReferenceActivity extends AppCompatActivity {

    View privacyBtn, termsBtn, rateBtn, supportBtn;
    TextView versionTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_reference);

        privacyBtn = findViewById(R.id.privacyBtn);
        termsBtn = findViewById(R.id.termsBtn);
        rateBtn = findViewById(R.id.rateBtn);
        supportBtn = findViewById(R.id.supportBtn);
        versionTxt = findViewById(R.id.versionTxt);

        versionTxt.setText("Version 1.0");

        privacyBtn.setOnClickListener(v -> {
            Intent i = new Intent(this, WebPageActivity.class);
            i.putExtra("title", "Privacy Policy");
            i.putExtra("url", "https://example.com/privacy");
            startActivity(i);
        });

        termsBtn.setOnClickListener(v -> {
            Intent i = new Intent(this, WebPageActivity.class);
            i.putExtra("title", "Terms & Conditions");
            i.putExtra("url", "https://example.com/terms");
            startActivity(i);
        });

        rateBtn.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=" + getPackageName())));
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
            }
        });

        supportBtn.setOnClickListener(v -> {
            Intent email = new Intent(Intent.ACTION_SENDTO);
            email.setData(Uri.parse("mailto:redixmarketingplus@gmail.com"));
            email.putExtra(Intent.EXTRA_SUBJECT, "RM Plus Support");
            startActivity(email);
        });
    }
}
