package com.example.rmplus;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    EditText email;
    Button sendBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        email=findViewById(R.id.email);
        sendBtn=findViewById(R.id.sendBtn);

        sendBtn.setOnClickListener(v -> {

            String em=email.getText().toString();

            // ✅ NEW: Check empty field FIRST
            if(em.isEmpty()){
                email.setError(getString(R.string.hint_enter_email));
                return;
            }

            // Existing validation

            if(!Patterns.EMAIL_ADDRESS.matcher(em).matches()){
                email.setError(getString(R.string.err_invalid_email));
                return;
            }

            FirebaseAuth.getInstance()
                    .sendPasswordResetEmail(em)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this,
                                    R.string.msg_reset_link_sent,
                                    Toast.LENGTH_LONG).show()
                    )
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    R.string.msg_email_not_registered,
                                    Toast.LENGTH_LONG).show()
                    );
        });
    }
}
