package com.example.rmplus;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class FestivalCardAdapter
        extends RecyclerView.Adapter<FestivalCardAdapter.VH> {

    public interface OnFestivalClickListener {
        void onClick(FestivalCardItem item);
    }

    ArrayList<FestivalCardItem> list;
    OnFestivalClickListener clickListener;

    public FestivalCardAdapter(ArrayList<FestivalCardItem> list, OnFestivalClickListener clickListener) {
        this.list = list;
        this.clickListener = clickListener;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_square, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH h, int position) {

        FestivalCardItem item = list.get(position);

        if ("DUMMY".equals(item.imagePath)) {
            h.img.setImageResource(R.drawable.ic_add);
            if (h.dateBadge != null) h.dateBadge.setVisibility(View.GONE);

            h.itemView.setOnClickListener(null);
            return;
        }

        // 🌐 LOAD IMAGE
        Glide.with(h.img.getContext())
                .load(item.imagePath)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(h.img);

        // DATE BADGE (Restored as per user request)
        if (h.dateBadge != null) {
            h.dateBadge.setVisibility(View.VISIBLE);
            h.dateBadge.setText(formatDate(item.date));
        }





        // 🔥 CLICK
        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {

        ImageView img;
        TextView dateBadge;

        VH(View v) {
            super(v);
            img = v.findViewById(R.id.img);
            dateBadge = v.findViewById(R.id.txtDateBadge);
        }
    }

    String formatDate(String d) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
            SimpleDateFormat out = new SimpleDateFormat("d MMM", Locale.getDefault());
            return out.format(in.parse(d));
        } catch (Exception e) {
            return d != null ? d : "";
        }
    }
}