package com.rmads.maker;

import android.content.*;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.*;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;

public class SearchActivity extends BaseActivity {

    private EditText edtSearch;
    private ImageButton btnSearch, btnMic, btnClear;
    private RecyclerView recycler, recyclerRecent, recyclerRecommended;
    private View emptyView, suggestionsView, sectionRecent;
    private ChipGroup chipGroupPopular;

    private TemplateGridAdapter resultsAdapter;
    private SuggestionAdapter recentAdapter, recommendedAdapter;

    private ArrayList<TemplateSearchItem> allTemplates = new ArrayList<>();
    private List<String> recentSearches = new ArrayList<>();
    private List<String> dynamicRecommended = new ArrayList<>();
    private Set<String> dynamicCategories = new HashSet<>();
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_search);
        prefs = getSharedPreferences("search_prefs", MODE_PRIVATE);

        initViews();
        setupResultsRecycler();
        setupSuggestionRecyclers();
        setupChips();
        
        loadAllTemplates();
        loadRecentSearches();

        btnSearch.setOnClickListener(v -> performSearch(edtSearch.getText().toString()));
        btnMic.setOnClickListener(v -> startVoice());
        btnClear.setOnClickListener(v -> clearSearch());

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                btnClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                if (s.length() == 0) {
                    showSuggestions();
                } else {
                    performSearch(s.toString());
                }
            }
            @Override public void afterTextChanged(Editable e) {}
        });

        edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            String q = edtSearch.getText().toString();
            performSearch(q);
            saveSearchQuery(q);
            return true;
        });
    }

    private void initViews() {
        edtSearch = findViewById(R.id.edtSearch);
        btnSearch = findViewById(R.id.btnSearch);
        btnMic = findViewById(R.id.btnMic);
        btnClear = findViewById(R.id.btnClear);
        recycler = findViewById(R.id.recyclerSearch);
        emptyView = findViewById(R.id.emptyView);
        suggestionsView = findViewById(R.id.suggestionsView);
        sectionRecent = findViewById(R.id.sectionRecent);
        recyclerRecent = findViewById(R.id.recyclerRecent);
        recyclerRecommended = findViewById(R.id.recyclerRecommended);
        chipGroupPopular = findViewById(R.id.chipGroupPopular);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupResultsRecycler() {
        recycler.setLayoutManager(new GridLayoutManager(this, 3));
        resultsAdapter = new TemplateGridAdapter(new ArrayList<>(), R.layout.item_grid_square, this::openPreview);
        recycler.setAdapter(resultsAdapter);
    }

    private void setupSuggestionRecyclers() {
        recyclerRecent.setLayoutManager(new LinearLayoutManager(this));
        recentAdapter = new SuggestionAdapter(recentSearches, true, this::performSearchFromSuggestion, this::removeRecentQuery);
        recyclerRecent.setAdapter(recentAdapter);

        recyclerRecommended.setLayoutManager(new LinearLayoutManager(this));
        recommendedAdapter = new SuggestionAdapter(dynamicRecommended, false, this::performSearchFromSuggestion, null);
        recyclerRecommended.setAdapter(recommendedAdapter);
    }

    private void setupChips() {
        chipGroupPopular.removeAllViews();
        for (String p : dynamicCategories) {
            Chip chip = new Chip(this);
            chip.setText(localizeDisplay(p)); // Show Hindi if active
            chip.setChipBackgroundColorResource(R.color.surface_light);
            chip.setChipStrokeColorResource(R.color.card_stroke);
            chip.setChipStrokeWidth(1.5f);
            chip.setOnClickListener(v -> performSearchFromSuggestion(p));
            chipGroupPopular.addView(chip);
        }
    }

    // --------------------------------------

    private void performSearchFromSuggestion(String query) {
        edtSearch.setText(query);
        edtSearch.setSelection(query.length());
        performSearch(query);
        saveSearchQuery(query);
    }

    private void performSearch(String rawQ) {
        String q = rawQ.trim().toLowerCase();
        if (q.isEmpty()) {
            showSuggestions();
            return;
        }

        suggestionsView.setVisibility(View.GONE);
        recycler.setVisibility(View.VISIBLE);

        // Improved bilingual search: detect keywords in BOTH languages
        String altQ = translateHindiToEnglish(q);
        String[] qWords = altQ.split("\\s+"); // Split all detected keywords
        
        ArrayList<TemplateModel> result = new ArrayList<>();

        for (TemplateSearchItem item : allTemplates) {
            String fullSearchBlob = (item.title + " " + item.category + " " + (item.keywords != null ? item.keywords : "")).toLowerCase();
            
            boolean isMatch = false;
            // Check if ANY word from our expanded query matches the template
            for (String word : qWords) {
                if (word.length() < 2) continue; // Skip tiny noise characters
                if (fullSearchBlob.contains(word)) {
                    isMatch = true;
                    break;
                }
            }

            if (isMatch) {
                result.add(new TemplateModel(makeSafeKey(item.path), item.path, item.category));
            }
        }

        resultsAdapter.setData(result);
        emptyView.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
        recycler.setVisibility(result.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showSuggestions() {
        suggestionsView.setVisibility(View.VISIBLE);
        recycler.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        loadRecentSearches();
    }

    private void saveSearchQuery(String q) {
        if (q == null || q.trim().isEmpty()) return;
        String query = q.trim();
        recentSearches.remove(query);
        recentSearches.add(0, query);
        if (recentSearches.size() > 10) recentSearches.remove(10);
        prefs.edit().putString("recent", new Gson().toJson(recentSearches)).apply();
    }

    private void removeRecentQuery(String q) {
        recentSearches.remove(q);
        prefs.edit().putString("recent", new Gson().toJson(recentSearches)).apply();
        loadRecentSearches();
    }

    private void loadRecentSearches() {
        String json = prefs.getString("recent", null);
        if (json != null) {
            recentSearches.clear();
            Type type = new TypeToken<List<String>>() {}.getType();
            recentSearches.addAll(new Gson().fromJson(json, type));
            sectionRecent.setVisibility(recentSearches.isEmpty() ? View.GONE : View.VISIBLE);
            if (recentAdapter != null) recentAdapter.notifyDataSetChanged();
        } else {
            sectionRecent.setVisibility(View.GONE);
        }
    }

    private void clearSearch() {
        edtSearch.setText("");
        showSuggestions();
    }

    // --------------------------------------

    // --- ADAPTER FOR SUGGESTIONS ---
    private class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.VH> {
        private List<String> items;
        private boolean isHistory;
        private Consumer<String> callback;
        private Consumer<String> removeCallback;

        SuggestionAdapter(List<String> items, boolean isHistory, Consumer<String> callback, Consumer<String> removeCallback) {
            this.items = items;
            this.isHistory = isHistory;
            this.callback = callback;
            this.removeCallback = removeCallback;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(getLayoutInflater().inflate(R.layout.item_search_suggestion, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int p) {
            String s = items.get(p);
            h.txt.setText(localizeDisplay(s)); // Translate for display
            h.img.setImageResource(isHistory ? android.R.drawable.ic_menu_recent_history : android.R.drawable.ic_menu_send);
            if (!isHistory) h.img.setRotation(-45); else h.img.setRotation(0);
            
            h.imgArrow.setVisibility(isHistory ? View.GONE : View.VISIBLE);
            h.btnRemove.setVisibility(isHistory ? View.VISIBLE : View.GONE);
            
            h.itemView.setOnClickListener(v -> callback.accept(s));
            h.btnRemove.setOnClickListener(v -> {
                if (removeCallback != null) removeCallback.accept(s);
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView txt; ImageView img, imgArrow; ImageButton btnRemove;
            VH(View v) { 
                super(v); 
                txt = v.findViewById(R.id.txtTitle); 
                img = v.findViewById(R.id.imgIcon); 
                imgArrow = v.findViewById(R.id.imgArrow);
                btnRemove = v.findViewById(R.id.btnRemove);
            }
        }
    }

    private void openPreview(TemplateModel t) {
        Intent i = new Intent(this, TemplatePreviewActivity.class);
        i.putExtra("id", t.id); i.putExtra("path", t.url); i.putExtra("category", t.category);
        startActivity(i);
    }

    private void loadAllTemplates() {
        FirebaseDatabase.getInstance().getReference("templates").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                allTemplates.clear();
                dynamicCategories.clear();
                for (DataSnapshot cat : s.getChildren()) {
                    String k = cat.getKey();
                    if (k == null || k.equals("Advertisement") || k.equals("Frame")) continue;
                    
                    dynamicCategories.add(k); // Detect categories in real-time
                    
                    for (DataSnapshot item : cat.getChildren()) {
                        String path = item.hasChild("imagePath") ? item.child("imagePath").getValue(String.class) : item.child("url").getValue(String.class);
                        if (path == null) continue;
                        String title = extractTitle(path);
                        allTemplates.add(new TemplateSearchItem(path, title, k, buildKeywords(title, k)));
                    }
                }
                
                // Update suggestions based on real-time data
                updateDynamicSuggestions();
            }
            @Override public void onCancelled(DatabaseError e) {}
        });
    }

    private void updateDynamicSuggestions() {
        dynamicRecommended.clear();
        List<String> cats = new ArrayList<>(dynamicCategories);
        Collections.shuffle(cats);
        for (int i = 0; i < Math.min(cats.size(), 5); i++) {
            dynamicRecommended.add(cats.get(i));
        }
        recommendedAdapter.notifyDataSetChanged();
        setupChips(); // Refresh chips with real categories
    }

    private String extractTitle(String p) { return p.substring(p.lastIndexOf("/") + 1).replace(".jpg", "").replace(".png", "").replace("_", " ").toLowerCase(); }
    private String buildKeywords(String t, String c) { return (t + " " + c + " poster template design photo image").toLowerCase(); }
    private String translateHindiToEnglish(String q) {
        if (q == null || q.isEmpty()) return q;
        String res = q;
        
        // --- Business & Categories ---
        if (containsAny(q, "व्यापार", "बिज़नेस", "धंधा", "दुकान")) res += " business shop";
        if (containsAny(q, "राजनीति", "चुनाव", "पार्टी", "नेता", "प्रचार")) res += " political election party leader";
        if (containsAny(q, "त्योहार", "उत्सव", "पर्व", "होली", "दीवाली", "ईद")) res += " festival celebration greetings";
        if (containsAny(q, "विज्ञापन", "एड", "प्रचार")) res += " advertisement ads";
        if (containsAny(q, "जन्मदिन", "जन्म", "बर्थडे")) res += " birthday birth";
        if (containsAny(q, "शिक्षा", "स्कूल", "कोचिंग", "पढ़ाई")) res += " education school coaching";
        if (containsAny(q, "स्वास्थ", "अस्पताल", "डॉक्टर", "दवाई")) res += " health hospital doctor medical";
        if (containsAny(q, "भगवान", "भक्ति", "मंदिर", "पूजा", "राम", "शिव")) res += " god devotional temple prayer";
        if (containsAny(q, "खेल", "क्रिकेट", "मैच")) res += " sports cricket match";
        if (containsAny(q, "शुभकामना", "बधाई", "मुबारक")) res += " wishes greeting congratulation";
        if (containsAny(q, "गहने", "ज्वेलरी", "सोना", "चांदी")) res += " jewelry gold silver jewelry";
        if (containsAny(q, "ज़मीन", "मकान", "प्रॉपर्टी", "प्लाट", "रियल एस्टेट")) res += " real estate property home plot house";
        if (containsAny(q, "होटल", "खाना", "रेस्टोरेंट", "नाश्ता")) res += " restaurant food hotel";
        if (containsAny(q, "जिम", "कसरत", "फिटनेस")) res += " gym fitness workout";
        
        // --- Design Terms ---
        if (containsAny(q, "पोस्टर", "बैनर", "डिज़ाइन")) res += " poster banner design graphic";
        if (containsAny(q, "फोटो", "चित्र", "इमेज")) res += " photo image picture";
        if (containsAny(q, "वीडियो", "रील")) res += " video reel";

        return res;
    }

    private boolean containsAny(String q, String... keywords) {
        for (String k : keywords) {
            if (q.contains(k)) return true;
        }
        return false;
    }

    private String localizeDisplay(String s) {
        if (s == null) return "";

        // Map Category IDs to localized Display Names
        if (s.equalsIgnoreCase("Business")) return getString(R.string.cat_business);
        if (s.equalsIgnoreCase("Political")) return getString(R.string.cat_political);
        if (s.equalsIgnoreCase("Festival")) return getString(R.string.cat_festival);
        if (s.equalsIgnoreCase("Jewelry")) return getString(R.string.cat_jewelry);
        if (s.equalsIgnoreCase("Real Estate")) return getString(R.string.cat_real_estate);
        if (s.equalsIgnoreCase("Education")) return getString(R.string.cat_education);
        if (s.equalsIgnoreCase("Health")) return getString(R.string.cat_health);
        if (s.equalsIgnoreCase("Motivation")) return getString(R.string.section_motivation);
        if (s.equalsIgnoreCase("Birthday")) return getString(R.string.cat_birthday);
        if (s.equalsIgnoreCase("Restaurant")) return getString(R.string.cat_restaurant);
        if (s.equalsIgnoreCase("Gym")) return getString(R.string.cat_gym);
        if (s.equalsIgnoreCase("Festival Cards")) return getString(R.string.section_festival_cards);
        if (s.equalsIgnoreCase("Business Ethics")) return getString(R.string.section_business_ethics);
        
        return s;
    }

    private void startVoice() { /* Logic handled by system recognizer in BaseActivity */ }
}
