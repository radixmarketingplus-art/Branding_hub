package com.rmads.maker;

import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rmads.maker.adapters.SubscriptionPlanAdapter;
import com.rmads.maker.adapters.SubscriptionRequestAdapter;
import com.rmads.maker.models.SubscriptionPlan;
import com.rmads.maker.models.SubscriptionRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONObject;
import android.util.TypedValue;
import java.util.Date;
import java.util.Locale;
import com.yalantis.ucrop.UCrop;
import java.io.File;
import android.graphics.Color;
import androidx.annotation.Nullable;
import com.google.android.material.datepicker.MaterialDatePicker;
import java.util.Calendar;
import java.util.TimeZone;

public class SubscriptionRequestsActivity extends BaseActivity {

    RecyclerView requestRecycler, plansRecycler;
    TextView pendingBtn, approvedBtn, rejectedBtn;
    TextView tabRequests, tabManage;
    View layoutRequests, layoutManage, mainSelectionIndicator;

    ArrayList<SubscriptionRequest> list = new ArrayList<>();
    SubscriptionRequestAdapter adapter;

    ArrayList<SubscriptionPlan> planList = new ArrayList<>();
    SubscriptionPlanAdapter planAdapter;

    DatabaseReference requestRef, plansRef;
    String currentStatus = "pending";

    // Dynamic Plan Upload Fields
    EditText etDurationCount, etAmount, etDiscount, etUpiId;
    android.widget.AutoCompleteTextView spinnerDurationUnit;
    CheckBox cbSpecificDay;
    EditText etSpecificDate;
    View cardSpecificDate;
    View btnPickScanner;
    Button btnUploadPlan;
    ImageView ivScannerPreview;
    View layoutUploadPrompt;

