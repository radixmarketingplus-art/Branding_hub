package com.example.rmplus;

public class UserItem {
    public String uid;
    public String name;
    public String email;
    public String mobile;
    public String profileImage;
    public String role;
    public boolean verified;

    public UserItem() {
    }

    public UserItem(String uid, String name, String email, String mobile, String profileImage) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.profileImage = profileImage;
    }
}
