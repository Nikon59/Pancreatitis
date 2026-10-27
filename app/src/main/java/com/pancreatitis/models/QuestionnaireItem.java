package com.pancreatitis.models;

import java.util.HashMap;
import java.util.Map;

public class QuestionnaireItem {
    private int idQuestionnaire;
    private int idPatient;
    private String fioPatient;
    private String diagnosis = "-";
    private boolean selected = false;

    public QuestionnaireItem() {}

    private String codeToDiagnosis(String code){
        if (code == null || code.trim().isEmpty()) {
            return "-";
        }
        Map<String, String> diagnosisMap = new HashMap<>();
        diagnosisMap.put("0", "Отёчный панкреатит");
        diagnosisMap.put("1", "Панкрионекроз");
        diagnosisMap.put("5", "Панкреонекроз среднетяжелое течение");
        diagnosisMap.put("6", "Панкреонекроз тяжелое течение");
        if (diagnosisMap.containsKey(code)) {
            return diagnosisMap.get(code);
        }
        return code;
    }

    public boolean isSelected(){
        return selected;
    }

    public void setSelected(boolean selected){
        this.selected = selected;
    }

    public int getIdQuestionnaire() {
        return idQuestionnaire;
    }

    public void setIdQuestionnaire(int idQuestionnaire) {
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
        return codeToDiagnosis(diagnosis);
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }
}
