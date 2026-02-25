package com.example.rmplus;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class UserListActivity extends AppCompatActivity {

    RecyclerView rvUsers;
    ProgressBar loader;
    TextView txtEmpty;
    ArrayList<UserItem> list = new ArrayList<>();
    ArrayList<UserItem> fullList = new ArrayList<>(); // To store original data
    UserListAdapter adapter;
    EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvUsers = findViewById(R.id.rvUsers);
        loader = findViewById(R.id.loader);
        txtEmpty = findViewById(R.id.txtEmpty);
        etSearch = findViewById(R.id.etSearch);

        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserListAdapter(this, list);
        rvUsers.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadUsers();
    }

    private void filter(String text) {
        ArrayList<UserItem> filteredList = new ArrayList<>();

        for (UserItem item : fullList) {
            String query = text.toLowerCase().trim();
            boolean match = false;

            if (item.name != null && item.name.toLowerCase().contains(query)) match = true;
            if (item.email != null && item.email.toLowerCase().contains(query)) match = true;
            if (item.mobile != null && item.mobile.contains(query)) match = true;
            if (item.uid != null && item.uid.toLowerCase().contains(query)) match = true;

            if (match) {
                filteredList.add(item);
            }
        }

        adapter.updateList(filteredList);
    }

    private void loadUsers() {
        loader.setVisibility(View.VISIBLE);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                fullList.clear();
                for (DataSnapshot d : snapshot.getChildren()) {
                    UserItem user = d.getValue(UserItem.class);
                    if (user != null) {
                        user.uid = d.getKey();
                        list.add(user);
                        fullList.add(user);
                    }
                }

                loader.setVisibility(View.GONE);
                if (list.isEmpty()) {
                    txtEmpty.setVisibility(View.VISIBLE);
                    rvUsers.setVisibility(View.GONE);
                } else {
                    txtEmpty.setVisibility(View.GONE);
                    rvUsers.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loader.setVisibility(View.GONE);
                Toast.makeText(UserListActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
