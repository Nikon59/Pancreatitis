package com.pancreatitis.models;

public class QuestionnaireItem {
    private int id;
    private String patientName;
    private String diagnosis;

    public QuestionnaireItem() {}

    public QuestionnaireItem(int id, String patientName, String diagnosis) {
        this.id = id;
        this.patientName = patientName;
        this.diagnosis = diagnosis;
    }

    // Геттеры и сеттеры
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
}
