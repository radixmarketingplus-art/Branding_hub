package com.example.rmplus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

public class AdminPanelActivity extends BaseActivity {

    Button uploadTemplatesBtn,
            subscriptionRequestsBtn,
            advertisementRequestBtn,
            contactRequestsBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);

        SharedPreferences sp =
                getSharedPreferences("APP_DATA", MODE_PRIVATE);

        String role = sp.getString("role", "user");

        setupBase(role, R.id.admin);

        uploadTemplatesBtn = findViewById(R.id.uploadTemplatesBtn);
        subscriptionRequestsBtn = findViewById(R.id.subscriptionRequestsBtn);
        advertisementRequestBtn = findViewById(R.id.advertisementRequestBtn);
        contactRequestsBtn = findViewById(R.id.contactRequestsBtn);

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

        uploadTemplatesBtn.setOnClickListener(v ->
                startActivity(new Intent(this,
                        UploadManagerActivity.class)));

        subscriptionRequestsBtn.setOnClickListener(v ->
                startActivity(new Intent(this,
                        SubscriptionRequestsActivity.class)));

        advertisementRequestBtn.setOnClickListener(v ->
                startActivity(new Intent(this,
                        AdminAdvertisementRequestsActivity.class)));

        contactRequestsBtn.setOnClickListener(v ->
                startActivity(new Intent(this,
                        AdminContactRequestsActivity.class)));
    }
}
