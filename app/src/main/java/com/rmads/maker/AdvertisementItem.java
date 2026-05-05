package com.rmads.maker;

public class AdvertisementItem {

    public String id;
    public String imagePath;
    public String link;
    public long expiryDate;
    public String requestedBy;
    public long requestedAt;
    public String uid; // 🔥 Added to notify on expiry
    public String requestId; // 🔥 Added to link back to original request
    public String type; // "image" or "video"

    public AdvertisementItem() {}

    // Constructor for HomeActivity (3 args)
    public AdvertisementItem(String id, String imagePath, String link) {
        this.id = id;
        this.imagePath = imagePath;
        this.link = link;
        this.expiryDate = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000);
        this.type = (imagePath != null && (imagePath.toLowerCase().contains(".mp4") || imagePath.toLowerCase().contains(".mkv") || imagePath.toLowerCase().contains(".webm") || imagePath.toLowerCase().contains(".mov") || imagePath.toLowerCase().contains(".3gp"))) ? "video" : "image";
    }

    // Constructor for Admin Uploads (5 args)
    public AdvertisementItem(String imagePath, String link, long expiryDate, String requestedBy, long requestedAt) {
        this.imagePath = imagePath;
        this.link = link;
        this.expiryDate = expiryDate;
        this.requestedBy = requestedBy;
        this.requestedAt = requestedAt;
        this.type = (imagePath != null && (imagePath.toLowerCase().contains(".mp4") || imagePath.toLowerCase().contains(".mkv") || imagePath.toLowerCase().contains(".webm") || imagePath.toLowerCase().contains(".mov") || imagePath.toLowerCase().contains(".3gp"))) ? "video" : "image";
    }

    // Constructor for Full Data (6 args)
    public AdvertisementItem(String id, String imagePath, String link, long expiryDate, String requestedBy, long requestedAt) {
        this.id = id;
        this.imagePath = imagePath;
        this.link = link;
        this.expiryDate = expiryDate;
        this.requestedBy = requestedBy;
        this.requestedAt = requestedAt;
        this.type = (imagePath != null && (imagePath.toLowerCase().contains(".mp4") || imagePath.toLowerCase().contains(".mkv") || imagePath.toLowerCase().contains(".webm") || imagePath.toLowerCase().contains(".mov") || imagePath.toLowerCase().contains(".3gp"))) ? "video" : "image";
    }
}
