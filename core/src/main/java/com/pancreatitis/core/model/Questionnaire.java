package com.pancreatitis.core.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Questionnaire implements Serializable {

    private long id;
    private String uuid;
    private long idPatient;
    private long idDoctor;
    private long idExpert;
    private String diagnosis;
    private String dateOfCompletion;
    private String admittedFrom;
    private String lastModified;
    private SyncStatus syncStatus;
    private List<QuestionnaireAnswer> answers;

    public Questionnaire() {
        this.id = -1;
        this.idPatient = -1;
        this.idDoctor = -1;
        this.idExpert = -1;
        this.syncStatus = SyncStatus.NEW;
        this.answers = new ArrayList<>();
    }

    public Questionnaire(long id, String uuid, long idPatient, long idDoctor, long idExpert,
                         String diagnosis, String dateOfCompletion, String admittedFrom,
                         String lastModified, SyncStatus syncStatus) {
        this.id = id;
        this.uuid = uuid;
        this.idPatient = idPatient;
        this.idDoctor = idDoctor;
        this.idExpert = idExpert;
        this.diagnosis = diagnosis;
        this.dateOfCompletion = dateOfCompletion;
        this.admittedFrom = admittedFrom;
        this.lastModified = lastModified;
        this.syncStatus = syncStatus != null ? syncStatus : SyncStatus.NEW;
        this.answers = new ArrayList<>();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getIdPatient() {
        return idPatient;
    }

    public void setIdPatient(long idPatient) {
        this.idPatient = idPatient;
    }

    public long getIdDoctor() {
        return idDoctor;
    }

    public void setIdDoctor(long idDoctor) {
        this.idDoctor = idDoctor;
    }

    public long getIdExpert() {
        return idExpert;
    }

    public void setIdExpert(long idExpert) {
        this.idExpert = idExpert;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getDateOfCompletion() {
        return dateOfCompletion;
    }

    public void setDateOfCompletion(String dateOfCompletion) {
        this.dateOfCompletion = dateOfCompletion;
    }

    public String getAdmittedFrom() {
        return admittedFrom;
    }

    public void setAdmittedFrom(String admittedFrom) {
        this.admittedFrom = admittedFrom;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

    public List<QuestionnaireAnswer> getAnswers() {
        return Collections.unmodifiableList(answers);
    }

    public void setAnswers(List<QuestionnaireAnswer> answers) {
        this.answers = (answers != null) ? new ArrayList<>(answers) : new ArrayList<>();
    }

    public void addAnswer(QuestionnaireAnswer answer) {
        if (answer != null) {
            this.answers.add(answer);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Questionnaire that = (Questionnaire) o;
        return id == that.id &&
                idPatient == that.idPatient &&
                idDoctor == that.idDoctor &&
                Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, uuid, idPatient, idDoctor);
    }

    @Override
    public String toString() {
        return "Questionnaire{" +
                "id=" + id +
                ", uuid='" + uuid + '\'' +
                ", idPatient=" + idPatient +
                ", diagnosis='" + diagnosis + '\'' +
                ", syncStatus=" + syncStatus +
                '}';
    }
}
