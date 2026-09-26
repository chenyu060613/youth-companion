package com.youthcompanion.model;

public class InterestItem {

    private String id;

    private String title;

    private String simple;

    private String complex;


    public InterestItem() {
    }


    public InterestItem(
            String id,
            String title,
            String simple,
            String complex
    ) {

        this.id = id;
        this.title = title;
        this.simple = simple;
        this.complex = complex;
    }


    public String getId() {
        return id;
    }


    public void setId(String id) {
        this.id = id;
    }


    public String getTitle() {
        return title;
    }


    public void setTitle(String title) {
        this.title = title;
    }


    public String getSimple() {
        return simple;
    }


    public void setSimple(String simple) {
        this.simple = simple;
    }


    public String getComplex() {
        return complex;
    }


    public void setComplex(String complex) {
        this.complex = complex;
    }
}