package com.example.rmplus;

public class FestivalCardItem {

    public String imagePath;
    public String date; // dd-MM-yyyy
    public long expiryDate;

    public FestivalCardItem(String imagePath, String date) {
        this.imagePath = imagePath;
        this.date = date;
        this.expiryDate = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000); // Default 7 days
    }

    public FestivalCardItem(String imagePath, String date, long expiryDate) {
        this.imagePath = imagePath;
        this.date = date;
        this.expiryDate = expiryDate;
    }
}
