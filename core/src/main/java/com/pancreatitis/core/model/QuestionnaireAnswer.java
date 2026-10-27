package com.pancreatitis.core.model;

import java.io.Serializable;
import java.util.Objects;

public class QuestionnaireAnswer implements Serializable {

    private long id;
    private int idCharacteristic;
    private long idAnket;
    private Integer idValue;
    private Float value;
    private String createdAt;
    private String lastModified;

    public QuestionnaireAnswer() {
        this.id = -1;
    }

    public QuestionnaireAnswer(long id, int idCharacteristic, long idAnket,
                               Integer idValue, Float value, String createdAt, String lastModified) {
        this.id = id;
        this.idCharacteristic = idCharacteristic;
        this.idAnket = idAnket;
        this.idValue = idValue;
        this.value = value;
        this.createdAt = createdAt;
        this.lastModified = lastModified;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getIdCharacteristic() {
        return idCharacteristic;
    }

    public void setIdCharacteristic(int idCharacteristic) {
        this.idCharacteristic = idCharacteristic;
    }

    public long getIdAnket() {
        return idAnket;
    }

    public void setIdAnket(long idAnket) {
        this.idAnket = idAnket;
    }

    public Integer getIdValue() {
        return idValue;
    }

    public void setIdValue(Integer idValue) {
        this.idValue = idValue;
    }

    public Float getValue() {
        return value;
    }

    public void setValue(Float value) {
        this.value = value;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
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
        QuestionnaireAnswer that = (QuestionnaireAnswer) o;
        return id == that.id &&
                idCharacteristic == that.idCharacteristic &&
                idAnket == that.idAnket;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, idCharacteristic, idAnket);
    }

    @Override
    public String toString() {
        return "QuestionnaireAnswer{" +
                "id=" + id +
                ", idCharacteristic=" + idCharacteristic +
                ", idAnket=" + idAnket +
                ", idValue=" + idValue +
                ", value=" + value +
                '}';
    }
}
