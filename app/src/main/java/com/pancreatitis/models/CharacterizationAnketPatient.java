package com.pancreatitis.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

public class CharacterizationAnketPatient {
    private long idAnket = -1;
    private int idCharacteristic = -1;
    private int value = 0;
    private LocalDateTime lastModified;

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

    public int getIdCharacteristic() {
        return idCharacteristic;
    }

    public void setIdCharacteristic(int idCharacteristic) {
        this.idCharacteristic = idCharacteristic;
    }

    @Override
    public String toString() {
        return "{" +
                "idAnket=" + idAnket +
                ", idCharacteristic=" + idCharacteristic +
                ", value=" + value +
                ", lastModified=" + lastModified +
                '}';
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