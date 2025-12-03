package com.pancreatitis.models;

import java.time.LocalDateTime;

public class Patient {
    private long id;
    private String fio;
    private LocalDateTime lastModified;

    // Конструкторы
    public Patient() {}

    public Patient(Integer id, String fio, LocalDateTime lastModified) {
        this.id = id;
        this.fio = fio;
        this.lastModified = lastModified;
    }

    // Геттеры и сеттеры
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getFio() { return fio; }
    public void setFio(String fio) { this.fio = fio; }

    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
}