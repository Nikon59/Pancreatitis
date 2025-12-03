package com.pancreatitis.models;

import java.time.LocalDateTime;

public class Doctor {
    private Integer id;
    private String fio;
    private Boolean status; // Используем Boolean вместо byte[] для BLOB
    private LocalDateTime lastModified;

    // Конструкторы
    public Doctor() {}

    public Doctor(Integer id, String fio, Boolean status, LocalDateTime lastModified) {
        this.id = id;
        this.fio = fio;
        this.status = status;
        this.lastModified = lastModified;
    }

    // Геттеры и сеттеры
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getFio() { return fio; }
    public void setFio(String fio) { this.fio = fio; }

    public Boolean getStatus() { return status; }
    public void setStatus(Boolean status) { this.status = status; }

    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
}
