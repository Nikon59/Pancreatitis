package com.pancreatitis.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

public class CharacterizationValue {
    private long id = -1;
    private int idCharacteristic = -1;
    private int idValue = -1;
    private String value;
    private LocalDateTime lastModified;

    // Конструкторы
    public CharacterizationValue() {}
    public CharacterizationValue(long id, int idCharacteristic, int idValue,
                      String value, LocalDateTime lastModified) {
        this.id = id;
        this.idCharacteristic = idCharacteristic;
        this.idValue = idValue;
        this.value = value;
        this.lastModified = lastModified;
    }

    // Геттеры и сеттеры
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getIdCharacteristic() { return idCharacteristic; }
    public void setIdCharacteristic(int idCharacteristic) { this.idCharacteristic = idCharacteristic; }

    public int getIdValue() { return idValue; }
    public void setIdValue(int idValue) { this.idValue = idValue; }

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