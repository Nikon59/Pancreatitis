package com.pancreatitis.models;

import java.time.LocalDateTime;

public class CharacterizationAnketPatient {
    private Integer idCharacteristic;
    private Integer idAnket;
    private Integer value;
    private LocalDateTime lastModified;

    // Конструкторы
    public CharacterizationAnketPatient() {}

    public CharacterizationAnketPatient(Integer idCharacteristic, Integer idAnket,
                                        Integer value, LocalDateTime lastModified) {
        this.idCharacteristic = idCharacteristic;
        this.idAnket = idAnket;
        this.value = value;
        this.lastModified = lastModified;
    }

    // Геттеры и сеттеры
    public Integer getIdCharacteristic() { return idCharacteristic; }
    public void setIdCharacteristic(Integer idCharacteristic) { this.idCharacteristic = idCharacteristic; }

    public Integer getIdAnket() { return idAnket; }
    public void setIdAnket(Integer idAnket) { this.idAnket = idAnket; }

    public Integer getValue() { return value; }
    public void setValue(Integer value) { this.value = value; }

    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
}