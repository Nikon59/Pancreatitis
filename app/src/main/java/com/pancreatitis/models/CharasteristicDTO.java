// CharasteristicDTO.java
package com.pancreatitis.models;

import com.pancreatitis.models.Characteristic;

public class CharasteristicDTO extends Characteristic {
    private float value;

    public CharasteristicDTO() {
        super();
    }

    public CharasteristicDTO(Characteristic characteristic, float value) {
        this.setId(characteristic.getId());
        this.setName(characteristic.getName());
        this.setOpis(characteristic.getOpis());
        this.setType(characteristic.getType());
        this.setHints(characteristic.getHints());
        this.setWeight(characteristic.getWeight());
        this.setMinValue(characteristic.getMinValue());
        this.setMaxValue(characteristic.getMaxValue());
        this.setLastModified(characteristic.getLastModified());
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