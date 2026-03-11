package com.example.rmplus;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.ArrayList;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.rmplus.adapters.UserSubscriptionPlanAdapter;
import com.example.rmplus.models.SubscriptionPlan;
import com.example.rmplus.NotificationHelper;

public class ApproveRejectActivity extends BaseActivity {

        TextView nameTxt, emailTxt, mobileTxt, planTxt, dateTxt;
        ImageView proofImage;
        Button approveBtn, rejectBtn, downloadBtn;

        DatabaseReference requestRef, userRef;
        String uid;
        String proofPath = "";
        RecyclerView plansRecycler;
        UserSubscriptionPlanAdapter planAdapter;
        String userRequestedPlan = "";
        long currentExpiry = 0;
        Button btnUpdateExpiry;
        View planGrantContainer;
        TextView txtCurrentExpiry;

        ArrayList<SubscriptionPlan> dynamicPlans = new ArrayList<>();
        DatabaseReference dynamicPlansRef;

        @Override
        protected void onCreate(Bundle b) {
                super.onCreate(b);
                setContentView(R.layout.activity_approve_reject);

                nameTxt = findViewById(R.id.nameTxt);
                emailTxt = findViewById(R.id.emailTxt);
                mobileTxt = findViewById(R.id.mobileTxt);
                planTxt = findViewById(R.id.planTxt);
                dateTxt = findViewById(R.id.dateTxt);
                proofImage = findViewById(R.id.proofImage);
                approveBtn = findViewById(R.id.approveBtn);
                rejectBtn = findViewById(R.id.rejectBtn);
                downloadBtn = findViewById(R.id.downloadBtn);
                plansRecycler = findViewById(R.id.plansRecycler);
                btnUpdateExpiry = findViewById(R.id.btnUpdateExpiry);
                planGrantContainer = findViewById(R.id.planGrantContainer);
                txtCurrentExpiry = findViewById(R.id.txtCurrentExpiry);

                plansRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

                findViewById(R.id.btnBack).setOnClickListener(v -> finish());

                uid = getIntent().getStringExtra("uid");

                requestRef = FirebaseDatabase.getInstance()
                                .getReference("subscription_requests")
                                .child(uid);

                userRef = FirebaseDatabase.getInstance()
                                .getReference("users")
                                .child(uid);

                dynamicPlansRef = FirebaseDatabase.getInstance()
                                .getReference("dynamic_subscriptions");

                loadDynamicPlans();
                loadData();

                approveBtn.setOnClickListener(v -> approve());
                rejectBtn.setOnClickListener(v -> reject());
                downloadBtn.setOnClickListener(v -> saveToGallery());

                if (btnUpdateExpiry != null) {
                        btnUpdateExpiry.setOnClickListener(v -> pickNewExpiry());
                }
        }

        // ---------------------------

