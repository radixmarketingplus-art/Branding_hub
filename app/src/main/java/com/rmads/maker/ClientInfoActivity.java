package com.rmads.maker;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import androidx.annotation.NonNull;
import com.rmads.maker.models.ClientInfoRequest;

public class ClientInfoActivity extends BaseActivity {

    EditText etName, etContact, etEmail, etBusinessName, etBusinessCategory;
    EditText etDescription, etRemark;
    Spinner spinnerService;
    Button btnSubmitInfo;
    ProgressBar progressBar;
    java.util.List<com.rmads.maker.models.DynamicService> dynamicServices = new java.util.ArrayList<>();

    DatabaseReference clientInfoRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_info);

        setupBase("user", -1);

        etName = findViewById(R.id.etName);
        etContact = findViewById(R.id.etContact);
        etEmail = findViewById(R.id.etEmail);
        etBusinessName = findViewById(R.id.etBusinessName);
        etBusinessCategory = findViewById(R.id.etBusinessCategory);
        etDescription = findViewById(R.id.etDescription);
        etRemark = findViewById(R.id.etRemark);
        spinnerService = findViewById(R.id.spinnerService);
        btnSubmitInfo = findViewById(R.id.btnSubmitInfo);
        progressBar = findViewById(R.id.progressBar);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        clientInfoRef = FirebaseDatabase.getInstance().getReference("client_info_requests");
        loadDynamicServices();

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseDatabase.getInstance().getReference("users").child(uid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot s) {
                            if (s.exists()) {
                                String name = s.child("name").getValue(String.class);
                                String email = s.child("email").getValue(String.class);
                                String mobile = s.child("mobile").getValue(String.class);
                                
                                if (name != null && etName.getText().toString().isEmpty()) etName.setText(name);
                                if (email != null && etEmail.getText().toString().isEmpty()) etEmail.setText(email);
                                if (mobile != null && etContact.getText().toString().isEmpty()) etContact.setText(mobile);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError e) {}
                    });
        }

        btnSubmitInfo.setOnClickListener(v -> submitClientInfo());
    }

    private void loadDynamicServices() {
        FirebaseDatabase.getInstance().getReference("client_info_services")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        dynamicServices.clear();
                        java.util.List<String> displayList = new java.util.ArrayList<>();
                        
                        // Get current app language from configuration
                        String currentLang = getResources().getConfiguration().getLocales().get(0).getLanguage();
                        
                        for (DataSnapshot d : snapshot.getChildren()) {
                            com.rmads.maker.models.DynamicService s = d.getValue(com.rmads.maker.models.DynamicService.class);
                            if (s != null) {
                                dynamicServices.add(s);
                                // Show Hindi if Hindi is selected, otherwise English
                                displayList.add(currentLang.equals("hi") ? s.hi : s.en);
                            }
                        }
                        
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(ClientInfoActivity.this, 
                                android.R.layout.simple_spinner_item, displayList);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerService.setAdapter(adapter);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void submitClientInfo() {
        String name = etName.getText().toString().trim();
        String contact = etContact.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String businessName = etBusinessName.getText().toString().trim();
        String businessCategory = etBusinessCategory.getText().toString().trim();
        
        if (name.isEmpty() || contact.isEmpty() || email.isEmpty() || businessName.isEmpty() || businessCategory.isEmpty()) {
            Toast.makeText(this, R.string.msg_please_fill_details, Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedIndex = spinnerService.getSelectedItemPosition();
        if (selectedIndex < 0 || selectedIndex >= dynamicServices.size()) {
             Toast.makeText(this, "Please select a service", Toast.LENGTH_SHORT).show();
             return;
        }

        setSubmitting(true);

        String uid = FirebaseAuth.getInstance().getUid();
        String id = clientInfoRef.push().getKey();

        ClientInfoRequest req = new ClientInfoRequest();
        req.requestId = id;
        req.uid = uid;
        req.name = name;
        req.contact = contact;
        req.email = email;
        req.businessName = businessName;
        req.businessCategory = businessCategory;
        
        // Save English version to DB for Admin consistency
        req.serviceType = dynamicServices.get(selectedIndex).en;
        
        req.description = etDescription.getText().toString().trim();
        req.remark = etRemark.getText().toString().trim();
        req.status = "pending";
        req.time = System.currentTimeMillis();

        clientInfoRef.child(id).setValue(req)
                .addOnSuccessListener(aVoid -> {
                    
                    // Notify Admin
                     NotificationHelper.notifyAdmins(
                                ClientInfoActivity.this,
                                "New Client Info Request",
                                "A user has submitted a new client info request.",
                                "OPEN_CLIENT_INFO_REQUESTS", 
                                id);
                                
                    setSubmitting(false);
                    Toast.makeText(ClientInfoActivity.this, R.string.msg_client_info_sent, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setSubmitting(false);
                    Toast.makeText(ClientInfoActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setSubmitting(boolean submitting) {
        btnSubmitInfo.setEnabled(!submitting);
        btnSubmitInfo.setAlpha(submitting ? 0.6f : 1.0f);
        progressBar.setVisibility(submitting ? View.VISIBLE : View.GONE);
    }
}
