package com.pancreatitis.models;

public class Characteristic {
    private String title;
    private String type;
    private String value;
    private int inputType;
    private long id;

    public Characteristic(String title, String type, String value, int inputType, long id) {
        this.title = title;
        this.type = type;
        this.value = value;
        this.inputType = inputType;
        this.id = id;
    }

    // Геттеры и сеттеры

    public long getId() { return this.id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public int getInputType() { return inputType; }
    public void setInputType(int inputType) { this.inputType = inputType; }
}