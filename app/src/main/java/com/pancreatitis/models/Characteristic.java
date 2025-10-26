package com.pancreatitis.models;

public class Characteristic {
    private String title;
    private String value;
    private int inputType;

    public Characteristic(String title, String value, int inputType) {
        this.title = title;
        this.value = value;
        this.inputType = inputType;
    }

    // Геттеры и сеттеры
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public int getInputType() { return inputType; }
    public void setInputType(int inputType) { this.inputType = inputType; }
}