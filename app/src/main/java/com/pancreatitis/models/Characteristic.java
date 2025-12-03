package com.pancreatitis.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;

public class Characteristic {
    private Integer id;
    private String name;
    private String opis;
    private String type;
    private String hints;
    private Integer weight;
    private Integer minValue;
    private Integer maxValue;
    private LocalDateTime lastModified;
    private List<CharacterizationValue> characterizationValues;

    // Конструкторы
    public Characteristic() {}
    public Characteristic(Integer id, String name, String opis, String type,
                          String hints, Integer weight, Integer minValue,
                          Integer maxValue, LocalDateTime lastModified) {
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
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOpis() { return opis; }
    public void setOpis(String opis) { this.opis = opis; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getHints() { return hints; }
    public void setHints(String hints) { this.hints = hints; }

    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }

    public Integer getMinValue() { return minValue; }
    public void setMinValue(Integer minValue) { this.minValue = minValue; }

    public Integer getMaxValue() { return maxValue; }
    public void setMaxValue(Integer maxValue) { this.maxValue = maxValue; }

    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }

    public List<CharacterizationValue> getCharacterizationValue() {
        return characterizationValues;
    }
    public void setCharacterizationValue(List<CharacterizationValue> characterizationValues) {
        this.characterizationValues = characterizationValues;
    }

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public static Characteristic fromJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Characteristic.class);
    }
}