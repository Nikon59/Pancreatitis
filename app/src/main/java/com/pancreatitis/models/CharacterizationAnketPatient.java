package com.pancreatitis.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

public class CharacterizationAnketPatient {
    private long idAnket = -1;
    private int value = -1;
    private LocalDateTime lastModified;
    private Characteristic characteristic;

    // Конструкторы
    public CharacterizationAnketPatient() {}

    public CharacterizationAnketPatient(long idAnket, int value, LocalDateTime lastModified) {
        this.idAnket = idAnket;
        this.value = value;
        this.lastModified = lastModified;
    }

    // Геттеры и сеттеры
    public long getIdAnket() { return idAnket; }
    public void setIdAnket(long idAnket) { this.idAnket = idAnket; }

    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }

    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }

    public Characteristic getCharacteristic() {
        return characteristic;
    }

    public void setCharacteristic(Characteristic characteristic) {
        this.characteristic = characteristic;
    }

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public static CharacterizationAnketPatient fromJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, CharacterizationAnketPatient.class);
    }
}