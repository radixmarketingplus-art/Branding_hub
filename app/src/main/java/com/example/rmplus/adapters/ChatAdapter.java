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

import android.view.View;
import android.content.Intent;
import android.net.Uri;
import android.widget.ImageView;
import java.io.File;

public class ChatAdapter
        extends RecyclerView.Adapter<ChatAdapter.Holder> {

    ArrayList<ChatMessage> list;
    String myUid;

    public ChatAdapter(ArrayList<ChatMessage> list,
                       String myUid) {
        this.list = list;
        this.myUid = myUid;
    }

    @Override
    public int getItemViewType(int position) {
        if (list.get(position).senderId.equals(myUid))
            return 1;
        else
            return 0;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent,
                                     int viewType) {

        if (viewType == 1) {
            return new Holder(
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.row_chat_right,
                                    parent, false)
            );
        } else {
            return new Holder(
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.row_chat_left,
                                    parent, false)
            );
        }
    }

    @Override
    public void onBindViewHolder(Holder h, int position) {

        ChatMessage m = list.get(position);

        // TEXT MESSAGE
        if (m.message != null && !m.message.isEmpty()) {

            h.txtMsg.setVisibility(View.VISIBLE);
            h.imgChat.setVisibility(View.GONE);
            h.txtMsg.setText(m.message);
        }

        // IMAGE MESSAGE
        else if (m.imageUrl != null && !m.imageUrl.isEmpty()) {

            h.txtMsg.setVisibility(View.GONE);
            h.imgChat.setVisibility(View.VISIBLE);

            loadImageFromUrl(m.imageUrl, h.imgChat);


            // Fullscreen on click
            h.imgChat.setOnClickListener(v -> {

                Intent i = new Intent(
                        v.getContext(),
                        ImagePreviewActivity.class
                );
                i.putExtra("img", m.imageUrl);
                v.getContext().startActivity(i);

            });
        }
    }

    private void loadImageFromUrl(String url, ImageView imageView) {

        new Thread(() -> {
            try {

                java.net.URL u = new java.net.URL(url);
                java.net.HttpURLConnection conn =
                        (java.net.HttpURLConnection) u.openConnection();

                conn.setDoInput(true);
                conn.connect();

                java.io.InputStream input = conn.getInputStream();

                android.graphics.Bitmap bitmap =
                        android.graphics.BitmapFactory.decodeStream(input);

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

        TextView txtMsg;
        ImageView imgChat;

        Holder(View v) {
            super(v);
            txtMsg = v.findViewById(R.id.txtMsg);
            imgChat = v.findViewById(R.id.imgChat);
        }
    }
}
