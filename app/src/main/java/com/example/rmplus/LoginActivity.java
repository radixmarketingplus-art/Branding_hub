package com.example.rmplus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.ImageView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ServerValue;


public class LoginActivity extends AppCompatActivity {

    EditText email,password;
    Button loginBtn;
    TextView forgotBtn,registerBtn;
    ImageView eyeBtn;
    boolean visible=false;
    FirebaseAuth auth;

    // ✅ Added for Email / Phone mode
    Button emailModeBtn, phoneModeBtn;
    boolean loginWithPhone = false;
    boolean isLoggingIn = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        email=findViewById(R.id.email);
        password=findViewById(R.id.password);
        loginBtn=findViewById(R.id.loginBtn);
        forgotBtn=findViewById(R.id.forgotPassword);
        registerBtn=findViewById(R.id.registerText);
        eyeBtn = findViewById(R.id.eyeBtn);

        // ✅ Initialize mode buttons
        emailModeBtn = findViewById(R.id.emailModeBtn);
        phoneModeBtn = findViewById(R.id.phoneModeBtn);

        auth=FirebaseAuth.getInstance();
        SharedPreferences sp =
                getSharedPreferences("APP_DATA", MODE_PRIVATE);

        boolean loggedIn =
                sp.getBoolean("isLoggedIn", false);

        if (loggedIn && auth.getCurrentUser() != null) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        // ✅ Mode switching
        emailModeBtn.setOnClickListener(v -> {
            loginWithPhone = false;
            email.setHint("Enter Email");
        });

        phoneModeBtn.setOnClickListener(v -> {
            loginWithPhone = true;
            email.setHint("Enter Phone Number");
        });


        eyeBtn.setOnClickListener(v -> {

            if(visible){
                password.setTransformationMethod(
                        PasswordTransformationMethod.getInstance());
                visible=false;
            }else{
                password.setTransformationMethod(
                        HideReturnsTransformationMethod.getInstance());
                visible=true;
            }

        });


