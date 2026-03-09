package com.example.rmplus.models;

public class SubscriptionRequest {
    public String uid;
    public String name;
    public String plan;
    public String duration;
    public String amount;
    public String discountPrice;
    public String status;
    public String proofPath;
    public long time;

    public SubscriptionRequest() {
    }

    public SubscriptionRequest(String uid, String name, String plan, String status, String proofPath, long time, String duration, String amount, String discountPrice) {
        this.uid = uid;
        this.name = name;
        this.plan = plan;
        this.duration = duration;
        this.amount = amount;
        this.discountPrice = discountPrice;
        this.status = status;
        this.proofPath = proofPath;
        this.time = time;
    }
}
