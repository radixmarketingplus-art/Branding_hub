package com.example.rmplus.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rmplus.ImagePreviewActivity;
import com.example.rmplus.R;
import com.example.rmplus.models.ChatMessage;
import java.util.ArrayList;
import android.content.Intent;
import android.widget.ImageView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.Holder> {

    ArrayList<ChatMessage> list;
    String myUid;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public ChatAdapter(ArrayList<ChatMessage> list, String myUid) {
        this.list = list;
        this.myUid = (myUid != null) ? myUid.trim() : "";
    }

    @Override
    public int getItemViewType(int position) {
        // Robust comparison: If senderId matches current user's UID, it's a "Sent"
        // message (Right Side)
        String senderId = list.get(position).senderId;
        if (senderId != null && senderId.trim().equalsIgnoreCase(myUid)) {
            return 1; // RIGHT SIDE (ME)
        } else {
            return 0; // LEFT SIDE (OTHER/ADMIN)
        }
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == 1) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_chat_right, parent, false));
        } else {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_chat_left, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(Holder h, int position) {
        ChatMessage m = list.get(position);

        // Reset visibility
        h.txtMsg.setVisibility(View.GONE);
        if (h.cardImgChat != null)
            h.cardImgChat.setVisibility(View.GONE);
        else
            h.imgChat.setVisibility(View.GONE);

        // Set Time
        if (h.txtTime != null) {
            h.txtTime.setVisibility(View.VISIBLE);
            h.txtTime.setText(timeFormat.format(new Date(m.time)));
        }

        // TEXT MESSAGE
        if (m.message != null && !m.message.trim().isEmpty()) {
            h.txtMsg.setVisibility(View.VISIBLE);
            h.txtMsg.setText(m.message);
        }

        // IMAGE MESSAGE
        else if (m.imageUrl != null && !m.imageUrl.isEmpty()) {
            if (h.cardImgChat != null)
                h.cardImgChat.setVisibility(View.VISIBLE);
            h.imgChat.setVisibility(View.VISIBLE);
            loadImageFromUrl(m.imageUrl, h.imgChat);

            h.imgChat.setOnClickListener(v -> {
                Intent i = new Intent(v.getContext(), ImagePreviewActivity.class);
                i.putExtra("img", m.imageUrl);
                v.getContext().startActivity(i);
            });
        }

        // Distinguish Admin messages visually (Optional: add a tag or change color)
        if (h.itemView.findViewById(R.id.supportTag) != null) {
            boolean isAdmin = m.senderId != null && m.senderId.equalsIgnoreCase("ADMIN");
            h.itemView.findViewById(R.id.supportTag).setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        }
    }

    private void loadImageFromUrl(String url, ImageView imageView) {
        new Thread(() -> {
            try {
                java.net.URL u = new java.net.URL(url);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) u.openConnection();
                conn.setDoInput(true);
                conn.connect();
                java.io.InputStream input = conn.getInputStream();
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(input);
                imageView.post(() -> imageView.setImageBitmap(bitmap));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class Holder extends RecyclerView.ViewHolder {
        TextView txtMsg, txtTime;
        ImageView imgChat;
        View cardImgChat;

        Holder(View v) {
            super(v);
            txtMsg = v.findViewById(R.id.txtMsg);
            txtTime = v.findViewById(R.id.txtTime);
            imgChat = v.findViewById(R.id.imgChat);
            cardImgChat = v.findViewById(R.id.cardImgChat);
        }
    }
}
