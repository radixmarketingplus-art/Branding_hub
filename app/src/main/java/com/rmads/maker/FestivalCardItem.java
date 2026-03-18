package com.rmads.maker;

public class FestivalCardItem {

    public String imagePath;
    public String date;       // dd-MM-yyyy  e.g. "17-03-2026"
    public String festivalName; // e.g. "Holi", "Eid", "Ram Navami"
    public long expiryDate;
    public String templateId; // Firebase key

    public FestivalCardItem() {} // required for Firebase deserialization

    public FestivalCardItem(String imagePath, String date, String festivalName, long expiryDate) {
        this.imagePath = imagePath;
        this.date = date;
        this.festivalName = festivalName;
        this.expiryDate = expiryDate;
    }

    // Legacy constructor (backward compat)
    public FestivalCardItem(String imagePath, String date) {
        this.imagePath = imagePath;
        this.date = date;
        this.festivalName = "";
        this.expiryDate = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000);
    }

    // Legacy constructor (backward compat)
    public FestivalCardItem(String imagePath, String date, long expiryDate) {
        this.imagePath = imagePath;
        this.date = date;
        this.festivalName = "";
        this.expiryDate = expiryDate;
    }
}
