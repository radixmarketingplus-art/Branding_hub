package com.example.rmplus;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.util.ArrayList;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.VH> {

    private ArrayList<String> uriList;      // Admin images
    private ArrayList<Integer> resList;     // Default images
    private int layout;
    private boolean isUri;                  // 🔥 TYPE FLAG
    private String category;

    // ✅ SINGLE CONSTRUCTOR (NO ERASURE ISSUE)

    public ImageAdapter(ArrayList<String> uriList,
                        ArrayList<Integer> resList,
                        boolean isUri,
                        int layout,
                        String category)
    {
        this.uriList = uriList;
        this.resList = resList;
        this.isUri = isUri;
        this.layout = layout;
        this.category = category;
    }


    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(layout, parent, false);
        return new VH(v);
    }


    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {

        // 🔥 RecyclerView reuse fix
        holder.img.setImageDrawable(null);
        holder.img.setImageBitmap(null);

        try {
            if (isUri) {
                String item = uriList.get(position);

                // Dummy card
                if ("DUMMY".equals(item)) {
                    holder.img.setImageResource(R.drawable.ic_add);
                }
                // ✅ REAL IMAGE (FILE PATH)
                else {
                    Bitmap bitmap = BitmapFactory.decodeFile(item);
                    if (bitmap != null) {
                        holder.img.setImageBitmap(bitmap);
                    } else {
                        holder.img.setImageResource(R.drawable.ic_launcher_foreground);
                    }
                }

            } else {
                holder.img.setImageResource(resList.get(position));
            }

        } catch (Exception e) {
            holder.img.setImageResource(R.drawable.ic_launcher_foreground);
        }

        // ---------- CLICK ----------
        holder.img.setOnClickListener(v -> {

            // Dummy → Admin upload
            if (isUri && "DUMMY".equals(uriList.get(position))) {
                v.getContext().startActivity(
                        new Intent(v.getContext(), UploadTemplatesActivity.class)
                );
                return;
            }

            // 🔥 BUSINESS FRAME → DIRECT EDITOR
            if ("Business Frame".equals(category)) {

                Intent i = new Intent(v.getContext(), ManageTemplatesActivity.class);

                // main template image
                i.putExtra("uri", uriList.get(position));

                // 🔥 frame list key (HomeActivity se match karega)
                i.putExtra("frames_key", "Business Frame");

                v.getContext().startActivity(i);
                return;
            }

            // 🔁 ALL OTHER CATEGORIES (OLD FLOW SAFE)
            Intent i = new Intent(v.getContext(), TemplatePreviewActivity.class);

            if (isUri) {
                i.putExtra("path", uriList.get(position));
                i.putExtra("category", category);
            } else {
                i.putExtra("res", resList.get(position));
            }

            v.getContext().startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return isUri ? uriList.size() : resList.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        VH(View v) {
            super(v);
            img = v.findViewById(R.id.img);
        }
    }
}