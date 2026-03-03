package com.example.rmplus;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.imageview.ShapeableImageView;
import android.widget.ImageView;

import java.util.ArrayList;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.ViewHolder> {

    private Context context;
    private ArrayList<UserItem> list;

    public UserListAdapter(Context context, ArrayList<UserItem> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserItem user = list.get(position);

        holder.txtName.setText(user.name != null ? user.name : "Unknown User");
        holder.txtEmail.setText(user.email != null ? user.email : "No Email");

        if (user.profileImage != null && !user.profileImage.isEmpty()) {
            holder.imgUser.setImageTintList(null);
            Glide.with(context)
                    .load(user.profileImage)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.imgUser);
        } else {
            holder.imgUser.setImageResource(R.drawable.ic_profile);
            holder.imgUser.setImageTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary)));
        }

        // ROLE LOGIC
        String role = user.role != null ? user.role.toLowerCase() : "user";
        if (role.equals("admin")) {
            holder.roleBadge.setText("ADMIN");
            holder.roleBadge.setBackgroundResource(R.drawable.bg_role_admin);
            holder.roleStrip.setBackgroundColor(ContextCompat.getColor(context, R.color.primary));
        } else {
            holder.roleBadge.setText("USER");
            holder.roleBadge.setBackgroundResource(R.drawable.bg_role_user);
            holder.roleStrip.setBackgroundColor(ContextCompat.getColor(context, R.color.darker_gray));
        }

        holder.txtMobile.setText(user.mobile != null ? user.mobile : "No Mobile");

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, UserDetailActivity.class);
            intent.putExtra("uid", user.uid);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void updateList(ArrayList<UserItem> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView imgUser;
        TextView txtName, txtEmail, txtMobile, roleBadge;
        View roleStrip;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgUser = itemView.findViewById(R.id.imgUser);
            txtName = itemView.findViewById(R.id.txtName);
            txtEmail = itemView.findViewById(R.id.txtEmail);
            txtMobile = itemView.findViewById(R.id.txtMobile);
            roleBadge = itemView.findViewById(R.id.roleBadge);
            roleStrip = itemView.findViewById(R.id.roleStrip);
        }
    }
}