        loginBtn.setOnClickListener(v -> {

            if (isLoggingIn) return;   // ❌ prevent multiple clicks

            setLoading(true);  // 🔒 disable button

            String input = email.getText().toString().trim();
            String ps = password.getText().toString().trim();

            // ❗ EMPTY INPUT CHECK
            if (input.isEmpty() || ps.isEmpty()) {
                Toast.makeText(this,
                        "Enter email/phone and password",
                        Toast.LENGTH_SHORT).show();
                setLoading(false);
                return;
            }
            email.clearFocus();
            password.clearFocus();

            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            getSystemService(INPUT_METHOD_SERVICE);

            if (imm != null) {
                imm.hideSoftInputFromWindow(
                        getCurrentFocus() != null
                                ? getCurrentFocus().getWindowToken()
                                : email.getWindowToken(),
                        0
                );
            }

            if (!isInternetAvailable()) {
                Toast.makeText(this,
                        "No internet connection",
                        Toast.LENGTH_SHORT).show();
                setLoading(false);
                return;
            }


            // ❌ OLD EMAIL-ONLY VALIDATION (commented)
            /*
            if(!Patterns.EMAIL_ADDRESS.matcher(em).matches()){
                email.setError("Invalid Email");
                return;
            }
            */

            // ===================================================
            // ✅ NEW LOGIN LOGIC — EMAIL OR PHONE
            // ===================================================

            if (loginWithPhone) {

                // 📱 PHONE LOGIN
                if (!input.matches("^[6-9][0-9]{9}$")) {
                    email.setError("Invalid Phone Number");
                    setLoading(false);
                    return;
                }

                DatabaseReference ref =
                        FirebaseDatabase.getInstance()
                                .getReference("users");

                ref.orderByChild("mobile")
                        .equalTo(input)
                        .addListenerForSingleValueEvent(
                                new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot snapshot) {

                                        if (!snapshot.exists()) {
                                            Toast.makeText(LoginActivity.this,
                                                    "Phone not registered",
                                                    Toast.LENGTH_SHORT).show();

                                            setLoading(false);
                                            return;
                                        }

//                                        for (DataSnapshot ds : snapshot.getChildren()) {
//
//                                            String userEmail =
//                                                    ds.child("email")
//                                                            .getValue(String.class);
//
//                                            loginWithEmail(userEmail, ps);
//                                        }

                                        for (DataSnapshot ds : snapshot.getChildren()) {

                                            String userEmail =
                                                    ds.child("email")
                                                            .getValue(String.class);

                                            if(userEmail != null && !userEmail.isEmpty()) {
                                                loginWithEmail(userEmail, ps);
                                                return; // ✅ stop after first match
                                            }
                                        }

                                        Toast.makeText(LoginActivity.this,
                                                "Email not found for this phone",
                                                Toast.LENGTH_SHORT).show();

                                        setLoading(false);

                                    }

                                    @Override
                                    public void onCancelled(DatabaseError error) {

                                        setLoading(false);
                                    }
                                });

            } else {

                // 📧 EMAIL LOGIN
                if (!Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
                    email.setError("Invalid Email");
                    setLoading(false);
                    return;
                }

                loginWithEmail(input, ps);
            }

        });

        forgotBtn.setOnClickListener(v -> {
            setLoading(false);
            clearInputs();
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });

        registerBtn.setOnClickListener(v -> {
            setLoading(false);
            clearInputs();
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }


    // ===================================================
    // ✅ HELPER METHOD — SIGN IN WITH EMAIL
    // ===================================================

    private void loginWithEmail(String em, String ps) {

        auth.signInWithEmailAndPassword(em,ps)
                .addOnSuccessListener(result -> {

                    // ❌ OLD EMAIL VERIFICATION CHECK (commented)
                    /*
                    if(auth.getCurrentUser().isEmailVerified()){
                    */

                    // ===================================================
                    // ✅ DIRECT LOGIN WITHOUT VERIFICATION
                    // ===================================================

//                    String uid = auth.getUid();
                    if (auth.getCurrentUser() == null) {
                        setLoading(false);
                        Toast.makeText(this,
                                "Authentication error",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String uid = auth.getCurrentUser().getUid();
                    String userEmail = auth.getCurrentUser().getEmail();

                    DatabaseReference userRef =
                            FirebaseDatabase.getInstance()
                                    .getReference("users")
                                    .child(uid);

                    userRef.addListenerForSingleValueEvent(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {

                                    if(!snapshot.exists()){
                                        userRef.child("email").setValue(userEmail);
                                        userRef.child("loginCount").setValue(1);
                                    }else{

                                        Long count = snapshot.child("loginCount")
                                                .getValue(Long.class);

                                        if(count == null){
                                            count = 0L;
                                        }

                                        userRef.child("loginCount")
                                                .setValue(count + 1);
                                    }

                                    userRef.child("hasLoggedIn")
                                            .setValue(true);

                                    userRef.child("lastLogin")
                                            .setValue(ServerValue.TIMESTAMP);

                                    userRef.child("role")
                                            .addListenerForSingleValueEvent(
                                                    new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(DataSnapshot snapshot) {

                                                            String role = snapshot.getValue(String.class);

                                                            if(role == null){
                                                                role = "user";
                                                                userRef.child("role").setValue("user");
                                                            }

//                                                            Toast.makeText(LoginActivity.this,
//                                                                    "Login Success",
//                                                                    Toast.LENGTH_SHORT).show();

//                                                            Intent i = new Intent(
//                                                                    LoginActivity.this,
//                                                                    HomeActivity.class);
//
//                                                            i.putExtra("role", role);
//                                                            startActivity(i);
//                                                            finish();

                                                            // ===== SAVE ROLE PERMANENTLY =====
                                                            SharedPreferences sp =
                                                                    getSharedPreferences("APP_DATA", MODE_PRIVATE);

                                                            sp.edit()
                                                                    .putString("role", role)
                                                                    .apply();

// ===== OPTIONAL: SAVE LOGIN STATUS =====
                                                            sp.edit()
                                                                    .putBoolean("isLoggedIn", true)
                                                                    .apply();

                                                            Toast.makeText(LoginActivity.this,
                                                                    "Login Success",
                                                                    Toast.LENGTH_SHORT).show();

// ===== OPEN HOME =====
                                                            startActivity(new Intent(
                                                                    LoginActivity.this,
                                                                    HomeActivity.class));

                                                            finish();
                                                        }

                                                        @Override
                                                        public void onCancelled(DatabaseError error) {

                                                            Toast.makeText(LoginActivity.this,
                                                                    "Network error",
                                                                    Toast.LENGTH_SHORT).show();

                                                            setLoading(false);
                                                        }
                                                    });
                                }

                                @Override
                                public void onCancelled(DatabaseError error) {

                                    setLoading(false);
                                }
                            });

                    // ❌ OLD ELSE BLOCK FOR VERIFICATION (commented)
                    /*
                    }
                    else{
                        Toast.makeText(this,
                                "Verify Email First",
                                Toast.LENGTH_LONG).show();
                    }
                    */

                }).addOnFailureListener(e -> {

                    Toast.makeText(this,
                            "Wrong Credentials",
                            Toast.LENGTH_SHORT).show();

                    setLoading(false);
                });
    }

    void clearInputs() {

        email.setText("");
        password.setText("");

        email.clearFocus();
        password.clearFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();

        setLoading(false);
        clearInputs();
    }

    void setLoading(boolean loading) {

        isLoggingIn = loading;

        // 🔘 Button
        loginBtn.setClickable(!loading);

        // ✏️ Inputs
        email.setEnabled(!loading);
        password.setEnabled(!loading);

        // 🔄 Mode buttons
        emailModeBtn.setEnabled(!loading);
        phoneModeBtn.setEnabled(!loading);

        // 👁 Password toggle
        eyeBtn.setEnabled(!loading);

        // 🔗 Links
        forgotBtn.setEnabled(!loading);
        registerBtn.setEnabled(!loading);

        // Optional text change
        loginBtn.setText(loading ? "Please wait..." : "Login");
    }

    @Override
    public void onBackPressed() {
        if (isLoggingIn) return;   // block back during login
        super.onBackPressed();
    }

    boolean isInternetAvailable() {
        android.net.ConnectivityManager cm =
                (android.net.ConnectivityManager)
                        getSystemService(CONNECTIVITY_SERVICE);

        android.net.NetworkInfo net =
                cm != null ? cm.getActiveNetworkInfo() : null;

        return net != null && net.isConnected();
    }
}
