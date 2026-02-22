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
    String savedImagePath = "";

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
        startActivityForResult(i,101);
    }

    // -----------------------
    @Override
    protected void onActivityResult(int r,int c,Intent d){
        super.onActivityResult(r,c,d);

        if(r==101 && c==RESULT_OK && d!=null){
            selectedUri = d.getData();

            imgPreview.setImageURI(selectedUri);
            imgPreview.setVisibility(ImageView.VISIBLE);

            savedImagePath = copyImageToLocal(selectedUri);
        }
    }

    // -----------------------
    String copyImageToLocal(Uri uri){

        try{
            InputStream in = getContentResolver()
                    .openInputStream(uri);

            File folder =
                    new File(getFilesDir(),"request_images");

            if(!folder.exists())
                folder.mkdir();

            File file =
                    new File(folder,
                            System.currentTimeMillis()+".jpg");

            OutputStream out =
                    new FileOutputStream(file);

            byte[] buf = new byte[1024];
            int len;

            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }

            in.close();
            out.close();

            return file.getAbsolutePath();

        }catch(Exception e){
            e.printStackTrace();
            return "";
        }
    }

    // -----------------------
    void saveRequest(){

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
                        r.attachmentUrl = savedImagePath;

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