        void loadData() {

                requestRef.addListenerForSingleValueEvent(
                                new ValueEventListener() {
                                        public void onDataChange(DataSnapshot s) {

                                                userRequestedPlan = s.child("plan").getValue(String.class);
                                                String amt = s.child("amount").getValue(String.class);
                                                String disc = s.child("discountPrice").getValue(String.class);

                                                String localizedPlan = getLocalizedPlanName(userRequestedPlan);
                                                String fullPlanDetails = localizedPlan;
                                                if (amt != null)
                                                        fullPlanDetails += "\n" + getString(R.string.label_amount_format, amt);
                                                if (disc != null && !disc.equals("0"))
                                                        fullPlanDetails += " " + getString(R.string.label_disc_format, disc);

                                                planTxt.setText(fullPlanDetails);

                                                // Load User Info from request node
                                                String name = s.child("name").getValue(String.class);
                                                String email = s.child("email").getValue(String.class);
                                                String mobile = s.child("mobile").getValue(String.class);

                                                nameTxt.setText(name != null ? name : getString(R.string.placeholder_name));
                                                emailTxt.setText(email != null ? email : getString(R.string.placeholder_email));
                                                mobileTxt.setText(mobile != null ? mobile : getString(R.string.hint_phone_number));

                                                ((TextView) findViewById(R.id.txtHeaderTitle))
                                                                .setText(name != null ? name : getString(R.string.menu_subscription));

                                                String status = s.child("status").getValue(String.class);

                                                checkAndSelectPlannedItem();

                                                TextView txtStatus = findViewById(R.id.txtStatus);
                                                int statusColor = android.graphics.Color.parseColor("#F59E0B"); // Pending

                                                if ("approved".equalsIgnoreCase(status)) {
                                                        txtStatus.setText(getString(R.string.tab_accepted));
                                                        statusColor = android.graphics.Color.parseColor("#10B981");
                                                        approveBtn.setVisibility(View.GONE);
                                                        rejectBtn.setVisibility(View.GONE);
                                                        if (planGrantContainer != null)
                                                                planGrantContainer.setVisibility(View.GONE);
                                                        loadExpiryFromUser();
                                                } else if ("pending".equalsIgnoreCase(status)) {
                                                        txtStatus.setText(getString(R.string.tab_pending));
                                                        statusColor = android.graphics.Color.parseColor("#F59E0B");
                                                        approveBtn.setVisibility(View.VISIBLE);
                                                        rejectBtn.setVisibility(View.VISIBLE);
                                                        if (planGrantContainer != null)
                                                                planGrantContainer.setVisibility(View.VISIBLE);
                                                } else if ("rejected".equalsIgnoreCase(status)) {
                                                        txtStatus.setText(getString(R.string.tab_rejected));
                                                        statusColor = android.graphics.Color.parseColor("#EF4444");
                                                        approveBtn.setVisibility(View.GONE);
                                                        rejectBtn.setVisibility(View.GONE);
                                                        if (planGrantContainer != null)
                                                                planGrantContainer.setVisibility(View.GONE);
                                                }
                                                txtStatus.setTextColor(statusColor);

                                                Long t = s.child("time")
                                                                .getValue(Long.class);

                                                if (t != null) {
                                                        String pattern = "dd MMM yyyy '" + getString(R.string.label_at) + "' hh:mm a";
                                                String dateStr = new java.text.SimpleDateFormat(
                                                                pattern,
                                                                java.util.Locale.getDefault())
                                                                .format(new java.util.Date(t));
                                                dateTxt.setText(dateStr);
                                                }

                                                proofPath = s.child("proofPath")
                                                                .getValue(String.class);

                                                if (proofPath != null && !proofPath.isEmpty()) {

                                                        loadImageFromUrl(proofPath, proofImage);

                                                        proofImage.setOnClickListener(v -> {

                                                                Intent i = new Intent(
                                                                                ApproveRejectActivity.this,
                                                                                ImagePreviewActivity.class);

                                                                i.putExtra("img", proofPath);
                                                                startActivity(i);
                                                        });

                                                } else {
                                                        proofImage.setVisibility(View.GONE);
                                                        downloadBtn.setEnabled(false);
                                                }
                                        }

                                        public void onCancelled(DatabaseError e) {
                                        }
                                });
        }

