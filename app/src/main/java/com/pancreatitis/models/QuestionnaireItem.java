package com.pancreatitis.models;

public class QuestionnaireItem {
    private int id;
    private int patientId;
    private String patientName;
    private String diagnosis;

    public QuestionnaireItem(int id, int patientId, String patientName, String diagnosis) {
        this.id = id;
        this.patientId = patientId;
        this.patientName = patientName;
        this.diagnosis = diagnosis;
    }

    // Геттеры и сеттеры
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
}
