package com.example.rmplus;

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

    public TemplateGridAdapter(ArrayList<TemplateModel> list,
                               ClickListener l) {
        this.list = list;
        listener = l;
    }

    public void setData(ArrayList<TemplateModel> newList) {
        list = newList;
        notifyDataSetChanged();
    }


    @Override
    public Holder onCreateViewHolder(ViewGroup p, int v) {
        return new Holder(
                LayoutInflater.from(p.getContext())
                        .inflate(R.layout.item_square, p, false)
        );
    }

    @Override
    public void onBindViewHolder(Holder h, int i) {

        TemplateModel template = list.get(i);

        // 🔥 Load from URL or local automatically
        Glide.with(h.img.getContext())
                .load(template.url)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(h.img);

        // 📅 SHOW DATE IF AVAILABLE (For Festival Cards)
        if (template.date != null && !template.date.isEmpty()) {
            h.txtDateBadge.setVisibility(View.VISIBLE);
            h.txtDateBadge.setText(formatDate(template.date));
        } else {
            h.txtDateBadge.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v ->
                listener.onClick(list.get(i)));
    }

    private String formatDate(String d) {
        try {
            java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("d-M-yyyy", java.util.Locale.US); // ✅ parse ASCII stored dates
            java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault()); // display in user language
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
        TextView txtDateBadge;

        Holder(View v) {
            super(v);
            img = v.findViewById(R.id.img);
            txtDateBadge = v.findViewById(R.id.txtDateBadge);
        }
    }
}
