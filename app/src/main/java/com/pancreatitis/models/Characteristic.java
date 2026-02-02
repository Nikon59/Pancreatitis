package com.pancreatitis.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class Characteristic {
    private int id;
    private String name;
    private String opis;
    private String type;
    private String hints;
    private float weight;
    private int minValue;
    private int maxValue;
    private String lastModified;

    // Конструкторы
    public Characteristic() {}

    public Characteristic(Characteristic characteristic) {
        this.id = characteristic.getId();
        this.name = characteristic.getName();
        this.opis = characteristic.getOpis();
        this.type = characteristic.getType();
        this.hints = characteristic.getHints();
        this.weight = characteristic.getWeight();
        this.minValue = characteristic.getMinValue();
        this.maxValue = characteristic.getMaxValue();
        this.lastModified = characteristic.getLastModified();
    }

    // Геттеры и сеттеры
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOpis() { return opis; }
    public void setOpis(String opis) { this.opis = opis; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getHints() { return hints; }
    public void setHints(String hints) { this.hints = hints; }

    public float getWeight() { return weight; }
    public void setWeight(float weight) { this.weight = weight; }

    public int getMinValue() { return minValue; }
    public void setMinValue(int minValue) { this.minValue = minValue; }

    public int getMaxValue() { return maxValue; }
    public void setMaxValue(int maxValue) { this.maxValue = maxValue; }

    public String getLastModified() { return lastModified; }
    public void setLastModified(String lastModified) { this.lastModified = lastModified; }

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public static Characteristic fromJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Characteristic.class);
    }
}