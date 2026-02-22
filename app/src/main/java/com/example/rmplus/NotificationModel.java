package com.example.rmplus;

public class NotificationModel {

    public String title;
    public String message;
    public long time;
    public boolean read;

    public NotificationModel(){}

    public NotificationModel(String t,String m,long time,boolean r){
        title=t;
        message=m;
        this.time=time;
        read=r;
    }
}
