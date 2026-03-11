package com.example.rmplus.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rmplus.ApproveRejectActivity;
import com.example.rmplus.R;
import com.example.rmplus.models.SubscriptionRequest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class SubscriptionRequestAdapter extends RecyclerView.Adapter<SubscriptionRequestAdapter.Holder> {

    private ArrayList<SubscriptionRequest> list;
    private Context context;

    public SubscriptionRequestAdapter(ArrayList<SubscriptionRequest> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(context).inflate(R.layout.row_request, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        SubscriptionRequest r = list.get(position);

        // 1. SET TYPE & ICON (Subscription Icon)
        h.txtRequestType.setText(context.getString(R.string.menu_subscription));
        h.imgIcon.setImageResource(R.drawable.ic_profile); // Or a subscription specific icon if available
        h.imgIcon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E8F5E9"))); // Light Green tint

        // 2. SET NAME & DETAILS
        String localizedPlan = getLocalizedPlanName(r.plan, context);
        String details = r.name + " - " + localizedPlan;
        if (r.amount != null) details += "\n" + context.getString(R.string.label_amount_format, r.amount);
        if (r.discountPrice != null && !r.discountPrice.equals("0")) details += " " + context.getString(R.string.label_disc_format, r.discountPrice);
        h.title.setText(details);

        // 3. SET DATE/TIME
        String timeStr = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(r.time));
        h.txtTime.setText(timeStr);

        // 4. SET STATUS WITH COLOR
        String displayStatus = r.status;
        int statusColor = android.graphics.Color.parseColor("#F59E0B"); // Pending

        if ("pending".equalsIgnoreCase(r.status)) {
            displayStatus = context.getString(R.string.tab_pending);
            statusColor = android.graphics.Color.parseColor("#F59E0B");
        } else if ("approved".equalsIgnoreCase(r.status)) {
            displayStatus = context.getString(R.string.tab_accepted);
            statusColor = android.graphics.Color.parseColor("#10B981");
        } else if ("rejected".equalsIgnoreCase(r.status)) {
            displayStatus = context.getString(R.string.tab_rejected);
            statusColor = android.graphics.Color.parseColor("#EF4444");
        }

        h.status.setText(displayStatus);
        h.status.setTextColor(statusColor);

        h.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ApproveRejectActivity.class);
            intent.putExtra("uid", r.uid);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private String getLocalizedPlanName(String canonical, Context context) {
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

    static class Holder extends RecyclerView.ViewHolder {
        TextView title, status, txtRequestType, txtTime;
        ImageView imgIcon;

        Holder(View v) {
            super(v);
            title = v.findViewById(R.id.txtTitle);
            status = v.findViewById(R.id.txtStatus);
            txtRequestType = v.findViewById(R.id.txtRequestType);
            txtTime = v.findViewById(R.id.txtTime);
            imgIcon = v.findViewById(R.id.imgIcon);
        }
    }
}
