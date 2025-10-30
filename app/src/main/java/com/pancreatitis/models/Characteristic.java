package com.pancreatitis.models;

public class Characteristic {
    private String title;
    private String hints;
    private String type;
    private String value;
    private int inputType;
    private int min_value;
    private int max_value;
    private long id;

    public Characteristic(String title, String hints, String type, String value, int min_value, int max_value, int inputType, long id) {
        this.title = title;
        this.hints = hints;
        this.type = type;
        this.value = value;
        this.inputType = inputType;
        this.id = id;
        this.min_value = min_value;
        this.max_value = max_value;
    }

    // Геттеры и сеттеры

    public int getMin_value() {
        return min_value;
    }

    public void setMin_value(int min_value) {
        this.min_value = min_value;
    }

    public int getMax_value() {
        return max_value;
    }

    public void setMax_value(int max_value) {
        this.max_value = max_value;
    }

    public String getHints() {
        return hints;
    }

    public void setHints(String hints) {
        this.hints = hints;
    }

    public long getId() { return this.id; }
    public void setId(long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public int getInputType() { return inputType; }
    public void setInputType(int inputType) { this.inputType = inputType; }
}