        void loadDynamicPlans() {
                dynamicPlansRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                dynamicPlans.clear();
                                ArrayList<String> displayNames = new ArrayList<>();

                                for (DataSnapshot d : snapshot.getChildren()) {
                                        SubscriptionPlan p = d.getValue(SubscriptionPlan.class);
                                        if (p != null) {
                                                dynamicPlans.add(p);
                                                displayNames.add(getLocalizedPlanName(p.duration));
                                        }
                                }

                                if (!dynamicPlans.isEmpty()) {
                                        planAdapter = new UserSubscriptionPlanAdapter(dynamicPlans, plan -> {
                                                // Handle plan selection if needed (auto-selection logic is separate)
                                        });
                                        plansRecycler.setAdapter(planAdapter);

                                        checkAndSelectPlannedItem();
                                }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                });
        }

        private void checkAndSelectPlannedItem() {
                if (dynamicPlans.isEmpty() || userRequestedPlan == null || userRequestedPlan.isEmpty() || planAdapter == null) {
                    android.util.Log.d("ApproveReject", "Skipping select: plans empty? " + dynamicPlans.isEmpty() + ", reqPlan: " + userRequestedPlan);
                    return;
                }
                
                String req = userRequestedPlan.toLowerCase();
                int matchIndex = -1;

                for (int i = 0; i < dynamicPlans.size(); i++) {
                        String duration = dynamicPlans.get(i).duration.toLowerCase();
                        // Flexible matching: either exact, or one contains the other
                        if (duration.equals(req) || duration.contains(req) || req.contains(duration)) {
                                matchIndex = i;
                                break;
                        }
                }

                if (matchIndex != -1) {
                        android.util.Log.d("ApproveReject", "Found match for " + userRequestedPlan + " at index " + matchIndex);
                        planAdapter.setSelectedPosition(matchIndex);
                        plansRecycler.scrollToPosition(matchIndex);
                } else {
                        android.util.Log.d("ApproveReject", "No match found for " + userRequestedPlan + ", defaulting to index 0");
                        planAdapter.setSelectedPosition(0); // Default to first plan if no match found
                }
        }

        void loadExpiryFromUser() {
                userRef.child("subscriptionExpiry").get().addOnSuccessListener(s -> {
                        if (s.exists()) {
                                currentExpiry = (long) s.getValue();
                                String dateStr = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.US) // ✅
                                                                                                                   // Locale.US:
                                                                                                                   // ASCII
                                                                                                                   // digits
                                                                                                                   // for
                                                                                                                   // display
                                                .format(new Date(currentExpiry));
                                txtCurrentExpiry.setText(getString(R.string.label_expires_on, dateStr));
                                txtCurrentExpiry.setVisibility(View.VISIBLE);
                                btnUpdateExpiry.setVisibility(View.VISIBLE);
                                findViewById(R.id.expiryInfoCard).setVisibility(View.VISIBLE);
                        }
                });
        }

        void pickNewExpiry() {
                android.icu.util.Calendar c = android.icu.util.Calendar.getInstance();
                if (currentExpiry > 0)
                        c.setTimeInMillis(currentExpiry);

                new android.app.DatePickerDialog(this, (view, year, month, day) -> {
                        android.icu.util.Calendar s = android.icu.util.Calendar.getInstance();
                        s.set(year, month, day, 23, 59, 59);
                        long newExpiry = s.getTimeInMillis();
                        updateExpiryInDatabase(newExpiry);
                }, c.get(android.icu.util.Calendar.YEAR), c.get(android.icu.util.Calendar.MONTH),
                                c.get(android.icu.util.Calendar.DAY_OF_MONTH)).show();
        }

        void updateExpiryInDatabase(long newExpiry) {
                userRef.child("subscriptionExpiry").setValue(newExpiry).addOnSuccessListener(aVoid -> {
                        currentExpiry = newExpiry;
                        loadExpiryFromUser();
                        Toast.makeText(this, R.string.msg_expiry_updated, Toast.LENGTH_SHORT).show();
                });
        }

        // ---------------------------

        private void loadImageFromUrl(String url, ImageView imageView) {

                imageView.setImageResource(
                                android.R.drawable.ic_menu_gallery);

                new Thread(() -> {
                        try {

                                java.net.URL u = new java.net.URL(url);
                                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) u.openConnection();

                                conn.setDoInput(true);
                                conn.connect();

                                java.io.InputStream input = conn.getInputStream();

                                Bitmap bitmap = BitmapFactory.decodeStream(input);

                                imageView.post(() -> imageView.setImageBitmap(bitmap));

                        } catch (Exception e) {
                                e.printStackTrace();

                                imageView.post(() -> imageView.setImageResource(
                                                android.R.drawable.ic_menu_report_image));
                        }
                }).start();
        }

        void approve() {

                if (dynamicPlans.isEmpty() || planAdapter == null) return;
                SubscriptionPlan selected = dynamicPlans.get(planAdapter.getSelectedPosition());
                String grantedPlan = selected.duration;

                long now = System.currentTimeMillis();
                long expiry = calculateExpiry(now, grantedPlan);

                requestRef.child("status").setValue("approved");
                requestRef.child("plan").setValue(grantedPlan);
                requestRef.child("expiryDate").setValue(expiry);
                requestRef.child("amountApplied").setValue(selected.amount);

                userRef.child("subscribed").setValue(true);
                userRef.child("subscriptionStatus").setValue("approved");
                userRef.child("plan").setValue(grantedPlan);
                userRef.child("subscriptionExpiry").setValue(expiry);
                userRef.child("subscriptionStartDate").setValue(now);

                Toast.makeText(this,
                                R.string.msg_approved,
                                Toast.LENGTH_SHORT).show();

                                NotificationHelper.send(
                                ApproveRejectActivity.this,
                                uid,
                                getString(R.string.title_sub_approved),
                                getString(R.string.msg_sub_approved_desc));

                finish();
        }

        // ---------------------------

        void reject() {

                requestRef.child("status")
                                .setValue("rejected");

                userRef.child("subscribed")
                                .setValue(false);

                userRef.child("subscriptionStatus")
                                .setValue("rejected");

                Toast.makeText(this,
                                R.string.msg_rejected,
                                Toast.LENGTH_SHORT).show();

                                NotificationHelper.send(
                                ApproveRejectActivity.this,
                                uid,
                                getString(R.string.title_sub_rejected),
                                getString(R.string.msg_sub_rejected_desc));

                finish();
        }

        // ---------------------------

        void saveToGallery() {

                if (proofPath == null || proofPath.isEmpty()) {
                        Toast.makeText(this,
                                        R.string.msg_no_image,
                                        Toast.LENGTH_SHORT).show();
                        return;
                }

                new Thread(() -> {
                        try {

                                java.net.URL url = new java.net.URL(proofPath);
                                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();

                                conn.connect();

                                Bitmap bitmap = BitmapFactory.decodeStream(conn.getInputStream());

                                ContentValues values = new ContentValues();
                                values.put(MediaStore.Images.Media.DISPLAY_NAME,
                                                "proof_" + uid + ".jpg");
                                values.put(MediaStore.Images.Media.MIME_TYPE,
                                                "image/jpeg");
                                values.put(MediaStore.Images.Media.RELATIVE_PATH,
                                                "Pictures/RMAdsMaker");

                                Uri uri = getContentResolver().insert(
                                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                                values);

                                OutputStream out = getContentResolver().openOutputStream(uri);

                                bitmap.compress(
                                                Bitmap.CompressFormat.JPEG,
                                                100,
                                                out);

                                out.close();

                                runOnUiThread(() -> Toast.makeText(
                                                this,
                                                R.string.msg_saved_to_gallery,
                                                Toast.LENGTH_LONG).show());

                        } catch (Exception e) {

                                runOnUiThread(() -> Toast.makeText(
                                                this,
                                                R.string.msg_save_failed,
                                                Toast.LENGTH_SHORT).show());
                        }
                }).start();
        }

        private String getLocalizedPlanName(String canonical) {
                if (canonical == null) return "";
                String c = canonical.toLowerCase();
                if (c.contains("silver")) return getString(R.string.plan_silver);
                if (c.contains("gold")) return getString(R.string.plan_gold);
                if (c.contains("diamond")) return getString(R.string.plan_diamond);
                if (c.contains("custom") || c.contains("7 days")) return getString(R.string.plan_custom);
                if (c.contains("1 month")) return getString(R.string.plan_1_month);
                if (c.contains("3 month")) return getString(R.string.plan_3_month);
                if (c.contains("6 month")) return getString(R.string.plan_6_month);
                if (c.contains("1 year")) return getString(R.string.plan_1_year);
                return canonical;
        }

        private long calculateExpiry(long startTime, String duration) {
                if (duration == null) return startTime + (7L * 24 * 60 * 60 * 1000);
                String d = duration.toLowerCase();
                long day = 24L * 60 * 60 * 1000;
                
                if (d.contains("1 month") || d.contains("silver")) return startTime + (30 * day);
                if (d.contains("3 month")) return startTime + (90 * day);
                if (d.contains("6 month") || d.contains("gold")) return startTime + (180 * day);
                if (d.contains("1 year") || d.contains("diamond")) return startTime + (365 * day);
                if (d.contains("7 days")) return startTime + (7 * day);
                
                // Fallback: try to parse number of days if starts with digit
                try {
                    String firstPart = d.split(" ")[0];
                    int num = Integer.parseInt(firstPart);
                    if (d.contains("month")) return startTime + (num * 30L * day);
                    if (d.contains("year")) return startTime + (num * 365L * day);
                    if (d.contains("day")) return startTime + (num * day);
                } catch (Exception ignored) {}

                return startTime + (7L * 24 * 60 * 60 * 1000);
        }
}
