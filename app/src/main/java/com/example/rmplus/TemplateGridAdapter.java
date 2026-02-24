package com.example.rmplus;

import android.net.Uri;
import android.view.*;
import android.widget.ImageView;

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
                        .inflate(R.layout.item_template_grid, p, false)
        );
    }

    @Override
    public void onBindViewHolder(Holder h, int i) {

        TemplateModel template = list.get(i);

        // 🔥 Load from URL or local automatically
        // ⭐ UNIVERSAL IMAGE LOADING (VPS + Local + Uri)
        Glide.with(h.img.getContext())
                .load(template.url)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(h.img);

        // Image will now respect layout params and adjustViewBounds from XML
 
        h.itemView.setOnClickListener(v ->
                listener.onClick(list.get(i)));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class Holder extends RecyclerView.ViewHolder {

        ImageView img;

        Holder(View v) {
            super(v);
            img = v.findViewById(R.id.imgTemplate);
        }
    }
}
