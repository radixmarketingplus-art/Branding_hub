package com.example.rmplus;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class FestivalDateAdapter
        extends RecyclerView.Adapter<FestivalDateAdapter.VH> {

    ArrayList<Calendar> dates;
    OnDateClickListener listener;
    int selectedPosition = RecyclerView.NO_POSITION;

//    int todayIndex = -1;

    public interface OnDateClickListener {
        void onDateSelected(String date);
    }

    public FestivalDateAdapter(ArrayList<Calendar> dates,
                               OnDateClickListener listener) {
        this.dates = dates;
        this.listener = listener;

//        Calendar today = Calendar.getInstance();
//
//        for (int i = 0; i < dates.size(); i++) {
//            Calendar c = dates.get(i);
//            if (c == null) continue;
//
//            if (c.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
//                    && c.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
//                todayIndex = i;
//                break;
//            }
//        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_festival_date, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {

        Calendar c = dates.get(pos);

        // 🔥 CLEAR FILTER CHIP
        if (c == null) {
            h.day.setText("All");
            h.month.setText("");
            h.itemView.setBackgroundResource(R.drawable.bg_date_normal);

            h.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDateSelected("CLEAR");
                }
            });
            return;
        }

        h.day.setText(String.valueOf(c.get(Calendar.DAY_OF_MONTH)));
        h.month.setText(
                new SimpleDateFormat("MMM", Locale.getDefault())
                        .format(c.getTime())
        );

//        if (pos == todayIndex) {
//            h.itemView.setBackgroundResource(R.drawable.bg_date_today);
//        } else {
//            h.itemView.setBackgroundResource(R.drawable.bg_date_normal);
//        }

        if (pos == selectedPosition) {
            h.itemView.setBackgroundResource(R.drawable.bg_date_today);
        } else {
            h.itemView.setBackgroundResource(R.drawable.bg_date_normal);
        }

//        h.itemView.setOnClickListener(v -> {
//            SimpleDateFormat sdf =
//                    new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
//
//            if (listener != null) {
//                listener.onDateSelected(sdf.format(c.getTime()));
//            }
//        });

        h.itemView.setOnClickListener(v -> {

            int oldPos = selectedPosition;
            selectedPosition = pos;

            if (oldPos != RecyclerView.NO_POSITION) {
                notifyItemChanged(oldPos);
            }
            notifyItemChanged(selectedPosition);

            SimpleDateFormat sdf =
                    new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

            if (listener != null) {
                listener.onDateSelected(sdf.format(c.getTime()));
            }
        });

    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView day, month;

        VH(View v) {
            super(v);
            day = v.findViewById(R.id.txtDay);
            month = v.findViewById(R.id.txtMonth);
        }
    }

    public void clearSelection() {
        int oldPos = selectedPosition;
        selectedPosition = RecyclerView.NO_POSITION;

        if (oldPos != RecyclerView.NO_POSITION) {
            notifyItemChanged(oldPos);
        }
    }
}