package com.example.rmplus;

import android.os.Bundle;
import android.widget.*;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.example.rmplus.NotificationHelper;

public class EditProfileActivity extends AppCompatActivity {

    EditText name,designation,dob,email,mobile,city,state,pincode,landmark;
    RadioGroup genderGroup;
    RadioButton male,female,other;
    Button saveBtn;

    DatabaseReference userRef;

    String oldName,oldDesignation,oldDob,oldMobile,oldCity,
            oldState,oldPincode,oldLandmark,oldGender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        name = findViewById(R.id.name);
        designation = findViewById(R.id.designation);
        dob = findViewById(R.id.dob);
        email = findViewById(R.id.email);
        mobile = findViewById(R.id.mobile);
        city = findViewById(R.id.city);
        state = findViewById(R.id.state);
        pincode = findViewById(R.id.pincode);
        landmark = findViewById(R.id.landmark);

        genderGroup = findViewById(R.id.genderGroup);
        male = findViewById(R.id.male);
        female = findViewById(R.id.female);
        other = findViewById(R.id.other);

        saveBtn = findViewById(R.id.saveBtn);

        String uid = FirebaseAuth.getInstance().getUid();

        if(uid == null){
            Toast.makeText(this,"User not logged in",Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid);

        loadData();

        saveBtn.setOnClickListener(v -> confirmSave());
    }

    // -----------------------------

    void loadData(){

        userRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot s) {

                        name.setText(getValue(s,"name"));
                        designation.setText(getValue(s,"designation"));
                        dob.setText(getValue(s,"dob"));
                        email.setText(getValue(s,"email"));
                        mobile.setText(getValue(s,"mobile"));
                        city.setText(getValue(s,"city"));
                        state.setText(getValue(s,"state"));
                        pincode.setText(getValue(s,"pincode"));
                        landmark.setText(getValue(s,"landmark"));

                        oldName = getValue(s,"name");
                        oldDesignation = getValue(s,"designation");
                        oldDob = getValue(s,"dob");
                        oldMobile = getValue(s,"mobile");
                        oldCity = getValue(s,"city");
                        oldState = getValue(s,"state");
                        oldPincode = getValue(s,"pincode");
                        oldLandmark = getValue(s,"landmark");
                        oldGender = getValue(s,"gender");

                        if(oldGender.equals("Male")) male.setChecked(true);
                        else if(oldGender.equals("Female")) female.setChecked(true);
                        else other.setChecked(true);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) { }
                });
    }

    String getValue(DataSnapshot s,String key){
        if(s.child(key).exists())
            return s.child(key).getValue(String.class);
        return "";
    }

    // -----------------------------

    void confirmSave(){

        new AlertDialog.Builder(this)
                .setTitle("Save Changes?")
                .setMessage("Do you want to update profile?")
                .setPositiveButton("Yes",(d,w)-> saveProfile())
                .setNegativeButton("No",null)
                .show();
    }

    // -----------------------------

    void saveProfile(){

        String newName = name.getText().toString();
        String newDesignation = designation.getText().toString();
        String newDob = dob.getText().toString();
        String newMobile = mobile.getText().toString();
        String newCity = city.getText().toString();
        String newState = state.getText().toString();
        String newPincode = pincode.getText().toString();
        String newLandmark = landmark.getText().toString();

        String gender="Other";
        if(male.isChecked()) gender="Male";
        if(female.isChecked()) gender="Female";

        userRef.child("name").setValue(newName);
        userRef.child("designation").setValue(newDesignation);
        userRef.child("dob").setValue(newDob);
        userRef.child("mobile").setValue(newMobile);
        userRef.child("city").setValue(newCity);
        userRef.child("state").setValue(newState);
        userRef.child("pincode").setValue(newPincode);
        userRef.child("landmark").setValue(newLandmark);
        userRef.child("gender").setValue(gender);

        Toast.makeText(this,"Profile Updated",Toast.LENGTH_SHORT).show();
        NotificationHelper.send(
                EditProfileActivity.this,
                FirebaseAuth.getInstance().getUid(),
                "Profile Updated",
                "Your profile updated successfully");

        finish();
    }
}
