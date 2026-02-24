package com.example.rmplus;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.rmplus.models.CustomerRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.io.*;

public class CreateRequestActivity extends AppCompatActivity {

    Spinner spType;
    EditText etTitle, etDesc;
    Button btnAttach, btnSubmit;
    ImageView imgPreview;

    Uri selectedUri;
    String attachmentUrl = "";

    DatabaseReference usersRef, requestRef;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_create_request);

        spType = findViewById(R.id.spType);
        etTitle = findViewById(R.id.etTitle);
        etDesc = findViewById(R.id.etDesc);
        btnAttach = findViewById(R.id.btnAttach);
        btnSubmit = findViewById(R.id.btnSubmit);
        imgPreview = findViewById(R.id.imgPreview);

        String[] arr = {"Issue","Order","Custom Template","Other"};
        spType.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                arr));

        String uid = FirebaseAuth.getInstance().getUid();

        // Preview Image
        imgPreview.setOnClickListener(v -> {

            if (selectedUri != null) {
                Intent i = new Intent(
                        CreateRequestActivity.this,
                        ImagePreviewActivity.class
                );
                i.putExtra("img", selectedUri.toString());
                startActivity(i);
            }

        });

        usersRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid);

        requestRef = FirebaseDatabase.getInstance()
                .getReference("customer_requests");

        btnAttach.setOnClickListener(v -> pickImage());
        btnSubmit.setOnClickListener(v -> saveRequest());
    }

    // -----------------------
    void pickImage(){
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        String[] mimeTypes = {"image/jpeg", "image/jpg", "image/png"};
        i.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(i,101);
    }

    // -----------------------
    @Override
    protected void onActivityResult(int r,int c,Intent d){
        super.onActivityResult(r,c,d);

        if(r==101 && c==RESULT_OK && d!=null){
            selectedUri = d.getData();
            if (selectedUri == null) return;

            // Validate format
            String mimeType = getContentResolver().getType(selectedUri);
            if (mimeType == null || (!mimeType.equals("image/jpeg") && !mimeType.equals("image/jpg") && !mimeType.equals("image/png"))) {
                Toast.makeText(this, "Only JPG, JPEG, or PNG allowed", Toast.LENGTH_SHORT).show();
                return;
            }

            imgPreview.setImageURI(selectedUri);
            imgPreview.setVisibility(ImageView.VISIBLE);

            uploadImageToServer(selectedUri, url -> attachmentUrl = url);
        }
    }

    // -----------------------

    private void uploadImageToServer(Uri uri, UrlCallback cb){

        new Thread(() -> {
            try {

                String boundary = "----RMPLUS" + System.currentTimeMillis();

                java.net.URL url =
                        new java.net.URL("http://187.77.184.84/upload.php");

                java.net.HttpURLConnection conn =
                        (java.net.HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                conn.setRequestProperty(
                        "Content-Type",
                        "multipart/form-data; boundary=" + boundary
                );

                java.io.DataOutputStream out =
                        new java.io.DataOutputStream(conn.getOutputStream());

                out.writeBytes("--" + boundary + "\r\n");
                out.writeBytes(
                        "Content-Disposition: form-data; name=\"file\"; filename=\"img.jpg\"\r\n"
                );
                out.writeBytes("Content-Type: image/jpeg\r\n\r\n");

                InputStream input =
                        getContentResolver().openInputStream(uri);

                byte[] buffer = new byte[4096];
                int len;

                while ((len = input.read(buffer)) != -1)
                    out.write(buffer, 0, len);

                input.close();

                out.writeBytes("\r\n--" + boundary + "--\r\n");
                out.flush();
                out.close();

                java.io.BufferedReader reader =
                        new java.io.BufferedReader(
                                new java.io.InputStreamReader(conn.getInputStream())
                        );

                StringBuilder res = new StringBuilder();
                String line;

                while((line=reader.readLine())!=null)
                    res.append(line);

                reader.close();

                org.json.JSONObject json =
                        new org.json.JSONObject(res.toString());

                cb.onResult(json.getString("url"));

            } catch (Exception e){
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Upload failed. Try again.",
                                Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
    interface UrlCallback{
        void onResult(String url);
    }
    // -----------------------
    void saveRequest(){

        if (selectedUri != null && attachmentUrl.isEmpty()) {
            Toast.makeText(this,
                    "Image uploading... Please wait",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if(etTitle.getText().toString().isEmpty()){
            etTitle.setError("Enter title");
            return;
        }

        if(etDesc.getText().toString().isEmpty()){
            etDesc.setError("Enter description");
            return;
        }

        usersRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot s) {

                        String name =
                                s.child("name").getValue(String.class);
                        String email =
                                s.child("email").getValue(String.class);
                        String mobile =
                                s.child("mobile").getValue(String.class);

                        String id = requestRef.push().getKey();

                        CustomerRequest r = new CustomerRequest();

                        r.requestId = id;
                        r.uid = FirebaseAuth.getInstance().getUid();
                        r.userName = name;
                        r.email = email;
                        r.mobile = mobile;
                        r.type = spType.getSelectedItem().toString();
                        r.title = etTitle.getText().toString();
                        r.description = etDesc.getText().toString();
                        r.status = "pending";
                        r.time = System.currentTimeMillis();
                        r.attachmentUrl = attachmentUrl;

                        requestRef.child(id)
                                .setValue(r)
                                .addOnSuccessListener(u->{

                                    NotificationHelper.send(
                                            CreateRequestActivity.this,
                                            "ADMIN",
                                            "New Request",
                                            r.title);

                                    Toast.makeText(
                                            CreateRequestActivity.this,
                                            "Request Sent",
                                            Toast.LENGTH_SHORT).show();

                                    finish();
                                });
                    }

                    @Override
                    public void onCancelled(DatabaseError error){}
                });
    }
}
