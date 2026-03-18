package com.rmads.maker.models;

public class SubscriptionPlan {
    public String id;
    public String duration; // e.g. "1 Month"
    public String amount;   // e.g. "199"
    public String discountPrice; // e.g. "99", default "0"
    public String scannerUrl;
    public String upiId; // New field for UPI ID
    public String specificDate; // Optional: format YYYY-MM-DD or similar
    public boolean isSpecificDay;

    public SubscriptionPlan() {
    }

    public SubscriptionPlan(String id, String duration, String amount, String discountPrice, String scannerUrl, String upiId) {
        this.id = id;
        this.duration = duration;
        this.amount = amount;
        this.discountPrice = discountPrice;
        this.scannerUrl = scannerUrl;
        this.upiId = upiId;
    }
}
