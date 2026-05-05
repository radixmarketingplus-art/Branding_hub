package com.rmads.maker;

import android.net.Uri;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;

public class TemplateGridAdapter
        extends RecyclerView.Adapter<TemplateGridAdapter.Holder> {

    public interface ClickListener {
        void onClick(TemplateModel template);
    }

    ArrayList<TemplateModel> list;
    ClickListener listener;
    String highlightId = null;
    int layoutRes = R.layout.item_square;

    public TemplateGridAdapter(ArrayList<TemplateModel> list,
            ClickListener l) {
        this.list = list;
        listener = l;
    }

    public TemplateGridAdapter(ArrayList<TemplateModel> list, int layoutRes, ClickListener l) {
        this.list = list;
        this.layoutRes = layoutRes;
        this.listener = l;
    }

    public void setHighlightId(String id) {
        this.highlightId = id;
        notifyDataSetChanged();
    }

    public void setData(ArrayList<TemplateModel> newList) {
        list = newList;
        notifyDataSetChanged();
    }

    boolean showDate = true;

    public void setShowDate(boolean show) {
        this.showDate = show;
        notifyDataSetChanged();
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup p, int v) {
        return new Holder(
                LayoutInflater.from(p.getContext())
                        .inflate(layoutRes, p, false));
    }

    @Override
    public void onBindViewHolder(Holder h, int i) {

        TemplateModel template = list.get(i);

        // 🔥 Load from URL or local automatically
        Glide.with(h.img.getContext())
                .load(template.url)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .centerCrop()
                .into(h.img);

        // 📅 SHOW DATE IF AVAILABLE (For Festival Cards)
        if (showDate && template.date != null && !template.date.isEmpty()) {
            h.txtDateBadge.setVisibility(View.VISIBLE);
            h.txtDateBadge.setText(formatDate(template.date));
        } else {
            h.txtDateBadge.setVisibility(View.GONE);
        }

        // 🎬 PREMIUM VIDEO UI
        // 🎬 IMPROVED VIDEO DETECTION (Handles MyDesign + Firebase URLs)
        String lowerUrl = template.url != null ? template.url.toLowerCase() : "";
        String cleanUrl = lowerUrl.split("\\?")[0];
        boolean isExtensionVideo = cleanUrl.endsWith(".mp4") || cleanUrl.endsWith(".webm") || cleanUrl.endsWith(".mkv");
        boolean isPathVideo = lowerUrl.contains("/reel maker/") || lowerUrl.contains("/reel%20maker/");
        
        boolean isVideo = "video".equalsIgnoreCase(template.type) || 
                         (template.category != null && template.category.equalsIgnoreCase("Reel Maker")) ||
                         isExtensionVideo || isPathVideo;
        
        if (isVideo) {
            h.layPlay.setVisibility(View.VISIBLE);
        } else {
            h.layPlay.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> listener.onClick(list.get(i)));

        // ✨ HIGHLIGHT LOGIC (Blue Shadow/Border)
        if (h.itemView instanceof com.google.android.material.card.MaterialCardView) {
            com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) h.itemView;
            float dp10 = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 10,
                    h.itemView.getResources().getDisplayMetrics());
            float dp20 = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 20,
                    h.itemView.getResources().getDisplayMetrics());

            if (highlightId != null && highlightId.equals(template.id)) {
                card.setStrokeColor(android.graphics.Color.parseColor("#4A6CF7")); // Blue highlight
                card.setStrokeWidth(12); // Thick highlight
                card.setCardElevation(dp20); // Extra shadow
            } else {
                float dp2 = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 2, h.itemView.getResources().getDisplayMetrics());
                float dp5 = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 5, h.itemView.getResources().getDisplayMetrics());
                card.setStrokeColor(android.graphics.Color.TRANSPARENT);
                card.setStrokeWidth(0);
                card.setCardElevation(dp5); // 5dp default shadow
            }
        }
    }

    private String formatDate(String d) {
        try {
            java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("d-M-yyyy", java.util.Locale.US); // ✅ parse
                                                                                                             // ASCII
                                                                                                             // stored
                                                                                                             // dates
            java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault()); // display
                                                                                                                      // in
                                                                                                                      // user
                                                                                                                      // language
            return out.format(in.parse(d));
        } catch (Exception e) {
            return d;
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class Holder extends RecyclerView.ViewHolder {

        ImageView img;
        View layPlay;
        TextView txtDateBadge;

        Holder(View v) {
            super(v);
            img = v.findViewById(R.id.img);
            layPlay = v.findViewById(R.id.layPlay);
            txtDateBadge = v.findViewById(R.id.txtDateBadge);
        }
    }
}
