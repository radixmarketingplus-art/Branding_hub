package com.example.rmplus;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class FestivalCardAdapter
        extends RecyclerView.Adapter<FestivalCardAdapter.VH> {

    ArrayList<FestivalCardItem> list;

    public FestivalCardAdapter(ArrayList<FestivalCardItem> list) {
        this.list = list;
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
            h.dateBadge.setVisibility(View.GONE);
            h.itemView.setOnClickListener(null);
            return;
        }

        // 🌐 LOAD FROM VPS URL OR LOCAL FILE (Glide handles both)
        Glide.with(h.img.getContext())
                .load(item.imagePath)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(h.img);

        h.dateBadge.setVisibility(View.VISIBLE);
        h.dateBadge.setText(format(item.date));

        // 🔥 PREVIEW CLICK
        h.itemView.setOnClickListener(v -> {
            Context ctx = v.getContext();
            Intent i = new Intent(ctx, TemplatePreviewActivity.class);
            i.putExtra("path", item.imagePath);
            i.putExtra("category", "Festival Cards");
            ctx.startActivity(i);
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

    String format(String d) {
        try {
            SimpleDateFormat in =
                    new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            SimpleDateFormat out =
                    new SimpleDateFormat("d MMM", Locale.getDefault());
            return out.format(in.parse(d));
        } catch (Exception e) {
            return d;
        }
    }
}