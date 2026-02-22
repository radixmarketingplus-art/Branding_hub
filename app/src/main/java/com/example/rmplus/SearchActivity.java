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

        String q = edtSearch.getText()
                .toString()
                .trim()
                .toLowerCase();

        if (q.isEmpty()) {
            adapter.setData(new ArrayList<>());
            txtEmpty.setVisibility(View.GONE);
            return;
        }

        ArrayList<String> result = new ArrayList<>();

        for (TemplateSearchItem item : allTemplates) {

            if ( item.title.contains(q)
                    || item.category.toLowerCase().contains(q)
                    || item.keywords.contains(q)
            ) {
                result.add(item.path);
            }
        }

        adapter.setData(result);

        txtEmpty.setVisibility(
                result.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );
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

    void openPreview(String path) {
        Intent i = new Intent(this,
                TemplatePreviewActivity.class);
        i.putExtra("path", path);
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

        String[] keys = {
                "Trending Now", "Festival Cards",
                "Latest Update", "Business Special",
                "Reel Maker", "Business Frame",
                "Motivation", "Good Morning",
                "Business Ethics"
        };

        SharedPreferences sp =
                getSharedPreferences("HOME_DATA", MODE_PRIVATE);

        Gson gson = new Gson();

        for (String k : keys) {

            String json = sp.getString(k, null);
            if (json == null) continue;

            if (k.equals("Festival Cards")) {

                Type t = new TypeToken<
                        ArrayList<FestivalCardItem>>() {}.getType();

                ArrayList<FestivalCardItem> list =
                        gson.fromJson(json, t);

                if (list != null) {
                    for (FestivalCardItem f : list) {

                        allTemplates.add(
                                new TemplateSearchItem(
                                        f.imagePath,
                                        "festival card",
                                        k,
                                        "festival greeting celebration wishes"
                                )
                        );
                    }
                }

            } else {

                Type t = new TypeToken<
                        ArrayList<String>>() {}.getType();

                ArrayList<String> list =
                        gson.fromJson(json, t);

                if (list != null) {
                    for (String path : list) {

                        String title = extractTitle(path);

                        allTemplates.add(
                                new TemplateSearchItem(
                                        path,
                                        title,
                                        k,
                                        buildKeywords(title, k)
                                )
                        );
                    }
                }
            }
        }
    }
}
