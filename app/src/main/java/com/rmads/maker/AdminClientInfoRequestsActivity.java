package com.rmads.maker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rmads.maker.models.ClientInfoRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.*;

public class AdminClientInfoRequestsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_client_info_requests);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddService).setOnClickListener(v -> 
                startActivity(new Intent(this, AdminManageServicesActivity.class)));

        TabLayout tab = findViewById(R.id.tabLayout);
        ViewPager2 pager = findViewById(R.id.viewPager);

        pager.setAdapter(new ServiceRequestPagerAdapter(this, true));

        new TabLayoutMediator(tab, pager, (t, p) -> {
            if (p == 0)
                t.setText(R.string.tab_pending);
            if (p == 1)
                t.setText(R.string.tab_accepted);
            if (p == 2)
                t.setText(R.string.tab_rejected);
        }).attach();
    }
}