    Uri scannerUri;
    String editingPlanId = null;
    private ActivityResultLauncher<Intent> scannerPickerLauncher;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_subscription_requests);

        requestRecycler = findViewById(R.id.requestRecycler);
        pendingBtn = findViewById(R.id.pendingBtn);
        approvedBtn = findViewById(R.id.approvedBtn);
        rejectedBtn = findViewById(R.id.rejectedBtn);
        
        tabRequests = findViewById(R.id.tabRequests);
        tabManage = findViewById(R.id.tabManage);
        layoutRequests = findViewById(R.id.layoutRequests);
        layoutManage = findViewById(R.id.layoutManage);
        mainSelectionIndicator = findViewById(R.id.mainSelectionIndicator);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        requestRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SubscriptionRequestAdapter(list, this);
        requestRecycler.setAdapter(adapter);

        requestRef = FirebaseDatabase.getInstance().getReference("subscription_requests");
        plansRef = FirebaseDatabase.getInstance().getReference("dynamic_subscriptions");

        initPlanManagement();
        loadRequests();
        loadPlans();

        // Main Navigation
        tabRequests.setOnClickListener(v -> switchPage(0));
        tabManage.setOnClickListener(v -> switchPage(1));

        // Sub Tabs
        pendingBtn.setOnClickListener(v -> {
            currentStatus = "pending";
            updateRequestTabs(pendingBtn);
            loadRequests();
        });

        approvedBtn.setOnClickListener(v -> {
            currentStatus = "approved";
            updateRequestTabs(approvedBtn);
            loadRequests();
        });

        rejectedBtn.setOnClickListener(v -> {
            currentStatus = "rejected";
            updateRequestTabs(rejectedBtn);
            loadRequests();
        });

        // Default: Requests Page
        tabRequests.post(() -> switchPage(0));
    }

    private void switchPage(int page) {
        if (page == 0) {
            layoutRequests.setVisibility(View.VISIBLE);
            layoutManage.setVisibility(View.GONE);
            animateIndicator(tabRequests);
            tabRequests.setTextColor(android.graphics.Color.WHITE);
            tabManage.setTextColor(getColorFromAttr(com.google.android.material.R.attr.colorOnSurfaceVariant));
            tabRequests.setTypeface(tabRequests.getTypeface(), android.graphics.Typeface.BOLD);
            tabManage.setTypeface(tabRequests.getTypeface(), android.graphics.Typeface.NORMAL);
        } else {
            layoutRequests.setVisibility(View.GONE);
            layoutManage.setVisibility(View.VISIBLE);
            animateIndicator(tabManage);
            tabManage.setTextColor(android.graphics.Color.WHITE);
            tabRequests.setTextColor(getColorFromAttr(com.google.android.material.R.attr.colorOnSurfaceVariant));
            tabManage.setTypeface(tabRequests.getTypeface(), android.graphics.Typeface.BOLD);
            tabRequests.setTypeface(tabRequests.getTypeface(), android.graphics.Typeface.NORMAL);
        }
    }

    private void animateIndicator(View target) {
        int left = target.getLeft();
        int width = target.getWidth();

        mainSelectionIndicator.animate()
                .x(left)
                .setDuration(250)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mainSelectionIndicator.getLayoutParams();
        params.width = width;
        mainSelectionIndicator.setLayoutParams(params);
    }

    private void updateRequestTabs(TextView active) {
        pendingBtn.setBackgroundResource(R.drawable.bg_filter_chip);
        approvedBtn.setBackgroundResource(R.drawable.bg_filter_chip);
        rejectedBtn.setBackgroundResource(R.drawable.bg_filter_chip);

        int inactiveColor = getColorFromAttr(com.google.android.material.R.attr.colorOnSurface);
        pendingBtn.setTextColor(inactiveColor);
        approvedBtn.setTextColor(inactiveColor);
        rejectedBtn.setTextColor(inactiveColor);

        active.setBackgroundResource(R.drawable.bg_filter_chip_selected);
        active.setTextColor(android.graphics.Color.WHITE);
    }

    void initPlanManagement() {
        etDurationCount = findViewById(R.id.etDurationCount);
        spinnerDurationUnit = findViewById(R.id.spinnerDurationUnit);
        etAmount = findViewById(R.id.etAmount);
        etDiscount = findViewById(R.id.etDiscount);
        etUpiId = findViewById(R.id.etUpiId);
        
        // Setup Duration Unit Dropdown
        String[] units = {
                getString(R.string.label_unit_day),
                getString(R.string.label_unit_month),
                getString(R.string.label_unit_year)
        };
        android.widget.ArrayAdapter<String> unitAdapter = new android.widget.ArrayAdapter<>(this, 
                android.R.layout.simple_dropdown_item_1line, units);
        spinnerDurationUnit.setAdapter(unitAdapter);
        spinnerDurationUnit.setText(units[1], false); // Default to Month(s)
        cbSpecificDay = findViewById(R.id.cbSpecificDay);
        etSpecificDate = findViewById(R.id.etSpecificDate);
        cardSpecificDate = findViewById(R.id.cardSpecificDate);
        btnPickScanner = findViewById(R.id.btnPickScanner);
        btnUploadPlan = findViewById(R.id.btnUploadPlan);
        ivScannerPreview = findViewById(R.id.ivScannerPreview);
        layoutUploadPrompt = findViewById(R.id.layoutUploadPrompt);
        plansRecycler = findViewById(R.id.plansRecycler);

        plansRecycler.setLayoutManager(new LinearLayoutManager(this));
        planAdapter = new SubscriptionPlanAdapter(planList, new SubscriptionPlanAdapter.OnPlanClickListener() {
            @Override
            public void onEdit(SubscriptionPlan p) {
                editingPlanId = p.id;
                
                // Parse duration (e.g., "1 Month(s)")
                if (p.duration != null && p.duration.contains(" ")) {
                    String[] parts = p.duration.split(" ", 2);
                    etDurationCount.setText(parts[0]);
                    spinnerDurationUnit.setText(parts[1], false);
                } else {
                    etDurationCount.setText(p.duration);
                }
                
                etAmount.setText(p.amount);
                etDiscount.setText(p.discountPrice);
                etUpiId.setText(p.upiId != null ? p.upiId : "");
                cbSpecificDay.setChecked(p.isSpecificDay);
                if (p.isSpecificDay) {
                    cardSpecificDate.setVisibility(View.VISIBLE);
                    etSpecificDate.setText(p.specificDate);
                } else {
                    cardSpecificDate.setVisibility(View.GONE);
                }
                if (p.scannerUrl != null) {
                    com.bumptech.glide.Glide.with(SubscriptionRequestsActivity.this)
                            .load(p.scannerUrl)
                            .into(ivScannerPreview);
                    ivScannerPreview.setVisibility(View.VISIBLE);
                    layoutUploadPrompt.setVisibility(View.GONE);
                }
                btnUploadPlan.setText(R.string.btn_update_plan);
                // Scroll to top of Manage section to see the form
                findViewById(R.id.layoutManage).scrollTo(0,0);
            }

            @Override
            public void onDelete(SubscriptionPlan p) {
                new android.app.AlertDialog.Builder(SubscriptionRequestsActivity.this)
                        .setTitle(R.string.title_delete_plan)
                        .setMessage(getString(R.string.msg_confirm_delete_plan, p.duration))
                        .setPositiveButton("Yes", (d, w) -> deletePlan(p))
                        .setNegativeButton("No", null)
                        .show();
            }
        });
        plansRecycler.setAdapter(planAdapter);

        cbSpecificDay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            cardSpecificDate.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        // Material Date Picker for specific day
        etSpecificDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(R.string.title_select_specific_day)
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    // Use a more premium full-screen theme or a standard dark one
                    .setTheme(com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialCalendar_Fullscreen)
                    .build();

            picker.addOnPositiveButtonClickListener(selection -> {
                Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                c.setTimeInMillis(selection);
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH) + 1;
                int day = c.get(Calendar.DAY_OF_MONTH);
                String date = String.format(Locale.US, "%d-%02d-%02d", year, month, day);
                etSpecificDate.setText(date);
            });
            picker.show(getSupportFragmentManager(), "SPECIFIC_DAY");
        });

        btnPickScanner.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            startActivityForResult(i, 101);
        });

        btnUploadPlan.setOnClickListener(v -> uploadPlan());
    }

    private void startCrop(@NonNull Uri uri) {
        String destinationFileName = "cropped_scanner_" + System.currentTimeMillis() + ".jpg";
        UCrop uCrop = UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), destinationFileName)));
        uCrop.withAspectRatio(1, 1);

        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(android.graphics.Bitmap.CompressFormat.JPEG);
        options.setHideBottomControls(false);
        options.setToolbarTitle(getString(R.string.title_crop_qr_scanner));
        options.setToolbarColor(android.graphics.Color.parseColor("#1B1B1B"));
        options.setStatusBarColor(android.graphics.Color.parseColor("#1B1B1B"));
        options.setToolbarWidgetColor(Color.WHITE);
        options.setActiveControlsWidgetColor(android.graphics.Color.parseColor("#4A6CF7"));
        options.setLogoColor(Color.TRANSPARENT);
        options.setDimmedLayerColor(Color.parseColor("#CC000000"));
        options.setRootViewBackgroundColor(Color.BLACK);
        uCrop.withOptions(options);
        uCrop.start(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            Uri sourceUri = data.getData();
            if (sourceUri != null) {
                startCrop(sourceUri);
            }
        } else if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            final Uri resultUri = UCrop.getOutput(data);
            if (resultUri != null) {
                scannerUri = resultUri;
                ivScannerPreview.setImageURI(scannerUri);
                ivScannerPreview.setVisibility(View.VISIBLE);
                layoutUploadPrompt.setVisibility(View.GONE);
                Toast.makeText(this, R.string.msg_scanner_ready, Toast.LENGTH_SHORT).show();
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
            if (cropError != null)
                Toast.makeText(this, getString(R.string.msg_crop_error, cropError.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    void uploadPlan() {
        String count = etDurationCount.getText().toString().trim();
        String unit = spinnerDurationUnit.getText().toString().trim();
        String dur = count + " " + unit;
        
        String amt = etAmount.getText().toString().trim();
        String disc = etDiscount.getText().toString().trim();
        String upi = etUpiId.getText().toString().trim();
        boolean isSpec = cbSpecificDay.isChecked();
        String specDate = etSpecificDate.getText().toString().trim();

        if (count.isEmpty() || amt.isEmpty()) {
            Toast.makeText(this, R.string.msg_check_duration_amount, Toast.LENGTH_SHORT).show();
            return;
        }

        if (disc.isEmpty()) disc = "0";

        if (editingPlanId == null) {
            for (SubscriptionPlan p : planList) {
                if (p.duration.equalsIgnoreCase(dur)) {
                    Toast.makeText(this, getString(R.string.msg_plan_already_exists, dur), Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }

        if (scannerUri != null) {
            uploadScannerAndSave(dur, amt, disc, upi, isSpec, specDate);
        } else if (editingPlanId != null) {
            SubscriptionPlan existing = null;
            for(SubscriptionPlan p : planList) if(p.id.equals(editingPlanId)) existing = p;
            savePlanToFirebase(editingPlanId, dur, amt, disc, existing != null ? existing.scannerUrl : null, upi, isSpec, specDate);
        } else {
            Toast.makeText(this, R.string.msg_select_scanner_first, Toast.LENGTH_SHORT).show();
        }
    }

    void uploadScannerAndSave(String dur, String amt, String disc, String upi, boolean isSpec, String specDate) {
        android.app.ProgressDialog pd = new android.app.ProgressDialog(this);
        pd.setMessage(getString(R.string.msg_uploading_scanner));
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            try {
                String boundary = "----RMPLUS" + System.currentTimeMillis();
                java.net.URL url = new java.net.URL("http://187.77.184.84/upload.php");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                java.io.DataOutputStream out = new java.io.DataOutputStream(conn.getOutputStream());
                out.writeBytes("--" + boundary + "\r\n");
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"scanner.jpg\"\r\n");
                out.writeBytes("Content-Type: image/jpeg\r\n\r\n");

                java.io.InputStream input = getContentResolver().openInputStream(scannerUri);
                byte[] buffer = new byte[8192];
                int len;
                while ((len = input.read(buffer)) != -1) out.write(buffer, 0, len);
                input.close();

                out.writeBytes("\r\n--" + boundary + "--\r\n");
                out.flush();
                out.close();

                if (conn.getResponseCode() == 200) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                    StringBuilder res = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) res.append(line);
                    reader.close();

                    JSONObject json = new JSONObject(res.toString());
                    String imageUrl = json.getString("url");
                    runOnUiThread(() -> {
                        pd.dismiss();
                        String id = (editingPlanId != null) ? editingPlanId : plansRef.push().getKey();
                        savePlanToFirebase(id, dur, amt, disc, imageUrl, upi, isSpec, specDate);
                    });
                } else {
                    runOnUiThread(() -> {
                        pd.dismiss();
                        Toast.makeText(this, R.string.msg_server_rejected_image, Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    Toast.makeText(this, "Network Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    void savePlanToFirebase(String id, String dur, String amt, String disc, String url, String upi, boolean isSpec, String specDate) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("duration", dur);
        map.put("amount", amt);
        map.put("discountPrice", disc);
        map.put("scannerUrl", url);
        map.put("upiId", upi);
        map.put("isSpecificDay", isSpec);
        map.put("specificDate", specDate);

        plansRef.child(id).setValue(map).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, R.string.msg_plan_synchronized, Toast.LENGTH_SHORT).show();
            clearPlanFields();
        });
    }

    private void deletePlan(SubscriptionPlan p) {
        if (p.scannerUrl != null && !p.scannerUrl.isEmpty()) {
            deleteFromVPS(p.scannerUrl);
        }
        plansRef.child(p.id).removeValue();
    }

    private void deleteFromVPS(String url) {
        new Thread(() -> {
            try {
                java.net.URL deleteUrl = new java.net.URL("http://187.77.184.84/delete.php");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) deleteUrl.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String data = "url=" + java.net.URLEncoder.encode(url, "UTF-8");
                java.io.OutputStream out = conn.getOutputStream();
                out.write(data.getBytes());
                out.flush();
                out.close();

                int code = conn.getResponseCode();
                android.util.Log.d("VPS_DELETE", "Status Code: " + code);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    void clearPlanFields() {
        editingPlanId = null;
        etDurationCount.setText("");
        etAmount.setText("");
        etDiscount.setText("0");
        etUpiId.setText("");
        cbSpecificDay.setChecked(false);
        etSpecificDate.setText("");
        ivScannerPreview.setVisibility(View.GONE);
        layoutUploadPrompt.setVisibility(View.VISIBLE);
        scannerUri = null;
        btnUploadPlan.setText(R.string.btn_publish_plan);
    }

    void loadPlans() {
        plansRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                planList.clear();
                for (DataSnapshot d : snapshot.getChildren()) {
                    SubscriptionPlan p = d.getValue(SubscriptionPlan.class);
                    if (p != null) planList.add(p);
                }
                planAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    void loadRequests() {
        requestRef.addValueEventListener(new ValueEventListener() {
            public void onDataChange(DataSnapshot snap) {
                list.clear();
                for (DataSnapshot d : snap.getChildren()) {
                    String status = d.child("status").getValue(String.class);
                    if (status != null && status.equalsIgnoreCase(currentStatus)) {
                        SubscriptionRequest r = d.getValue(SubscriptionRequest.class);
                        if (r != null) {
                            r.uid = d.getKey();
                            list.add(r);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }
            public void onCancelled(DatabaseError e) {}
        });
    }

    private int getColorFromAttr(int attr) {
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}
