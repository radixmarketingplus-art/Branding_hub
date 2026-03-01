package com.example.rmplus;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.*;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.util.ArrayList;

public class AdvertisementAdapter
        extends RecyclerView.Adapter<AdvertisementAdapter.VH> {

    ArrayList<AdvertisementItem> list;
    int highlightPos = -1;

    public void setHighlightPos(int pos) {
        this.highlightPos = pos;
        notifyDataSetChanged();
    }

    public AdvertisementAdapter(ArrayList<AdvertisementItem> list) {
        this.list = list;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trending_hotstar, parent, false);

        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH h, int position) {

        AdvertisementItem item = list.get(position);

        boolean isVideo = item.imagePath != null && (
                item.imagePath.toLowerCase().endsWith(".mp4") ||
                item.imagePath.toLowerCase().endsWith(".mkv") ||
                item.imagePath.toLowerCase().endsWith(".webm")
        );

        if (isVideo) {
            h.img.setVisibility(View.GONE);
            h.videoView.setVisibility(View.VISIBLE);
            h.videoView.setVideoPath(item.imagePath);
            h.videoView.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                h.videoView.start();
            });
        } else {
            h.img.setVisibility(View.VISIBLE);
            h.videoView.setVisibility(View.GONE);
            // 🌐 LOAD FROM VPS URL (Glide handles caching automatically)
            Glide.with(h.img.getContext())
                    .load(item.imagePath)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(h.img);
        }

        // 🔥 CLICK → OPEN LINK IN BROWSER
        h.itemView.setOnClickListener(v -> {
            Context ctx = v.getContext();
            if (item.link != null && !item.link.isEmpty()) {
                String finalLink = item.link.trim();
                if (!finalLink.startsWith("http://") && !finalLink.startsWith("https://")) {
                    finalLink = "https://" + finalLink;
                }
                Intent i = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(finalLink)
                );
                ctx.startActivity(i);
            }
        });

        // ✨ HIGHLIGHT LOGIC (Blue Border) - Specific Position
        if (h.itemView instanceof com.google.android.material.card.MaterialCardView) {
            com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) h.itemView;
            if (highlightPos != -1 && highlightPos == position) {
                card.setStrokeColor(android.graphics.Color.parseColor("#4A6CF7"));
                card.setStrokeWidth(12);
                card.setCardElevation(20);
            } else {
                card.setStrokeWidth(0);
                card.setCardElevation(10);
            }
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {

        ImageView img;
        android.widget.VideoView videoView;

        VH(View v) {
            super(v);
            img = v.findViewById(R.id.img);
            videoView = v.findViewById(R.id.videoView);
        }
    }
}
