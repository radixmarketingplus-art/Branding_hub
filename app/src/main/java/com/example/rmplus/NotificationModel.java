package com.example.rmplus;

public class NotificationModel {

    public String notificationId;
    public String title;
    public String message;
    public long time;
    public boolean read;
    public String action;
    public String extraData;
    public long expiryDate;

    public NotificationModel(){}

    public NotificationModel(String id, String t, String m, long time, boolean r, String action, String extraData, long expiryDate){
        this.notificationId = id;
        title=t;
        message=m;
        this.time=time;
        this.read=r;
        this.action=action;
        this.extraData = extraData;
        this.expiryDate = expiryDate;
    }
}
