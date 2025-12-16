package com.pancreatitis.modules.questionnairemanager;

import com.pancreatitis.models.CharacterizationAnketPatient;
import com.pancreatitis.models.Patient;
import com.pancreatitis.models.Questionnaire;
import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.safety.SafetyModule;

import java.util.List;

public class QuestionnaireManagerModule {
    private static DatabaseModule databaseModule;
    private static SafetyModule safetyModule;
    private static CloudStorageModule cloudStorageModule;
    private static QuestionnaireManagerModule instance;
    private QuestionnaireManagerModule(){
        cloudStorageModule = CloudStorageModule.getInstance();
        safetyModule = SafetyModule.getInstance();
        databaseModule = DatabaseModule.getInstance();
    }
    public static QuestionnaireManagerModule getInstance(){
        if (instance == null) {
            instance = new QuestionnaireManagerModule();
        }
        return instance;
    }
    public String saveQuestionnaire(Questionnaire questionnaire, Patient patient, List<CharacterizationAnketPatient> characterizationAnketPatientList){
        try {
            databaseModule.beginTransaction();

            if (questionnaire.getIdPatient() == -1){
                long idPatient = databaseModule.insertPatient(patient);
                questionnaire.setIdPatient(idPatient);
            }

            long idQuestionnaire;
            if (questionnaire.getId() == -1){
                idQuestionnaire = databaseModule.insertQuestionnaire(questionnaire);
                characterizationAnketPatientList.stream().forEach(сharacterizationAnketPatient -> сharacterizationAnketPatient.setIdAnket(idQuestionnaire));
                databaseModule.insertAllCharacterizationAnketPatient(characterizationAnketPatientList);
            } else {
                databaseModule.updateQuestionnaire(questionnaire);
                idQuestionnaire = questionnaire.getId();
                characterizationAnketPatientList.stream().forEach(сharacterizationAnketPatient -> сharacterizationAnketPatient.setIdAnket(idQuestionnaire));
                databaseModule.updateAllCharacterizationAnketPatient(characterizationAnketPatientList);
            }
            databaseModule.setTransactionSuccessful();

            return "Succesfull";
        } catch (Exception e){
            return e.toString();
        } finally {
            databaseModule.endTransaction();
        }
    }
    public void submitQuestionnaire(Questionnaire questionnaire) {

    }
    public void encryptQuestionnaire(Questionnaire questionnaire) {

    }
    public void decryptQuestionnaire(Questionnaire questionnaire) {

    }
}
