package com.example.rmplus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

public class ContactActivity extends BaseActivity {

    Button btnContact, btnMy, btnAdRequest;

    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_contact);

        SharedPreferences sp =
                getSharedPreferences("APP_DATA", MODE_PRIVATE);

        String role = sp.getString("role", "user");

        setupBase(role, R.id.contact);

        btnContact = findViewById(R.id.btnContact);
        btnMy = findViewById(R.id.btnMy);
        btnAdRequest = findViewById(R.id.btnAdRequest);

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

        btnContact.setOnClickListener(v ->
                startActivity(new Intent(this, CreateRequestActivity.class)));

        btnMy.setOnClickListener(v ->
                startActivity(new Intent(this, MyRequestsActivity.class)));

        // NEW BUTTON ACTION
        btnAdRequest.setOnClickListener(v ->
                startActivity(new Intent(this, AdRequestActivity.class)));
    }
}
