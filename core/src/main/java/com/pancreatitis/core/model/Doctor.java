package com.pancreatitis.core.model;

import java.io.Serializable;
import java.util.Objects;

public class Doctor implements Serializable {

    private long id;
    private String login;
    private String fio;
    private String phone;
    private String email;
    private boolean status;
    private String createdAt;

    public Doctor() {
        this.id = -1;
    }

    public Doctor(long id, String login, String fio, String phone, String email, boolean status, String createdAt) {
        this.id = id;
        this.login = login;
        this.fio = fio;
        this.phone = phone;
        this.email = email;
        this.status = status;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getFio() {
        return fio;
    }

    public void setFio(String fio) {
        this.fio = fio;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Doctor doctor = (Doctor) o;
        return id == doctor.id &&
                status == doctor.status &&
                Objects.equals(login, doctor.login) &&
                Objects.equals(fio, doctor.fio);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, login, fio, status);
    }

    @Override
    public String toString() {
        return "Doctor{" +
                "id=" + id +
                ", login='" + login + '\'' +
                ", fio='" + fio + '\'' +
                ", status=" + status +
                '}';
    }
}
