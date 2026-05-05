package com.rmads.maker.models;

public class ClientInfoRequest {
    public String requestId;
    public String uid;
    public String userName;
    public String name;
    public String contact;
    public String email;
    public String businessName;
    public String businessCategory;
    public String serviceType;
    public String description;
    public String remark;
    public String status; // pending, accepted, rejected
    public long time;

    public ClientInfoRequest() {
    }
}
