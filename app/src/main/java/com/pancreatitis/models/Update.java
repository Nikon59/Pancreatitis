package com.pancreatitis.models;

import java.util.List;

public class Update {
    private List<Patient> patientList;
    private List<QuestionnaireDTO> questionnaireDTOS;

    public List<Patient> getPatientList() {
        return patientList;
    }

    public void setPatientList(List<Patient> patientList) {
        this.patientList = patientList;
    }

    public List<QuestionnaireDTO> getQuestionnaireDTOS() {
        return questionnaireDTOS;
    }

    public void setQuestionnaireDTOS(List<QuestionnaireDTO> questionnaireDTOS) {
        this.questionnaireDTOS = questionnaireDTOS;
    }
}
