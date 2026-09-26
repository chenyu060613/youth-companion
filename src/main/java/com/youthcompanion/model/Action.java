package com.youthcompanion.model;

public class Action {

    private String id;
    private int level;
    private String text;

    public Action() {
    }

    public Action(String id, int level, String text) {
        this.id = id;
        this.level = level;
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}