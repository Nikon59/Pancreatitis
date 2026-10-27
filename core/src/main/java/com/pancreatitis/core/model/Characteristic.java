package com.pancreatitis.core.model;

import java.io.Serializable;
import java.util.Objects;

public class Characteristic implements Serializable {

    private int id;
    private String name;
    private String opis;
    private int idType;
    private String hints;
    private float weight;
    private float minValue;
    private float maxValue;
    private String lastModified;

    public Characteristic() {
        this.id = -1;
    }

    public Characteristic(int id, String name, String opis, int idType, String hints,
                          float weight, float minValue, float maxValue, String lastModified) {
        this.id = id;
        this.name = name;
        this.opis = opis;
        this.idType = idType;
        this.hints = hints;
        this.weight = weight;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.lastModified = lastModified;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOpis() {
        return opis;
    }

    public void setOpis(String opis) {
        this.opis = opis;
    }

    public int getIdType() {
        return idType;
    }

    public void setIdType(int idType) {
        this.idType = idType;
    }

    public String getHints() {
        return hints;
    }

    public void setHints(String hints) {
        this.hints = hints;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public float getMinValue() {
        return minValue;
    }

    public void setMinValue(float minValue) {
        this.minValue = minValue;
    }

    public float getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(float maxValue) {
        this.maxValue = maxValue;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Characteristic that = (Characteristic) o;
        return id == that.id && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "Characteristic{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", opis='" + opis + '\'' +
                ", idType=" + idType +
                '}';
    }
}
