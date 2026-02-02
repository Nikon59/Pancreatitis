package com.pancreatitis.models;

import android.annotation.SuppressLint;

import java.util.List;

public class CharacteristicItem {
    private int idCharacteristic;
    private String name;
    private String hint;

    public CharacteristicItem() {
    }

    public int getIdCharacteristic() {
        return idCharacteristic;
    }

    public void setIdCharacteristic(int idCharacteristic) {
        this.idCharacteristic = idCharacteristic;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }
}
