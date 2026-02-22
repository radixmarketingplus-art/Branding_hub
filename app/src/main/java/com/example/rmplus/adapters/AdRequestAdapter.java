package com.example.rmplus.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.*;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.rmplus.R;
import com.example.rmplus.AdRequestDetailActivity;
import com.example.rmplus.models.AdvertisementRequest;

import java.util.ArrayList;

public class AdRequestAdapter
        extends RecyclerView.Adapter<AdRequestAdapter.Holder> {

    ArrayList<AdvertisementRequest> list;
    boolean isAdmin;
    Context context;

    public AdRequestAdapter(ArrayList<AdvertisementRequest> list,
                            boolean isAdmin,
                            Context context){
        this.list = list;
        this.isAdmin = isAdmin;
        this.context = context;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup p, int v){
        return new Holder(
                LayoutInflater.from(p.getContext())
                        .inflate(R.layout.row_request, p, false)
        );
    }

    @Override
    public void onBindViewHolder(Holder h, int i){

        AdvertisementRequest r = list.get(i);

        // Title — show link or custom text
        h.title.setText("Advertisement");

        h.status.setText(r.status);

        h.itemView.setOnClickListener(v -> {

            Intent intent =
                    new Intent(context,
                            AdRequestDetailActivity.class);

            intent.putExtra("id", r.requestId);
            intent.putExtra("isAdmin", isAdmin);

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount(){
        return list.size();
    }

    class Holder extends RecyclerView.ViewHolder{

        TextView title, status;

        Holder(View v){
            super(v);
            title = v.findViewById(R.id.txtTitle);
            status = v.findViewById(R.id.txtStatus);
        }
    }
}
