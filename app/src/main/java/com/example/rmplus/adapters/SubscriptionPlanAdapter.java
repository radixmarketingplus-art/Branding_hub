package com.example.rmplus.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rmplus.R;
import com.example.rmplus.models.SubscriptionPlan;
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
        holder.duration.setText(p.duration);
        holder.amount.setText("₹" + p.amount);
        if (p.discountPrice != null && !p.discountPrice.equals("0")) {
            holder.discount.setText("Discount: ₹" + p.discountPrice);
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
