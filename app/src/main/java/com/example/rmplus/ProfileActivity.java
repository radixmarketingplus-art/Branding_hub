package com.example.rmplus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.imageview.ShapeableImageView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import androidx.core.content.FileProvider;
import java.io.File;

import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    TextView nameTxt,emailTxt,mobileTxt;
    LinearLayout editBtn,subscriptionBtn,languageBtn,
            settingsBtn,rateBtn,supportBtn,shareBtn,
            logoutBtn,deleteBtn;

    Switch darkSwitch;
    ImageView profileImage;

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
        rateBtn=findViewById(R.id.rateBtn);
        supportBtn=findViewById(R.id.supportBtn);
        shareBtn=findViewById(R.id.shareBtn);
        logoutBtn=findViewById(R.id.logoutBtn);
        deleteBtn=findViewById(R.id.deleteBtn);
        darkSwitch=findViewById(R.id.darkSwitch);
        profileImage=findViewById(R.id.profileImage);

//        auth=FirebaseAuth.getInstance();
//        userRef=FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(auth.getUid());

        auth = FirebaseAuth.getInstance();

// 🔐 CHECK USER LOGIN STATE
        if (auth.getCurrentUser() == null) {

            Toast.makeText(this,
                    R.string.msg_login_again,
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



        shareBtn.setOnClickListener(v->shareApp());
 
         rateBtn.setOnClickListener(v -> {
             try {
                 startActivity(new Intent(Intent.ACTION_VIEW,
                         Uri.parse("market://details?id=" + getPackageName())));
             } catch (Exception e) {
                 startActivity(new Intent(Intent.ACTION_VIEW,
                         Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
             }
         });
 
         supportBtn.setOnClickListener(v -> showContactSupportDialog());

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

                        String profileUrl = s.child("profileImage").getValue(String.class);
                        if (profileUrl != null && !profileUrl.isEmpty()) {
                            Glide.with(ProfileActivity.this)
                                    .load(profileUrl)
                                    .placeholder(R.drawable.ic_profile)
                                    .error(R.drawable.ic_profile)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .into(profileImage);
                        } else {
                            profileImage.setImageResource(R.drawable.ic_profile);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError e) { }
                });
    }

    void showLanguageDialog(){
        String[] lang = {getString(R.string.lang_english), getString(R.string.lang_hindi)};

        new AlertDialog.Builder(this)
                .setTitle(R.string.select_language)
                .setItems(lang,(d,i)->{
                    if(i==0) setLocale("en");
                    else setLocale("hi");
                }).show();
    }

    void setLocale(String code){
        LocaleListCompat appLocales = LocaleListCompat.forLanguageTags(code);
        AppCompatDelegate.setApplicationLocales(appLocales);
    }

    void shareApp(){
        String shareMessage = getString(R.string.share_msg_body, getString(R.string.app_name), getPackageName());
        Intent i=new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, shareMessage);
        startActivity(Intent.createChooser(i, getString(R.string.title_share_using)));
    }

    void deleteAccount(){
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_delete_account)
                .setMessage(R.string.msg_delete_confirm)
                .setPositiveButton(R.string.yes,(d,w)->{
                    userRef.removeValue();
//                    auth.getCurrentUser().delete();
                    if (auth.getCurrentUser() != null) {
                        auth.getCurrentUser().delete();
                    }
                    startActivity(new Intent(this,
                            RegisterActivity.class));
                    finish();
                })
                .setNegativeButton(R.string.no,null)
                .show();
    }
    void showContactSupportDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_contact_support, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnCallSupport).setOnClickListener(v -> {
            dialog.dismiss();
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:+917089927270"));
            startActivity(callIntent);
        });

        dialogView.findViewById(R.id.btnEmailSupport).setOnClickListener(v -> {
            dialog.dismiss();
            Intent email = new Intent(Intent.ACTION_SENDTO);
            email.setData(Uri.parse("mailto:prikhush332@gmail.com"));
            email.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.support_email_subject));
            startActivity(email);
        });

        dialog.show();
    }
}
