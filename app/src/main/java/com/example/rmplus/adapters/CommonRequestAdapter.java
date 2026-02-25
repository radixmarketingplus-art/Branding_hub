package com.example.rmplus.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.*;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.rmplus.R;
import com.example.rmplus.RequestDetailActivity;
import com.example.rmplus.AdRequestDetailActivity;
import com.example.rmplus.models.CommonRequest;

import java.util.ArrayList;

public class CommonRequestAdapter
        extends RecyclerView.Adapter<CommonRequestAdapter.Holder> {

    ArrayList<CommonRequest> list;
    Context context;

    public CommonRequestAdapter(
            ArrayList<CommonRequest> list,
            Context context){

        this.list = list;
        this.context = context;
    }

    @Override
    public Holder onCreateViewHolder(
            ViewGroup p, int v){

        return new Holder(
                LayoutInflater.from(p.getContext())
                        .inflate(R.layout.row_request, p, false));
    }

    @Override
    public void onBindViewHolder(Holder h, int i){

        CommonRequest r = list.get(i);

        // 🔥 SHOW TYPE
        if(r.requestType.equals("contact"))
            h.title.setText(context.getString(R.string.label_contact_prefix, r.title));
        else
            h.title.setText(context.getString(R.string.label_ad_prefix, r.title));

        String displayStatus = r.status;
        if ("pending".equalsIgnoreCase(r.status)) displayStatus = context.getString(R.string.tab_pending);
        else if ("accepted".equalsIgnoreCase(r.status)) displayStatus = context.getString(R.string.tab_accepted);
        else if ("rejected".equalsIgnoreCase(r.status)) displayStatus = context.getString(R.string.tab_rejected);
        h.status.setText(displayStatus);

        h.itemView.setOnClickListener(v -> {

            Intent intent;

            if(r.requestType.equals("contact")){
                intent = new Intent(
                        context,
                        RequestDetailActivity.class);
            }else{
                intent = new Intent(
                        context,
                        AdRequestDetailActivity.class);
            }

            intent.putExtra("id", r.requestId);
            intent.putExtra("isAdmin", false);

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
