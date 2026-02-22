package com.example.rmplus;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;

public class FrameAdapter
        extends RecyclerView.Adapter<FrameAdapter.VH> {

    public interface OnFrameClick {
        void onSelect(String path);
    }

    ArrayList<String> list;
    OnFrameClick listener;

    public FrameAdapter(ArrayList<String> list, OnFrameClick listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView img = new ImageView(parent.getContext());
        img.setLayoutParams(
                new ViewGroup.LayoutParams(180, 180)
        );
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setPadding(8,8,8,8);
        return new VH(img);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        String path = list.get(pos);

        h.img.setImageURI(
                android.net.Uri.fromFile(new File(path))
        );

        h.img.setOnClickListener(v -> {
            if (listener != null)
                listener.onSelect(path);
        });
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        VH(View v) {
            super(v);
            img = (ImageView) v;
        }
    }
}