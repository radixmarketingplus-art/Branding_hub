package com.example.rmplus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    TextView nameTxt,emailTxt,mobileTxt;
    LinearLayout editBtn,subscriptionBtn,languageBtn,
            settingsBtn,appRefBtn,shareBtn,
            logoutBtn,deleteBtn;

    Switch darkSwitch;

    FirebaseAuth auth;
    DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        nameTxt=findViewById(R.id.nameTxt);
        emailTxt=findViewById(R.id.emailTxt);
        mobileTxt=findViewById(R.id.mobileTxt);

        editBtn=findViewById(R.id.editBtn);
        subscriptionBtn=findViewById(R.id.subscriptionBtn);
        languageBtn=findViewById(R.id.languageBtn);
        settingsBtn=findViewById(R.id.settingsBtn);
        appRefBtn=findViewById(R.id.appRefBtn);
        shareBtn=findViewById(R.id.shareBtn);
        logoutBtn=findViewById(R.id.logoutBtn);
        deleteBtn=findViewById(R.id.deleteBtn);
        darkSwitch=findViewById(R.id.darkSwitch);

//        auth=FirebaseAuth.getInstance();
//        userRef=FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(auth.getUid());

        auth = FirebaseAuth.getInstance();

// 🔐 CHECK USER LOGIN STATE
        if (auth.getCurrentUser() == null) {

            Toast.makeText(this,
                    "Please login again",
                    Toast.LENGTH_SHORT).show();

            startActivity(new Intent(this,
                    LoginActivity.class));

            finish();
            return;
        }

// ✅ SAFE UID
        String uid = auth.getCurrentUser().getUid();

        userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid);

        loadProfile();

        editBtn.setOnClickListener(v->
                startActivity(new Intent(this,
                        EditProfileActivity.class)));

        subscriptionBtn.setOnClickListener(v->
                startActivity(new Intent(this,
                        SubscriptionActivity.class)));

        settingsBtn.setOnClickListener(v->
                startActivity(new Intent(this,
                        SettingsActivity.class)));

        appRefBtn.setOnClickListener(v->
                startActivity(new Intent(this,
                        AppReferenceActivity.class)));

        shareBtn.setOnClickListener(v->shareApp());

//        logoutBtn.setOnClickListener(v->{
//            auth.signOut();
//            startActivity(new Intent(this,
//                    LoginActivity.class));
//            finish();
//        });

        logoutBtn.setOnClickListener(v -> {

            auth.signOut();

            SharedPreferences sp =
                    getSharedPreferences("APP_DATA", MODE_PRIVATE);

            sp.edit().clear().apply();

            startActivity(new Intent(this,
                    LoginActivity.class));

            finish();
        });

        deleteBtn.setOnClickListener(v->deleteAccount());

        languageBtn.setOnClickListener(v->showLanguageDialog());

        darkSwitch.setOnCheckedChangeListener(
                (b,isChecked)->{
                    if(isChecked)
                        AppCompatDelegate
                                .setDefaultNightMode(
                                        AppCompatDelegate
                                                .MODE_NIGHT_YES);
                    else
                        AppCompatDelegate
                                .setDefaultNightMode(
                                        AppCompatDelegate
                                                .MODE_NIGHT_NO);
                });
    }

//    void loadProfile(){
//        userRef.addListenerForSingleValueEvent(
//                new ValueEventListener() {
//                    @Override
//                    public void onDataChange(DataSnapshot s) {
//                        nameTxt.setText(
//                                s.child("name")
//                                        .getValue(String.class));
//                        emailTxt.setText(
//                                s.child("email")
//                                        .getValue(String.class));
//                        mobileTxt.setText(
//                                s.child("mobile")
//                                        .getValue(String.class));
//                    }
//                    @Override public void onCancelled(DatabaseError e){}
//                });
//    }
    void loadProfile(){

        if (userRef == null) return;

        userRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot s) {

                        nameTxt.setText(
                                s.child("name")
                                        .getValue(String.class));

                        emailTxt.setText(
                                s.child("email")
                                        .getValue(String.class));

                        mobileTxt.setText(
                                s.child("mobile")
                                        .getValue(String.class));
                    }

                    @Override
                    public void onCancelled(DatabaseError e) { }
                });
    }

    void showLanguageDialog(){
        String[] lang={"English","Hindi"};

        new AlertDialog.Builder(this)
                .setTitle("Select Language")
                .setItems(lang,(d,i)->{
                    if(i==0) setLocale("en");
                    else setLocale("hi");
                }).show();
    }

    void setLocale(String code){
        Locale locale=new Locale(code);
        Locale.setDefault(locale);
        Configuration config=new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(
                config,getResources().getDisplayMetrics());
        recreate();
    }

    void shareApp(){
        Intent i=new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT,
                "Download RM Plus App");
        startActivity(Intent.createChooser(i,
                "Share Using"));
    }

    void deleteAccount(){
        new AlertDialog.Builder(this)
                .setTitle("Delete Account?")
                .setMessage("This cannot be undone")
                .setPositiveButton("Yes",(d,w)->{
                    userRef.removeValue();
//                    auth.getCurrentUser().delete();
                    if (auth.getCurrentUser() != null) {
                        auth.getCurrentUser().delete();
                    }
                    startActivity(new Intent(this,
                            RegisterActivity.class));
                    finish();
                })
                .setNegativeButton("No",null)
                .show();
    }
}
