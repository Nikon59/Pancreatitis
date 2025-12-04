package com.pancreatitis.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

public class Doctor {
    private long id = -1;
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
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getFio() { return fio; }
    public void setFio(String fio) { this.fio = fio; }

    public Boolean getStatus() { return status; }
    public void setStatus(Boolean status) { this.status = status; }

    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public static Doctor fromJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Doctor.class);
    }
}
