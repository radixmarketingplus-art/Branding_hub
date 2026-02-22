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
    String templatePath = "", proofPath = "";

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
        startActivityForResult(i, type);
    }

    @Override
    protected void onActivityResult(int r,int c,Intent d){
        super.onActivityResult(r,c,d);

        if(c==RESULT_OK && d!=null){

            Uri uri = d.getData();

            if(r==1){
                templateUri = uri;
                imgTemplate.setImageURI(uri);
                imgTemplate.setVisibility(ImageView.VISIBLE);
                templatePath = copyImage(uri);
            }

            if(r==2){
                proofUri = uri;
                imgProof.setImageURI(uri);
                imgProof.setVisibility(ImageView.VISIBLE);
                proofPath = copyImage(uri);
            }
        }
    }

    // ---------------- SAVE IMAGE LOCALLY ----------------

    String copyImage(Uri uri){
        try{
            InputStream in = getContentResolver().openInputStream(uri);

            File folder = new File(getFilesDir(),"ad_images");
            if(!folder.exists()) folder.mkdir();

            File file = new File(folder,
                    System.currentTimeMillis()+".jpg");

            OutputStream out = new FileOutputStream(file);

            byte[] buf = new byte[1024];
            int len;

            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }

            in.close();
            out.close();

            return file.getAbsolutePath();

        }catch(Exception e){
            return "";
        }
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
                        r.templatePath = templatePath;
                        r.proofPath = proofPath;

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
