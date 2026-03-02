package com.example.rmplus;

public class AdvertisementItem {

    public String id;
    public String imagePath;
    public String link;
    public long expiryDate;
    public String requestedBy;
    public long requestedAt;
    public String uid; // 🔥 Added to notify on expiry

    public AdvertisementItem() {}

    // Constructor for HomeActivity (3 args)
    public AdvertisementItem(String id, String imagePath, String link) {
        this.id = id;
        this.imagePath = imagePath;
        this.link = link;
        this.expiryDate = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000);
    }

    // Constructor for Admin Uploads (5 args)
    public AdvertisementItem(String imagePath, String link, long expiryDate, String requestedBy, long requestedAt) {
        this.imagePath = imagePath;
        this.link = link;
        this.expiryDate = expiryDate;
        this.requestedBy = requestedBy;
        this.requestedAt = requestedAt;
    }

    // Constructor for Full Data (6 args)
    public AdvertisementItem(String id, String imagePath, String link, long expiryDate, String requestedBy, long requestedAt) {
        this.id = id;
        this.imagePath = imagePath;
        this.link = link;
        this.expiryDate = expiryDate;
        this.requestedBy = requestedBy;
        this.requestedAt = requestedAt;
    }
}
