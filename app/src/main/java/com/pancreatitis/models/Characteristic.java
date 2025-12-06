package com.pancreatitis.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;

public class Characteristic {
    private int id = -1;
    private String name;
    private String opis;
    private String type;
    private String hints;
    private float weight;
    private int minValue;
    private int maxValue;
    private LocalDateTime lastModified;

    // Конструкторы
    public Characteristic() {}
    public Characteristic(int id, String name, String opis, String type,
                          String hints, float weight, int minValue,
                          int maxValue, LocalDateTime lastModified) {
        this.id = id;
        this.name = name;
        this.opis = opis;
        this.type = type;
        this.hints = hints;
        this.weight = weight;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.lastModified = lastModified;
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

    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public static Characteristic fromJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Characteristic.class);
    }
}