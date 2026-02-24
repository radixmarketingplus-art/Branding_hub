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

        recycler.setLayoutManager(
                new GridLayoutManager(this, 2)
        );

        adapter = new TemplateGridAdapter(
                new ArrayList<>(),
                this::openPreview
        );
        recycler.setAdapter(adapter);

        loadAllTemplates();

        btnSearch.setOnClickListener(v -> performSearch());
        btnMic.setOnClickListener(v -> startVoice());
        btnClear.setOnClickListener(v -> clearSearch());

        // AUTO SEARCH
        edtSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

            public void onTextChanged(CharSequence s, int a, int b, int c) {
                performSearch();
            }

            public void afterTextChanged(Editable e) {}
        });
    }

    // --------------------------------------

    void performSearch() {
        String q = edtSearch.getText().toString().trim().toLowerCase();

        if (q.isEmpty()) {
            adapter.setData(new ArrayList<>());
            txtEmpty.setVisibility(View.GONE);
            return;
        }

        ArrayList<TemplateModel> result = new ArrayList<>();

        for (TemplateSearchItem item : allTemplates) {
            if (item.title.contains(q)
                    || item.category.toLowerCase().contains(q)
                    || (item.keywords != null && item.keywords.contains(q))
            ) {
                // Determine ID (extracting from path or using item.title if ID is not available in TemplateSearchItem)
                // Actually, let's just use the path as the "url" and some generated ID if missing
                result.add(new TemplateModel(makeSafeKey(item.path), item.path, item.category));
            }
        }

        adapter.setData(result);
        txtEmpty.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
    }
    
    private String makeSafeKey(String s) {
        return s.replaceAll("[^a-zA-Z0-9]", "");
    }


    // --------------------------------------

    String extractTitle(String path) {
        String name = path.substring(path.lastIndexOf("/") + 1);
        return name.replace(".jpg","")
                .replace(".png","")
                .replace("_"," ")
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
        txtEmpty.setVisibility(View.GONE);
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
        voiceLauncher.launch(i);
    }

    ActivityResultLauncher<Intent> voiceLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    r -> {
                        if (r.getResultCode() == RESULT_OK &&
                                r.getData() != null) {

                            ArrayList<String> res =
                                    r.getData()
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
                            if (k == null || k.equals("Advertisement") || k.equals("Frame")) continue;

                            for (DataSnapshot itemSnapshot : categorySnapshot.getChildren()) {
                                String path = itemSnapshot.hasChild("imagePath") ? itemSnapshot.child("imagePath").getValue(String.class) : itemSnapshot.child("url").getValue(String.class);
                                if (path == null) continue;

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
                    public void onCancelled(DatabaseError error) {}
                });
    }
}
