package com.rmads.maker.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rmads.maker.R;
import com.rmads.maker.models.SubscriptionPlan;
import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class SubscriptionPlanAdapter extends RecyclerView.Adapter<SubscriptionPlanAdapter.PlanViewHolder> {

    ArrayList<SubscriptionPlan> list;
    OnPlanClickListener listener;

    public interface OnPlanClickListener {
        void onEdit(SubscriptionPlan plan);
        void onDelete(SubscriptionPlan plan);
    }

    public SubscriptionPlanAdapter(ArrayList<SubscriptionPlan> list, OnPlanClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subscription_plan, parent, false);
        return new PlanViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PlanViewHolder holder, int position) {
        SubscriptionPlan p = list.get(position);
        holder.duration.setText(getLocalizedPlanName(p.duration, holder.itemView.getContext()));
        holder.amount.setText("₹" + p.amount);
        if (p.discountPrice != null && !p.discountPrice.equals("0")) {
            holder.discount.setText(holder.itemView.getContext().getString(R.string.label_discount_value, p.discountPrice));
            holder.discount.setVisibility(View.VISIBLE);
        } else {
            holder.discount.setVisibility(View.GONE);
        }

        if (p.scannerUrl != null && !p.scannerUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(p.scannerUrl)
                    .placeholder(R.drawable.ic_gallery_modern)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(holder.scanner);
        }

        holder.itemView.setOnClickListener(v -> listener.onEdit(p));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(p));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private String getLocalizedPlanName(String canonical, android.content.Context context) {
        if (canonical == null) return "";
        String c = canonical.toLowerCase();
        if (c.contains("silver")) return context.getString(R.string.plan_silver);
        if (c.contains("gold")) return context.getString(R.string.plan_gold);
        if (c.contains("diamond")) return context.getString(R.string.plan_diamond);
        if (c.contains("custom") || c.contains("7 days")) return context.getString(R.string.plan_custom);
        if (c.contains("1 month")) return context.getString(R.string.plan_1_month);
        if (c.contains("3 month")) return context.getString(R.string.plan_3_month);
        if (c.contains("6 month")) return context.getString(R.string.plan_6_month);
        if (c.contains("1 year")) return context.getString(R.string.plan_1_year);
        return canonical;
    }

    static class PlanViewHolder extends RecyclerView.ViewHolder {
        TextView duration, amount, discount;
        ImageView scanner, btnDelete;

        public PlanViewHolder(@NonNull View v) {
            super(v);
            duration = v.findViewById(R.id.planDuration);
            amount = v.findViewById(R.id.planAmount);
            discount = v.findViewById(R.id.planDiscount);
            scanner = v.findViewById(R.id.planScanner);
            btnDelete = v.findViewById(R.id.btnDeletePlan);
        }
    }
}
