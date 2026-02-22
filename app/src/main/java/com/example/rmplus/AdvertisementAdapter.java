package com.example.rmplus;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.*;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;

public class AdvertisementAdapter
        extends RecyclerView.Adapter<AdvertisementAdapter.VH> {

    ArrayList<AdvertisementItem> list;

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

        // Show image
        h.img.setImageURI(Uri.fromFile(new File(item.imagePath)));

        // 🔥 CLICK → OPEN LINK IN BROWSER
        h.itemView.setOnClickListener(v -> {

            Context ctx = v.getContext();

            Intent i = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(item.link)
            );

            ctx.startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {

        ImageView img;

        VH(View v) {
            super(v);
            img = v.findViewById(R.id.img);
        }
    }
}
