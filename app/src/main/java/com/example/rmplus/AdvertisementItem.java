package com.example.rmplus;

public class AdvertisementItem {

    public String imagePath;
    public String link;
    public long expiryDate;
    public String requestedBy;
    public long requestedAt;

    public AdvertisementItem(String imagePath, String link) {
        this.imagePath = imagePath;
        this.link = link;
        this.expiryDate = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000); // Default 7 days
    }

    public AdvertisementItem(String imagePath, String link, long expiryDate, String requestedBy, long requestedAt) {
        this.imagePath = imagePath;
        this.link = link;
        this.expiryDate = expiryDate;
        this.requestedBy = requestedBy;
        this.requestedAt = requestedAt;
    }
}
