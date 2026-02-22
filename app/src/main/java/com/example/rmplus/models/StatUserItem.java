package com.example.rmplus.models;

public class StatUserItem {

    // Firebase UID
    public String uid;

    // User details (from /users/{uid})
    public String name;
    public String email;

    // Time (using lastLogin or action time if added later)
    public long time;

    // 🔹 Required empty constructor for Firebase
    public StatUserItem() {
    }

    // 🔹 Optional constructor (useful if needed later)
    public StatUserItem(String uid, String name, String email, long time) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.time = time;
    }
}
