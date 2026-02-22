package com.example.rmplus.models;

public class CommonRequest {

    public String requestId;
    public String uid;
    public String title;
    public String status;
    public long time;

    // Type identifier
    public String requestType;   // "contact" OR "advertisement"

    public CommonRequest() {}
}
