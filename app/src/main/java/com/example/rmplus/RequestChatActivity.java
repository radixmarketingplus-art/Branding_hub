package com.example.rmplus;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;

import com.example.rmplus.adapters.ChatAdapter;
import com.example.rmplus.models.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class RequestChatActivity extends AppCompatActivity {

    RecyclerView recycler;
    EditText etMsg;
    ImageButton btnAttach;
    ImageButton btnSend;

    String requestId;
    String myUid;

    ImageView imgPreview;
    Uri selectedImageUri;

    ArrayList<ChatMessage> list = new ArrayList<>();
    ChatAdapter adapter;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_chat);

        requestId = getIntent().getStringExtra("id");

        if (requestId == null) {
            Toast.makeText(this,
                    "Chat cannot open",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        myUid = FirebaseAuth.getInstance().getUid();

        imgPreview = findViewById(R.id.imgPreview);
        imgPreview.setVisibility(ImageView.GONE);
        recycler = findViewById(R.id.recyclerChat);
        etMsg = findViewById(R.id.etMsg);
        btnSend = findViewById(R.id.btnSend);
        btnAttach = findViewById(R.id.btnAttach);

        recycler.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter = new ChatAdapter(list, myUid);
        recycler.setAdapter(adapter);

        loadMessages();

        imgPreview.setOnClickListener(v -> {

            if (selectedImageUri != null) {
                Intent i = new Intent(
                        RequestChatActivity.this,
                        ImagePreviewActivity.class
                );
                i.putExtra("img", selectedImageUri.toString());
                startActivity(i);
            }

        });


        btnSend.setOnClickListener(v -> {

            if (selectedImageUri != null) {
                sendImage();
            } else {
                sendText();
            }

        });

        btnAttach.setOnClickListener(v -> pickImage());
    }

    String copyImageToLocal(Uri uri) {

        try {

            File folder = new File(getFilesDir(), "chat_images");

            if (!folder.exists())
                folder.mkdir();

            File file = new File(
                    folder,
                    System.currentTimeMillis() + ".jpg"
            );

            InputStream in =
                    getContentResolver().openInputStream(uri);

            OutputStream out =
                    new FileOutputStream(file);

            byte[] buf = new byte[1024];
            int len;

            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            in.close();
            out.close();

            return file.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }


    void loadMessages() {

        FirebaseDatabase.getInstance()
                .getReference("request_chats")
                .child(requestId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        list.clear();

                        for (DataSnapshot d : snapshot.getChildren()) {

                            ChatMessage m =
                                    d.getValue(ChatMessage.class);

                            if (m != null &&
                                    m.senderId != null) {

                                list.add(m);
                            }
                        }

                        adapter.notifyDataSetChanged();
                        recycler.scrollToPosition(
                                list.size() - 1
                        );
                    }

                    @Override
                    public void onCancelled(DatabaseError error) { }
                });
    }

    void sendText() {

        String txt = etMsg.getText().toString().trim();

        if (txt.isEmpty()) return;

        String id = FirebaseDatabase.getInstance()
                .getReference("request_chats")
                .child(requestId)
                .push()
                .getKey();

        ChatMessage m = new ChatMessage();
        m.senderId = myUid;
        m.message = txt;
        m.time = System.currentTimeMillis();
        m.seen = false;

        FirebaseDatabase.getInstance()
                .getReference("request_chats")
                .child(requestId)
                .child(id)
                .setValue(m);

        etMsg.setText("");
    }

    void sendImage() {

        String localPath =
                copyImageToLocal(selectedImageUri);

        if (localPath.isEmpty()) {
            Toast.makeText(this,
                    "Image send failed",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String id = FirebaseDatabase.getInstance()
                .getReference("request_chats")
                .child(requestId)
                .push()
                .getKey();

        ChatMessage m = new ChatMessage();
        m.senderId = myUid;
        m.imageUrl = localPath;
        m.time = System.currentTimeMillis();
        m.seen = false;

        FirebaseDatabase.getInstance()
                .getReference("request_chats")
                .child(requestId)
                .child(id)
                .setValue(m);

        // Clear preview
        selectedImageUri = null;
        imgPreview.setImageDrawable(null);
        imgPreview.setVisibility(ImageView.GONE);
    }


    void pickImage() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        startActivityForResult(i, 101);
    }

    @Override
    protected void onActivityResult(int r, int c, Intent d) {
        super.onActivityResult(r, c, d);

        if (r == 101 && c == RESULT_OK && d != null) {

            selectedImageUri = d.getData();

            imgPreview.setImageURI(selectedImageUri);
            imgPreview.setVisibility(ImageView.VISIBLE);
        }
    }


//    @Override
//    protected void onActivityResult(int r, int c, Intent d) {
//        super.onActivityResult(r, c, d);
//
//        if (r == 101 && c == RESULT_OK && d != null) {
//
//            Uri u = d.getData();
//
//            String id = FirebaseDatabase.getInstance()
//                    .getReference("request_chats")
//                    .child(requestId)
//                    .push()
//                    .getKey();
//
//            ChatMessage m = new ChatMessage();
//            m.senderId = myUid;
//            m.imageUrl = u.toString();
//            m.time = System.currentTimeMillis();
//            m.seen = false;
//
//            FirebaseDatabase.getInstance()
//                    .getReference("request_chats")
//                    .child(requestId)
//                    .child(id)
//                    .setValue(m);
//        }
//    }
}
