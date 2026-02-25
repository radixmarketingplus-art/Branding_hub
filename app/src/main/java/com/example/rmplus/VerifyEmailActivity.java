package com.example.rmplus;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class VerifyEmailActivity extends AppCompatActivity {

    Button checkBtn,resendBtn;
    FirebaseAuth auth;

//    String name,mobile,email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);

        checkBtn=findViewById(R.id.checkBtn);
        resendBtn=findViewById(R.id.resendBtn);

        auth=FirebaseAuth.getInstance();

//        name=getIntent().getStringExtra("name");
//        mobile=getIntent().getStringExtra("mobile");
//        email=getIntent().getStringExtra("email");

        resendBtn.setOnClickListener(v ->
                auth.getCurrentUser().sendEmailVerification()
        );

        checkBtn.setOnClickListener(v -> {

            auth.getCurrentUser().reload();

            if(auth.getCurrentUser().isEmailVerified()){

                String uid=auth.getUid();

//                FirebaseDatabase.getInstance()
//                        .getReference("users")
//                        .child(uid)
//                        .child("name").setValue(name);
//
//                FirebaseDatabase.getInstance()
//                        .getReference("users")
//                        .child(uid)
//                        .child("mobile").setValue(mobile);
//
//                FirebaseDatabase.getInstance()
//                        .getReference("users")
//                        .child(uid)
//                        .child("email").setValue(email);

                FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(uid)
                        .child("verified").setValue(true);

                startActivity(new Intent(this,LoginActivity.class));
                finish();

            }else{
                Toast.makeText(this,
                        R.string.msg_email_not_verified,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
