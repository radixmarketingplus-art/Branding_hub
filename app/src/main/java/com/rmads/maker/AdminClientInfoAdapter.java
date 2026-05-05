package com.rmads.maker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rmads.maker.models.ClientInfoRequest;

import java.util.List;

public class AdminClientInfoAdapter extends RecyclerView.Adapter<AdminClientInfoAdapter.ViewHolder> {

    private final Context context;
    private final List<ClientInfoRequest> list;
    private final DatabaseReference ref;
    private final boolean isAdmin;

    public AdminClientInfoAdapter(Context context, List<ClientInfoRequest> list, boolean isAdmin) {
        this.context = context;
        this.list = list;
        this.isAdmin = isAdmin;
        this.ref = FirebaseDatabase.getInstance().getReference("client_info_requests");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_client_info, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClientInfoRequest req = list.get(position);

        holder.tvName.setText(req.name + " (" + req.contact + ")");
        holder.tvBusinessName.setText(req.businessName + " | " + req.businessCategory);
        String localizedService = req.serviceType;
        if ("Digital Application".equals(req.serviceType)) localizedService = context.getString(R.string.opt_digital_app);
        else if ("Website Development".equals(req.serviceType)) localizedService = context.getString(R.string.opt_web_dev);
        else if ("App Development".equals(req.serviceType)) localizedService = context.getString(R.string.opt_app_dev);
        else if ("Graphic Post Design".equals(req.serviceType)) localizedService = context.getString(R.string.opt_graphic_design);
        else if ("Other Services".equals(req.serviceType)) localizedService = context.getString(R.string.opt_other_services);

        holder.tvServiceType.setText(localizedService);

        String emailLabel = context.getString(R.string.ci_hint_email);
        String descLabel = context.getString(R.string.ci_hint_description);
        String remarkLabel = context.getString(R.string.hint_remark);

        StringBuilder details = new StringBuilder();
        details.append(emailLabel).append(": ").append(req.email);
        if (!req.description.isEmpty()) details.append("\n").append(descLabel).append(": ").append(req.description);
        if (!req.remark.isEmpty()) details.append("\n").append(remarkLabel).append(": ").append(req.remark);

        holder.tvDetails.setText(details.toString());

        if (!isAdmin) {
            holder.btnAccept.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
            holder.tvStatusLabel.setVisibility(View.VISIBLE);
            
            String status = req.status != null ? req.status : "pending";
            holder.tvStatusLabel.setText(((BaseActivity)context).getLocalizedStatus(status));
            
            if (status.equalsIgnoreCase("accepted")) {
                holder.tvStatusLabel.setTextColor(context.getColor(android.R.color.holo_green_dark));
                holder.tvStatusLabel.setBackgroundResource(R.drawable.bg_status_badge_green);
            } else if (status.equalsIgnoreCase("rejected")) {
                holder.tvStatusLabel.setTextColor(context.getColor(android.R.color.holo_red_dark));
                holder.tvStatusLabel.setBackgroundResource(R.drawable.bg_status_badge_red);
            } else {
                holder.tvStatusLabel.setTextColor(context.getColor(android.R.color.holo_orange_dark));
                holder.tvStatusLabel.setBackgroundResource(R.drawable.bg_status_badge_orange);
            }
        } else {
            holder.tvStatusLabel.setVisibility(View.GONE);
            holder.btnAccept.setVisibility(View.VISIBLE);
            holder.btnReject.setVisibility(View.VISIBLE);

            if (req.status != null && req.status.equals("accepted")) {
                holder.btnAccept.setText(context.getString(R.string.btn_accepted));
                holder.btnAccept.setEnabled(false);
                holder.btnAccept.setAlpha(0.6f);
                holder.btnReject.setVisibility(View.GONE);
            } else if (req.status != null && req.status.equals("rejected")) {
                holder.btnReject.setText(context.getString(R.string.btn_rejected));
                holder.btnReject.setEnabled(false);
                holder.btnReject.setAlpha(0.6f);
                holder.btnAccept.setVisibility(View.GONE);
            } else {
                holder.btnAccept.setText(context.getString(R.string.btn_accept));
                holder.btnAccept.setEnabled(true);
                holder.btnAccept.setAlpha(1.0f);
                
                holder.btnReject.setText(context.getString(R.string.btn_reject));
                holder.btnReject.setEnabled(true);
                holder.btnReject.setAlpha(1.0f);
            }

            holder.btnReject.setOnClickListener(v -> rejectRequest(position, req));
            holder.btnAccept.setOnClickListener(v -> acceptRequest(position, req));
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private void acceptRequest(int position, ClientInfoRequest req) {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.title_action_required))
                .setMessage(context.getString(R.string.msg_accept_confirm_ci))
                .setPositiveButton(context.getString(R.string.btn_yes), (dialog, which) -> {
                    ref.child(req.requestId).child("status").setValue("accepted")
                            .addOnSuccessListener(aVoid -> {
                                req.status = "accepted";
                                notifyItemChanged(position);
                                Toast.makeText(context, context.getString(R.string.msg_request_accepted_ci), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton(context.getString(R.string.btn_no), null)
                .show();
    }

    private void rejectRequest(int position, ClientInfoRequest req) {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.title_action_required))
                .setMessage(context.getString(R.string.msg_reject_confirm_ci))
                .setPositiveButton(context.getString(R.string.btn_yes), (dialog, which) -> {
                    ref.child(req.requestId).child("status").setValue("rejected")
                            .addOnSuccessListener(aVoid -> {
                                req.status = "rejected";
                                notifyItemChanged(position);
                                Toast.makeText(context, context.getString(R.string.msg_request_rejected_ci), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton(context.getString(R.string.btn_no), null)
                .show();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvBusinessName, tvServiceType, tvDetails, tvStatusLabel;
        Button btnAccept, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvBusinessName = itemView.findViewById(R.id.tvBusinessName);
            tvServiceType = itemView.findViewById(R.id.tvServiceType);
            tvDetails = itemView.findViewById(R.id.tvDetails);
            tvStatusLabel = itemView.findViewById(R.id.tvStatusLabel);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
