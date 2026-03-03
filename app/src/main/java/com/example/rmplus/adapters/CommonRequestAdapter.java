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
        Context ctx = h.itemView.getContext();

        // 1. SET REQUEST TYPE & ICON
        if(r.requestType.equals("contact")) {
            h.txtRequestType.setText(ctx.getString(R.string.title_submit_request));
            h.imgIcon.setImageResource(R.drawable.ic_clipboard_list);
            h.imgIcon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E6EEFF")));
        } else {
            h.txtRequestType.setText(ctx.getString(R.string.title_adv_request));
            h.imgIcon.setImageResource(R.drawable.ic_megaphone);
            h.imgIcon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFF4E6")));
        }

        // 2. SET TITLE / PURPOSE
        h.title.setText(r.title);

        // 3. SET DATE/TIME (WhatsApp Style: "HH:mm AM/PM")
        String timeStr = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date(r.time));
        h.txtTime.setText(timeStr);

        // 4. SET STATUS WITH COLOR
        String displayStatus = r.status;
        int statusColor = ctx.getColor(R.color.primary);
        
        if ("pending".equalsIgnoreCase(r.status)) {
            displayStatus = ctx.getString(R.string.tab_pending);
            statusColor = android.graphics.Color.parseColor("#F59E0B"); // Yellow/Amber
        } else if ("accepted".equalsIgnoreCase(r.status)) {
            displayStatus = ctx.getString(R.string.tab_accepted);
            statusColor = android.graphics.Color.parseColor("#10B981"); // Green
        } else if ("rejected".equalsIgnoreCase(r.status)) {
            displayStatus = ctx.getString(R.string.tab_rejected);
            statusColor = android.graphics.Color.parseColor("#EF4444"); // Red
        }
        
        h.status.setText(displayStatus);
        h.status.setTextColor(statusColor);

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

        TextView title, status, txtRequestType, txtTime;
        android.widget.ImageView imgIcon;

        Holder(View v){
            super(v);
            title = v.findViewById(R.id.txtTitle);
            status = v.findViewById(R.id.txtStatus);
            txtRequestType = v.findViewById(R.id.txtRequestType);
            txtTime = v.findViewById(R.id.txtTime);
            imgIcon = v.findViewById(R.id.imgIcon);
        }
    }
}
