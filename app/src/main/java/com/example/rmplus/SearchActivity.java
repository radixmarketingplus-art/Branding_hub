package com.example.rmplus;

import android.content.*;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.*;
import android.view.View;
import android.widget.*;

import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;

public class SearchActivity extends AppCompatActivity {

    EditText edtSearch;
    ImageButton btnSearch, btnMic, btnClear;
    RecyclerView recycler;
    TextView txtEmpty;
    View emptyView;

    TemplateGridAdapter adapter;
    ArrayList<TemplateSearchItem> allTemplates = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_search);

        edtSearch = findViewById(R.id.edtSearch);
        btnSearch = findViewById(R.id.btnSearch);
        btnMic = findViewById(R.id.btnMic);
        btnClear = findViewById(R.id.btnClear);
        recycler = findViewById(R.id.recyclerSearch);
        txtEmpty = findViewById(R.id.txtEmpty);
        emptyView = findViewById(R.id.emptyView);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        recycler.setLayoutManager(
                new GridLayoutManager(this, 3));

        adapter = new TemplateGridAdapter(
                new ArrayList<>(),
                R.layout.item_grid_square,
                this::openPreview);
        recycler.setAdapter(adapter);

        loadAllTemplates();

        btnSearch.setOnClickListener(v -> performSearch());
        btnMic.setOnClickListener(v -> startVoice());
        btnClear.setOnClickListener(v -> clearSearch());

        // AUTO SEARCH
        edtSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                if (s.length() > 0) {
                    btnClear.setVisibility(View.VISIBLE);
                } else {
                    btnClear.setVisibility(View.GONE);
                }
                performSearch();
            }

            @Override
            public void afterTextChanged(Editable e) {
            }
        });
    }

    // --------------------------------------

    void performSearch() {
        String rawQ = edtSearch.getText().toString().trim().toLowerCase();
        String translatedQ = translateHindiToEnglish(rawQ);
        String q = rawQ; // original
        String altQ = translatedQ; // translated

        if (q.isEmpty()) {
            adapter.setData(new ArrayList<>());
            emptyView.setVisibility(View.GONE);
            recycler.setVisibility(View.VISIBLE);
            return;
        }

        ArrayList<TemplateModel> result = new ArrayList<>();

        for (TemplateSearchItem item : allTemplates) {
            boolean matchMain = item.title.contains(q)
                    || item.category.toLowerCase().contains(q)
                    || (item.keywords != null && item.keywords.contains(q));
            
            boolean matchAlt = false;
            if (!altQ.equals(q)) {
                matchAlt = item.title.contains(altQ)
                        || item.category.toLowerCase().contains(altQ)
                        || (item.keywords != null && item.keywords.contains(altQ));
            }

            if (matchMain || matchAlt) {
                result.add(new TemplateModel(makeSafeKey(item.path), item.path, item.category));
            }
        }

        adapter.setData(result);
        if (result.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recycler.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recycler.setVisibility(View.VISIBLE);
        }
    }

    private String translateHindiToEnglish(String q) {
        if (q == null || q.isEmpty()) return q;
        String res = q;
        
        // Category Mappings
        if (q.contains("व्यापार") || q.contains("बिज़नेस")) res += " business";
        if (q.contains("राजनीति") || q.contains("राजनीतिक") || q.contains("चुनाव")) res += " political";
        if (q.contains("त्योहार") || q.contains("उत्सव")) res += " festival";
        if (q.contains("विज्ञापन")) res += " advertisement";
        if (q.contains("प्रेरणा") || q.contains("प्रेरक")) res += " motivation";
        if (q.contains("शुभकामनाएं") || q.contains("बधाई")) res += " wishes greeting";
        if (q.contains("जन्मदिन") || q.contains("सालगिरह")) res += " birthday";
        if (q.contains("शिक्षा")) res += " education";
        if (q.contains("स्वास्थ") || q.contains("अस्पताल")) res += " health hospital";
        if (q.contains("भक्ति") || q.contains("भगवान")) res += " devotional god";
        if (q.contains("मजदूरी") || q.contains("श्रमिक")) res += " labor worker";
        if (q.contains("खेल") || q.contains("क्रिकेट")) res += " sports cricket";
        
        // General term mappings
        if (q.contains("पोस्टर")) res += " poster";
        if (q.contains("डिजाइन")) res += " design";
        if (q.contains("फोटो") || q.contains("चित्र")) res += " photo image";
        if (q.contains("वीडियो")) res += " video";
        
        return res;
    }

    private String makeSafeKey(String s) {
        return s.replaceAll("[^a-zA-Z0-9]", "");
    }

    // --------------------------------------

    String extractTitle(String path) {
        String name = path.substring(path.lastIndexOf("/") + 1);
        return name.replace(".jpg", "")
                .replace(".png", "")
                .replace("_", " ")
                .toLowerCase();
    }

    String buildKeywords(String title, String category) {
        return (title + " " +
                category +
                " poster template design photo image graphic").toLowerCase();
    }

    // --------------------------------------

    void clearSearch() {
        edtSearch.setText("");
        adapter.setData(new ArrayList<>());
        emptyView.setVisibility(View.GONE);
        recycler.setVisibility(View.VISIBLE);
    }

    // --------------------------------------

    void openPreview(TemplateModel template) {
        Intent i = new Intent(this, TemplatePreviewActivity.class);
        i.putExtra("id", template.id);
        i.putExtra("path", template.url);
        i.putExtra("category", template.category);
        startActivity(i);
    }

    // --------------------------------------

    void startVoice() {
        Intent i = new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault());
        i.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.hint_speak_now));
        voiceLauncher.launch(i);
    }

    ActivityResultLauncher<Intent> voiceLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            r -> {
                if (r.getResultCode() == RESULT_OK &&
                        r.getData() != null) {

                    ArrayList<String> res = r.getData()
                            .getStringArrayListExtra(
                                    RecognizerIntent.EXTRA_RESULTS);

                    if (res != null && !res.isEmpty()) {
                        edtSearch.setText(res.get(0));
                        performSearch();
                    }
                }
            });

    // --------------------------------------

    void loadAllTemplates() {
        allTemplates.clear();
        FirebaseDatabase.getInstance().getReference("templates")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                            String k = categorySnapshot.getKey();
                            if (k == null || k.equals("Advertisement") || k.equals("Frame"))
                                continue;

                            for (DataSnapshot itemSnapshot : categorySnapshot.getChildren()) {
                                String path = itemSnapshot.hasChild("imagePath")
                                        ? itemSnapshot.child("imagePath").getValue(String.class)
                                        : itemSnapshot.child("url").getValue(String.class);
                                if (path == null)
                                    continue;

                                String title = extractTitle(path);
                                String keywords = buildKeywords(title, k);
                                if (k.equals("Festival Cards")) {
                                    String date = itemSnapshot.child("date").getValue(String.class);
                                    if (date != null) {
                                        keywords += " festival greeting celebration wishes " + date;
                                    } else {
                                        keywords += " festival greeting celebration wishes";
                                    }
                                }

                                allTemplates.add(new TemplateSearchItem(path, title, k, keywords));
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                    }
                });
    }
}
