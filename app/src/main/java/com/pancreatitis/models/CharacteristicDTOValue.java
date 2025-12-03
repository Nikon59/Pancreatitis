package com.pancreatitis.models;

public class CharacteristicDTOValue {
    private String value;
    private Characteristic characteristic;
    public CharacteristicDTOValue(String value, Characteristic characteristic) {
        this.characteristic = characteristic;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Characteristic getCharacteristic() {
        return characteristic;
    }

    public void setCharacteristic(Characteristic characteristic) {
        this.characteristic = characteristic;
    }
}
