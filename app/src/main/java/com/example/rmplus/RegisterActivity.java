package com.example.rmplus;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.ImageView;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;

public class RegisterActivity extends AppCompatActivity {

    EditText name, mobile, email, password;
    Button registerBtn;
    ImageView eyeBtn;
    boolean visible = false;


    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        name = findViewById(R.id.name);
        mobile = findViewById(R.id.mobile);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        registerBtn = findViewById(R.id.registerBtn);
        eyeBtn = findViewById(R.id.eyeBtn);

        auth = FirebaseAuth.getInstance();
        eyeBtn.setOnClickListener(v -> {

            if (visible) {
                password.setTransformationMethod(
                        PasswordTransformationMethod.getInstance());
                visible = false;
            } else {
                password.setTransformationMethod(
                        HideReturnsTransformationMethod.getInstance());
                visible = true;
            }

        });


        registerBtn.setOnClickListener(v -> {

            String nm = name.getText().toString().trim();
            String mb = mobile.getText().toString().trim();
            String em = email.getText().toString().trim();
            String ps = password.getText().toString().trim();

            if (!nm.matches("[a-zA-Z ]+")) {
                name.setError("Only letters allowed");
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(em).matches()) {
                email.setError("Invalid Email");
                return;
            }

            if (!mb.matches("^[6-9][0-9]{9}$")) {
                mobile.setError("Invalid Mobile Number");
                return;
            }

            if (!ps.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).{6,}$")) {
                password.setError("Weak Password");
                return;
            }

            auth.createUserWithEmailAndPassword(em, ps)
                    .addOnSuccessListener(result -> {

                        String uid = auth.getUid();

                        DatabaseReference userRef =
                                FirebaseDatabase.getInstance()
                                        .getReference("users")
                                        .child(uid);

//                        userRef.child("name").setValue(nm);
//                        userRef.child("mobile").setValue(mb);
//                        userRef.child("email").setValue(em);
//                        userRef.child("verified").setValue(false);
//
//                        auth.getCurrentUser().sendEmailVerification();
//                        startActivity(new Intent(
//                                RegisterActivity.this,
//                                VerifyEmailActivity.class));
//
//                        finish();

                        userRef.child("name").setValue(nm);
                        userRef.child("mobile").setValue(mb);
                        userRef.child("email").setValue(em);
                        userRef.child("verified").setValue(true);

                        startActivity(new Intent(
                                RegisterActivity.this,
                                LoginActivity.class));

                        finish();

                    })
                    .addOnFailureListener(e -> {

                        Toast.makeText(RegisterActivity.this,
                                e.getMessage(),
                                Toast.LENGTH_LONG).show();

                    });
        });
    }
}
