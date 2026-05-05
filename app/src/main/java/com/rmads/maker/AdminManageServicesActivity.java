package com.rmads.maker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rmads.maker.models.DynamicService;

import java.util.ArrayList;
import java.util.List;

public class AdminManageServicesActivity extends AppCompatActivity {

    EditText etEn, etHi;
    RecyclerView recycler;
    DatabaseReference ref;
    List<DynamicService> serviceList = new ArrayList<>();
    ServiceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_services);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        etEn = findViewById(R.id.etServiceEn);
        etHi = findViewById(R.id.etServiceHi);
        recycler = findViewById(R.id.recyclerServices);
        
        ref = FirebaseDatabase.getInstance().getReference("client_info_services");

        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ServiceAdapter();
        recycler.setAdapter(adapter);

        findViewById(R.id.btnAdd).setOnClickListener(v -> addService());

        loadServices();
    }

    private void loadServices() {
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                serviceList.clear();
                for (DataSnapshot d : snapshot.getChildren()) {
                    DynamicService s = d.getValue(DynamicService.class);
                    if (s != null) serviceList.add(s);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addService() {
        String en = etEn.getText().toString().trim();
        String hi = etHi.getText().toString().trim();

        if (en.isEmpty() || hi.isEmpty()) {
            Toast.makeText(this, "Please enter both English and Hindi names", Toast.LENGTH_SHORT).show();
            return;
        }

        String id = ref.push().getKey();
        DynamicService s = new DynamicService(id, en, hi);
        
        ref.child(id).setValue(s).addOnSuccessListener(aVoid -> {
            etEn.setText("");
            etHi.setText("");
            Toast.makeText(this, "Service added", Toast.LENGTH_SHORT).show();
        });
    }

    class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_service, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            DynamicService s = serviceList.get(position);
            holder.tvEn.setText(s.en);
            holder.tvHi.setText(s.hi);
            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(AdminManageServicesActivity.this)
                        .setTitle("Delete Service")
                        .setMessage("Are you sure you want to delete this service?")
                        .setPositiveButton("Delete", (d, w) -> ref.child(s.id).removeValue())
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return serviceList.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvEn, tvHi;
            ImageView btnDelete;
            public VH(@NonNull View v) {
                super(v);
                tvEn = v.findViewById(R.id.tvEn);
                tvHi = v.findViewById(R.id.tvHi);
                btnDelete = v.findViewById(R.id.btnDelete);
            }
        }
    }
}
