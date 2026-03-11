package com.example.rmplus.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rmplus.R;
import com.example.rmplus.models.SubscriptionPlan;

import java.util.ArrayList;

public class UserSubscriptionPlanAdapter extends RecyclerView.Adapter<UserSubscriptionPlanAdapter.ViewHolder> {

    private ArrayList<SubscriptionPlan> list;
    private int selectedPos = -1;
    private OnPlanSelectedListener listener;

    public interface OnPlanSelectedListener {
        void onPlanSelected(SubscriptionPlan plan);
    }

    public UserSubscriptionPlanAdapter(ArrayList<SubscriptionPlan> list, OnPlanSelectedListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_membership_plan_user, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SubscriptionPlan p = list.get(position);

        holder.duration.setText(getLocalizedDuration(p.duration, holder.itemView.getContext()));
        holder.finalPrice.setText("₹" + p.amount);

        try {
            double amount = Double.parseDouble(p.amount);
            double discount = (p.discountPrice != null && !p.discountPrice.isEmpty()) ? Double.parseDouble(p.discountPrice) : 0;

            if (discount > 0) {
                double original = amount + discount;
                int percent = (int) ((discount / original) * 100);

                holder.originalPrice.setText("₹" + (int)original);
                holder.originalPrice.setPaintFlags(holder.originalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                holder.originalPrice.setVisibility(View.VISIBLE);

                holder.badge.setText(holder.itemView.getContext().getString(R.string.label_save_percent_tag, percent));
                holder.badge.setVisibility(View.VISIBLE);

                holder.savings.setText(holder.itemView.getContext().getString(R.string.label_save_amount_tag, (int)discount));
                holder.savings.setVisibility(View.VISIBLE);
            } else {
                holder.originalPrice.setVisibility(View.GONE);
                holder.badge.setVisibility(View.GONE);
                holder.savings.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            holder.originalPrice.setVisibility(View.GONE);
            holder.badge.setVisibility(View.GONE);
            holder.savings.setVisibility(View.GONE);
        }

        // Selection State
        if (selectedPos == position) {
            holder.card.setStrokeColor(holder.itemView.getContext().getResources().getColor(R.color.primary));
            holder.card.setStrokeWidth(4);
            holder.ivSelected.setVisibility(View.VISIBLE);
        } else {
            holder.card.setStrokeColor(0x10000000);
            holder.card.setStrokeWidth(2);
            holder.ivSelected.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            int old = selectedPos;
            selectedPos = holder.getAdapterPosition();
            notifyItemChanged(old);
            notifyItemChanged(selectedPos);
            if (listener != null) listener.onPlanSelected(p);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public int getSelectedPosition() {
        return selectedPos;
    }

    public void setSelectedPosition(int pos) {
        if (pos >= 0 && pos < getItemCount()) {
            int old = selectedPos;
            selectedPos = pos;
            notifyItemChanged(old);
            notifyItemChanged(selectedPos);
        }
    }

    private String getLocalizedDuration(String duration, android.content.Context context) {
        if (duration == null) return "";
        String d = duration.toLowerCase();
        if (d.contains("1 month")) return context.getString(R.string.plan_1_month);
        if (d.contains("3 month")) return context.getString(R.string.plan_3_month);
        if (d.contains("6 month")) return context.getString(R.string.plan_6_month);
        if (d.contains("1 year")) return context.getString(R.string.plan_1_year);
        if (d.contains("7 days") || d.contains("custom")) return context.getString(R.string.plan_custom);
        return duration; // Fallback to DB value if no match
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView duration, originalPrice, finalPrice, badge, savings;
        com.google.android.material.card.MaterialCardView card;
        ImageView ivSelected;

        public ViewHolder(@NonNull View v) {
            super(v);
            duration = v.findViewById(R.id.planDuration);
            originalPrice = v.findViewById(R.id.originalPrice);
            finalPrice = v.findViewById(R.id.finalPrice);
            badge = v.findViewById(R.id.planBadge);
            savings = v.findViewById(R.id.savingsText);
            card = v.findViewById(R.id.cardSelection);
            ivSelected = v.findViewById(R.id.ivSelected);
        }
    }
}
