package com.example.rmplus;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.Date;
import com.example.rmplus.NotificationHelper;


public class ApproveRejectActivity extends AppCompatActivity {

    TextView nameTxt,emailTxt,mobileTxt,planTxt,dateTxt;
    ImageView proofImage;
    Button approveBtn,rejectBtn,downloadBtn;

    DatabaseReference requestRef,userRef;
    String uid;
    String proofPath="";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_approve_reject);

        nameTxt=findViewById(R.id.nameTxt);
        emailTxt=findViewById(R.id.emailTxt);
        mobileTxt=findViewById(R.id.mobileTxt);
        planTxt=findViewById(R.id.planTxt);
        dateTxt=findViewById(R.id.dateTxt);
        proofImage=findViewById(R.id.proofImage);
        approveBtn=findViewById(R.id.approveBtn);
        rejectBtn=findViewById(R.id.rejectBtn);
        downloadBtn=findViewById(R.id.downloadBtn);

        uid=getIntent().getStringExtra("uid");

        requestRef= FirebaseDatabase.getInstance()
                .getReference("subscription_requests")
                .child(uid);

        userRef= FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid);

        loadData();

        approveBtn.setOnClickListener(v->approve());
        rejectBtn.setOnClickListener(v->reject());
        downloadBtn.setOnClickListener(v->saveToGallery());
    }

    // ---------------------------

    void loadData(){

        requestRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    public void onDataChange(DataSnapshot s){

                        nameTxt.setText("Name : " +
                                s.child("name").getValue(String.class));

                        emailTxt.setText("Email : " +
                                s.child("email").getValue(String.class));

                        mobileTxt.setText("Mobile : " +
                                s.child("mobile").getValue(String.class));

                        planTxt.setText("Plan : " +
                                s.child("plan").getValue(String.class));

                        Long t=s.child("time")
                                .getValue(Long.class);

                        if(t!=null){
                            dateTxt.setText(
                                    new Date(t).toString());
                        }

                        proofImage.setOnClickListener(v -> {
                            Intent i = new Intent(
                                    ApproveRejectActivity.this,
                                    ImagePreviewActivity.class
                            );
                            i.putExtra("img", proofPath);
                            startActivity(i);
                        });


                        proofPath=s.child("proofPath")
                                .getValue(String.class);

                        if(proofPath!=null){
                            proofImage.setImageURI(
                                    Uri.fromFile(
                                            new File(proofPath)));
                        }
                    }
                    public void onCancelled(DatabaseError e){}
                });
    }

    // ---------------------------

    void approve(){

        requestRef.child("status")
                .setValue("approved");

        userRef.child("subscribed")
                .setValue(true);

        userRef.child("subscriptionStatus")
                .setValue("approved");

        requestRef.child("plan")
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            public void onDataChange(DataSnapshot s) {
                                userRef.child("plan")
                                        .setValue(
                                                s.getValue(String.class));
                            }
                            public void onCancelled(DatabaseError e){}
                        });

        Toast.makeText(this,
                "Approved",
                Toast.LENGTH_SHORT).show();

        NotificationHelper.send(
                ApproveRejectActivity.this,
                uid,
                "Subscription Approved",
                "Your subscription has been approved");

        finish();
    }

    // ---------------------------

    void reject(){

        requestRef.child("status")
                .setValue("rejected");

        userRef.child("subscribed")
                .setValue(false);

        userRef.child("subscriptionStatus")
                .setValue("rejected");

        Toast.makeText(this,
                "Rejected",
                Toast.LENGTH_SHORT).show();

        NotificationHelper.send(
                ApproveRejectActivity.this,
                uid,
                "Subscription Rejected",
                "Your subscription has been rejected");

        finish();
    }

    // ---------------------------

    void saveToGallery(){

        try{

            FileInputStream fis =
                    new FileInputStream(proofPath);

            Bitmap bitmap =
                    BitmapFactory.decodeStream(fis);

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME,
                    "proof_"+uid+".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE,
                    "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH,
                    "Pictures/RMPlus");

            Uri uri = getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values);

            OutputStream out =
                    getContentResolver().openOutputStream(uri);

            bitmap.compress(Bitmap.CompressFormat.JPEG,
                    100,out);

            out.close();

            Toast.makeText(this,
                    "Saved in Gallery",
                    Toast.LENGTH_LONG).show();

        }catch(Exception e){
            Toast.makeText(this,
                    "Save Failed",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
