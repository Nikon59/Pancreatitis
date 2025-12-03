package com.pancreatitis.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

public class CharacterizationValue {
    private Integer id;
    private Integer idCharacteristic;
    private Integer idValue;
    private String value;
    private LocalDateTime lastModified;

    // Конструкторы
    public CharacterizationValue() {}
    public CharacterizationValue(Integer id, Integer idCharacteristic, Integer idValue,
                      String value, LocalDateTime lastModified) {
        this.id = id;
        this.idCharacteristic = idCharacteristic;
        this.idValue = idValue;
        this.value = value;
        this.lastModified = lastModified;
    }

    // Геттеры и сеттеры
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getIdCharacteristic() { return idCharacteristic; }
    public void setIdCharacteristic(Integer idCharacteristic) { this.idCharacteristic = idCharacteristic; }

    public Integer getIdValue() { return idValue; }
    public void setIdValue(Integer idValue) { this.idValue = idValue; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public static CharacterizationValue fromJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, CharacterizationValue.class);
    }
}