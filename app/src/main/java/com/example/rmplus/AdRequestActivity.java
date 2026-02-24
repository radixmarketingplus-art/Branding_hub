package com.example.rmplus;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.rmplus.models.AdvertisementRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.io.*;

public class AdRequestActivity extends AppCompatActivity {

    EditText etLink;
    ImageView imgTemplate, imgProof;
    Button btnUploadTemplate, btnUploadProof, btnSubmit;
    Uri templateUri, proofUri;
    String templateUrl = "", proofUrl = "";

    DatabaseReference usersRef, adRef;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_ad_request);

        etLink = findViewById(R.id.etLink);
        imgTemplate = findViewById(R.id.imgTemplate);
        imgProof = findViewById(R.id.imgProof);
        btnUploadTemplate = findViewById(R.id.btnUploadTemplate);
        btnUploadProof = findViewById(R.id.btnUploadProof);
        btnSubmit = findViewById(R.id.btnSubmitAd);

        String uid = FirebaseAuth.getInstance().getUid();

        usersRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid);

        adRef = FirebaseDatabase.getInstance()
                .getReference("advertisement_requests");

        btnUploadTemplate.setOnClickListener(v -> pickImage(1));
        btnUploadProof.setOnClickListener(v -> pickImage(2));
        btnSubmit.setOnClickListener(v -> saveAdRequest());
    }

    // ---------------- PICK IMAGE ----------------

    void pickImage(int type){
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        String[] mimeTypes = {"image/jpeg", "image/jpg", "image/png"};
        i.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(i, type);
    }

    @Override
    protected void onActivityResult(int r,int c,Intent d){
        super.onActivityResult(r,c,d);

        if(c==RESULT_OK && d!=null){

            Uri uri = d.getData();
            if (uri == null) return;

            // Validate format
            String mimeType = getContentResolver().getType(uri);
            if (mimeType == null || (!mimeType.equals("image/jpeg") && !mimeType.equals("image/jpg") && !mimeType.equals("image/png"))) {
                Toast.makeText(this, "Only JPG, JPEG, or PNG allowed", Toast.LENGTH_SHORT).show();
                return;
            }

            if(r==1){
                templateUri = uri;
                imgTemplate.setImageURI(uri);
                imgTemplate.setVisibility(ImageView.VISIBLE);

                uploadImageToServer(uri, url -> templateUrl = url);
            }

            if(r==2){
                proofUri = uri;
                imgProof.setImageURI(uri);
                imgProof.setVisibility(ImageView.VISIBLE);

                uploadImageToServer(uri, url -> proofUrl = url);
            }
        }
    }

    // ---------------- SAVE IMAGE LOCALLY ----------------
    interface UrlCallback{
        void onResult(String url);
    }
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
                e.printStackTrace();
            }
        }).start();
    }



    // ---------------- SAVE REQUEST ----------------

    void saveAdRequest(){

        if(etLink.getText().toString().isEmpty()){
            etLink.setError("Enter advertisement link");
            return;
        }

        usersRef.addListenerForSingleValueEvent(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot s) {

                        String name = s.child("name").getValue(String.class);
                        String email = s.child("email").getValue(String.class);
                        String mobile = s.child("mobile").getValue(String.class);

                        String id = adRef.push().getKey();

                        AdvertisementRequest r = new AdvertisementRequest();

                        r.requestId = id;
                        r.uid = FirebaseAuth.getInstance().getUid();
                        r.userName = name;
                        r.email = email;
                        r.mobile = mobile;

                        r.adLink = etLink.getText().toString();
                        r.templatePath = templateUrl;
                        r.proofPath = proofUrl;

                        r.status = "pending";
                        r.time = System.currentTimeMillis();

                        adRef.child(id).setValue(r)
                                .addOnSuccessListener(u->{

                                    Toast.makeText(
                                            AdRequestActivity.this,
                                            "Advertisement Request Sent",
                                            Toast.LENGTH_SHORT).show();

                                    finish();
                                });
                    }

                    @Override
                    public void onCancelled(DatabaseError error){}
                });
    }
}
