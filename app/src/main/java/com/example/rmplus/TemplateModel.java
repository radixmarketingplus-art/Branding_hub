package com.example.rmplus;

import java.io.Serializable;

public class TemplateModel implements Serializable {
    public String id;
    public String url;
    public String category;
    public String date; // Optional: used for festival filtering
    public String type; // IMAGE or VIDEO

    public TemplateModel() {}

    public TemplateModel(String id, String url, String category) {
        this.id = id;
        this.url = url;
        this.category = category;
    }

    public TemplateModel(String id, String url, String category, String date) {
        this.id = id;
        this.url = url;
        this.category = category;
        this.date = date;
    }

    public TemplateModel(String id, String url, String category, String date, String type) {
        this.id = id;
        this.url = url;
        this.category = category;
        this.date = date;
        this.type = type;
    }
}
