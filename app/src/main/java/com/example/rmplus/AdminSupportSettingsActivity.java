package com.example.rmplus;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminSupportSettingsActivity extends AppCompatActivity {

    EditText etPhone, etEmail;
    DatabaseReference ref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_support_settings);

        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        ref = FirebaseDatabase.getInstance().getReference("admin_settings").child("support_contact");

        // Load existing data
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    String phone = snapshot.child("phone").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    if(phone != null) etPhone.setText(phone);
                    if(email != null) etEmail.setText(email);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });

        // Save data
        findViewById(R.id.btnSave).setOnClickListener(v -> {
            String phone = etPhone.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            
            if(phone.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Please fill in all details", Toast.LENGTH_SHORT).show();
                return;
            }
            
            ref.child("phone").setValue(phone);
            ref.child("email").setValue(email)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Support settings saved!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        });
    }
}
