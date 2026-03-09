package com.example.rmplus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.net.Uri;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ContactActivity extends BaseActivity {

        Button btnInquiry, btnContactSupport, btnMy, btnAdRequest;

        protected void onCreate(Bundle b) {
                super.onCreate(b);
                setContentView(R.layout.activity_contact);

                SharedPreferences sp = getSharedPreferences("APP_DATA", MODE_PRIVATE);

                String role = sp.getString("role", "user");

                setupBase(role, R.id.contact);

                btnInquiry = findViewById(R.id.btnInquiry);
                btnContactSupport = findViewById(R.id.btnContactSupport);
                btnMy = findViewById(R.id.btnMy);
                btnAdRequest = findViewById(R.id.btnAdRequest);

                findViewById(R.id.btnBack).setOnClickListener(v -> finish());

                ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (view, insets) -> {

                        // ❌ bottom inset consume mat karo
                        view.setPadding(
                                        view.getPaddingLeft(),
                                        view.getPaddingTop(),
                                        view.getPaddingRight(),
                                        0);

                        return insets;
                });

                btnInquiry.setOnClickListener(v -> startActivity(new Intent(this, CreateRequestActivity.class)));
                btnContactSupport.setOnClickListener(v -> showContactSupportDialog());

                btnMy.setOnClickListener(v -> startActivity(new Intent(this, MyRequestsActivity.class)));

                // NEW BUTTON ACTION
                btnAdRequest.setOnClickListener(v -> startActivity(new Intent(this, AdRequestActivity.class)));
        }

        void showContactSupportDialog() {
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_contact_support, null);
                AlertDialog dialog = new AlertDialog.Builder(this)
                                .setView(dialogView)
                                .create();
                                
                android.widget.TextView tvCall = dialogView.findViewById(R.id.tvCallNumber);
                android.widget.TextView tvEmail = dialogView.findViewById(R.id.tvEmailAddress);
                
                // Set defaults and get custom from Firebase
                final String[] finalPhone = {"+917089927270"};
                final String[] finalEmail = {"prikhush332@gmail.com"};
                
                FirebaseDatabase.getInstance().getReference("admin_settings").child("support_contact")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot s) {
                            if(s.exists()) {
                                String dbPhone = s.child("phone").getValue(String.class);
                                String dbEmail = s.child("email").getValue(String.class);
                                if(dbPhone != null && !dbPhone.isEmpty()) {
                                    finalPhone[0] = dbPhone;
                                    tvCall.setText(dbPhone);
                                }
                                if(dbEmail != null && !dbEmail.isEmpty()) {
                                    finalEmail[0] = dbEmail;
                                    tvEmail.setText(dbEmail);
                                }
                            }
                        }
                        @Override public void onCancelled(DatabaseError e) {}
                    });

                dialogView.findViewById(R.id.btnCallSupport).setOnClickListener(v -> {
                        dialog.dismiss();
                        Intent callIntent = new Intent(Intent.ACTION_DIAL);
                        callIntent.setData(Uri.parse("tel:" + finalPhone[0]));
                        startActivity(callIntent);
                });

                dialogView.findViewById(R.id.btnEmailSupport).setOnClickListener(v -> {
                        dialog.dismiss();
                        Intent email = new Intent(Intent.ACTION_SENDTO);
                        email.setData(Uri.parse("mailto:" + finalEmail[0]));
                        email.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.support_email_subject));
                        startActivity(email);
                });

                dialog.show();
        }
}
