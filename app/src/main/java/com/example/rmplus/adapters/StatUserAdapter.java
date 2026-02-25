package com.example.rmplus.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rmplus.R;
import com.example.rmplus.models.StatUserItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class StatUserAdapter
        extends RecyclerView.Adapter<StatUserAdapter.ViewHolder> {

    private final ArrayList<StatUserItem> list;

    public StatUserAdapter(ArrayList<StatUserItem> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_stat_user, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {

        StatUserItem item = list.get(position);

        // Safety
        holder.txtName.setText(
                item.name == null ? holder.itemView.getContext().getString(R.string.unknown_user) : item.name);

        holder.txtEmail.setText(
                item.email == null ? holder.itemView.getContext().getString(R.string.no_email) : item.email);

        // Time formatting
        if (item.time > 0) {
            String formattedTime = new SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    Locale.getDefault()
            ).format(new Date(item.time));

            holder.txtTime.setText(formattedTime);
        } else {
            holder.txtTime.setText(holder.itemView.getContext().getString(R.string.unknown_time));
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // ---------------- VIEW HOLDER ----------------

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtName, txtEmail, txtTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtName = itemView.findViewById(R.id.txtName);
            txtEmail = itemView.findViewById(R.id.txtEmail);
            txtTime = itemView.findViewById(R.id.txtTime);
        }
    }
}
