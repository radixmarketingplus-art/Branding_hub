package com.example.rmplus.models;

public class ChatMessage {

    public String senderId;   // IMPORTANT
    public String message;
    public String imageUrl;
    public long time;
    public boolean seen;

    public ChatMessage() { }  // Required for Firebase
}
