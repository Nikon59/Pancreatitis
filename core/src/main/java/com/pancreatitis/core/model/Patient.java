package com.pancreatitis.core.model;

import java.io.Serializable;
import java.util.Objects;

public class Patient implements Serializable {

    private long id;
    private String uuid;
    private String fio;
    private String lastModified;

    public Patient() {
        this.id = -1;
    }

    public Patient(long id, String uuid, String fio, String lastModified) {
        this.id = id;
        this.uuid = uuid;
        this.fio = fio;
        this.lastModified = lastModified;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getFio() {
        return fio;
    }

    public void setFio(String fio) {
        this.fio = fio;
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
        Patient patient = (Patient) o;
        return id == patient.id &&
                Objects.equals(uuid, patient.uuid) &&
                Objects.equals(fio, patient.fio);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, uuid, fio);
    }

    @Override
    public String toString() {
        return "Patient{" +
                "id=" + id +
                ", uuid='" + uuid + '\'' +
                ", fio='" + fio + '\'' +
                ", lastModified='" + lastModified + '\'' +
                '}';
    }
}
