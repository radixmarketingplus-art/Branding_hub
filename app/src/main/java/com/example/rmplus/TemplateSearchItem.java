package com.example.rmplus;

public class TemplateSearchItem {

    public String path;
    public String title;
    public String category;
    public String keywords;   // tags

    public TemplateSearchItem(
            String path,
            String title,
            String category,
            String keywords
    ) {
        this.path = path;
        this.title = title;
        this.category = category;
        this.keywords = keywords;
    }
}
