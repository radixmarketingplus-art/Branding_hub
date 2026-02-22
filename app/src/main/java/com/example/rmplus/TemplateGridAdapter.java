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
        void onClick(String path);
    }

    ArrayList<String> list;
    ClickListener listener;

    public TemplateGridAdapter(ArrayList<String> list,
                               ClickListener l) {
        this.list = list;
        listener = l;
    }

    public void setData(ArrayList<String> newList) {
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

        File f = new File(list.get(i));

        h.img.setImageURI(Uri.fromFile(f)); // same as pehle

        // 🔳 square image (CORRECT)
        h.img.post(() -> {
            int width = h.img.getWidth();
            ViewGroup.LayoutParams params = h.img.getLayoutParams();
            params.height = width;
            h.img.setLayoutParams(params);
        });

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
