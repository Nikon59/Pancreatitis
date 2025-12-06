package com.pancreatitis.models;

public class QuestionnaireItem {
    private long idQuestionnaire;
    private int idPatient;
    private String fioPatient;
    private String diagnosis;

    public QuestionnaireItem() {}
    public QuestionnaireItem(long idQuestionnaire, int idPatient, String fioPatient, String diagnosis) {
        this.idQuestionnaire = idQuestionnaire;
        this.idPatient = idPatient;
        this.fioPatient = fioPatient;
        this.diagnosis = diagnosis;
    }

    public long getIdQuestionnaire() {
        return idQuestionnaire;
    }

    public void setIdQuestionnaire(long idQuestionnaire) {
        this.idQuestionnaire = idQuestionnaire;
    }

    public int getIdPatient() {
        return idPatient;
    }

    public void setIdPatient(int idPatient) {
        this.idPatient = idPatient;
    }

    public String getFioPatient() {
        return fioPatient;
    }

    public void setFioPatient(String fioPatient) {
        this.fioPatient = fioPatient;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }
}
