// CharasteristicDTO.java
package com.pancreatitis.models;

import com.pancreatitis.models.Characteristic;

public class CharasteristicDTO extends Characteristic {
    private float value;

    public CharasteristicDTO() {
        super();
    }

    public CharasteristicDTO(Characteristic characteristic) {
        super(characteristic);
    }
    public CharasteristicDTO(Characteristic characteristic, float value) {
        super(characteristic);
        this.value = value;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public int getIntValue() {
        return Math.round(value);
    }
}