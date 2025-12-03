package com.pancreatitis.models;

import java.time.LocalDateTime;
import java.util.*;

public class Questionnaire {
    private long id;
    private Patient patient;
    private Doctor doctor;
    private Integer idExpert;
    private String diagnosis;
    private LocalDateTime dateOfCompletion;
    private String admittedFrom;
    private LocalDateTime lastModified;
    List<CharacterizationAnketPatient> characterizationAnketPatients;

    // Конструкторы
    public Questionnaire() {}
    public Questionnaire(Integer id, Patient patient, Doctor doctor, Integer idExpert,
                 String diagnosis, LocalDateTime dateOfCompletion, String admittedFrom,
                 LocalDateTime lastModified) {
        this.id = id;
        this.patient = patient;
        this.doctor = doctor;
        this.idExpert = idExpert;
        this.diagnosis = diagnosis;
        this.dateOfCompletion = dateOfCompletion;
        this.admittedFrom = admittedFrom;
        this.lastModified = lastModified;
    }

    private String codeToDiagnosis(String code){
        Map<String, String> diagnosisMap = new HashMap<>();
        diagnosisMap.put("1", "Отечный панкреатит");
        diagnosisMap.put("5", "Панкреонекроз среднетяжелое течение");
        diagnosisMap.put("6", "Панкреонекроз тяжелое течение");
        return diagnosisMap.getOrDefault(code, "-");
    }

    // Геттеры и сеттеры
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public void setIdCharacterizationAnketPatient(){
        for (CharacterizationAnketPatient cap : characterizationAnketPatients) {
            cap.setIdAnket(id);
        }
    }
    public Patient getPatient() {
        return patient;
    }
    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Doctor getDoctor() {
        return doctor;
    }
    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public Integer getIdExpert() { return idExpert; }
    public void setIdExpert(Integer idExpert) { this.idExpert = idExpert; }

    public String getDiagnosis() { return codeToDiagnosis(diagnosis); }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public LocalDateTime getDateOfCompletion() { return dateOfCompletion; }
    public void setDateOfCompletion(LocalDateTime dateOfCompletion) { this.dateOfCompletion = dateOfCompletion; }

    public String getAdmittedFrom() { return admittedFrom; }
    public void setAdmittedFrom(String admittedFrom) { this.admittedFrom = admittedFrom; }

    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }

    public List<CharacterizationAnketPatient> getCharacterizationAnketPatients() {
        return characterizationAnketPatients;
    }

    public void setCharacterizationAnketPatients(List<CharacterizationAnketPatient> characterizationAnketPatients) {
        this.characterizationAnketPatients = characterizationAnketPatients;
    }
}