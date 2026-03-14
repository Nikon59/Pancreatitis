package com.pancreatitis.modules.questionnairemanager;

import android.util.Pair;
import android.widget.Toast;

import com.pancreatitis.models.CharacterizationAnketPatient;
import com.pancreatitis.models.Characteristic;
import com.pancreatitis.models.Patient;
import com.pancreatitis.models.Questionnaire;
import com.pancreatitis.models.QuestionnaireDTO;
import com.pancreatitis.models.Update;
import com.pancreatitis.models.User;
import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.safety.SafetyModule;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

public class QuestionnaireManagerModule {
    private static DatabaseModule databaseModule;
    private static SafetyModule safetyModule;
    private static CloudStorageModule cloudStorageModule;
    private static QuestionnaireManagerModule instance;
    private Questionnaire questionnaire;
    private Patient patient;
    private List<CharacterizationAnketPatient> characterizationAnketPatients;
    private QuestionnaireManagerModule(){
        cloudStorageModule = CloudStorageModule.getInstance();
        safetyModule = SafetyModule.getInstance();
        databaseModule = DatabaseModule.getInstance();

        patient = new Patient();
        questionnaire = new Questionnaire();
        characterizationAnketPatients = new ArrayList<>();
    }
    public static QuestionnaireManagerModule getInstance(){
        if (instance == null) {
            instance = new QuestionnaireManagerModule();
        }
        return instance;
    }

    public List<CharacterizationAnketPatient> getCharacterizationAnketPatients() {
        return characterizationAnketPatients;
    }

    public void setCharacterizationAnketPatients(List<CharacterizationAnketPatient> characterizationAnketPatients) {
        this.characterizationAnketPatients = characterizationAnketPatients;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Questionnaire getQuestionnaire() {
        return questionnaire;
    }

    public void setQuestionnaire(Questionnaire questionnaire) {
        this.questionnaire = questionnaire;
    }

    public String saveQuestionnaire(){
        try {
            databaseModule.beginTransaction();

            int idPatient = questionnaire.getIdPatient();
            if (idPatient == -1){
                idPatient = databaseModule.insertPatient(patient);
                questionnaire.setIdPatient(idPatient);
                patient = databaseModule.getPatientById(idPatient);
            }

            int idQuestionnaire = questionnaire.getId();
            if (idQuestionnaire == -1){
                idQuestionnaire = databaseModule.insertQuestionnaire(questionnaire);
                questionnaire = databaseModule.getQuestionnaireById(idQuestionnaire);
                if (!characterizationAnketPatients.isEmpty()) {
                    for(CharacterizationAnketPatient ch: characterizationAnketPatients){
                        ch.setIdAnket(idQuestionnaire);
                    }
                    databaseModule.insertAllCharacterizationAnketPatient(characterizationAnketPatients);
                }
            } else {
                databaseModule.updateQuestionnaire(questionnaire);
            }

            databaseModule.setTransactionSuccessful();

            return "Успешно";
        } catch (Exception e){
            return e.getMessage();
        } finally {
            databaseModule.endTransaction();
        }
    }
    public String deleteQuestionnaire(){
        try {
            databaseModule.beginTransaction();

            int idQuestionnaire = questionnaire.getId();
            if (idQuestionnaire != -1) {
                int d1 = databaseModule.deleteQuestionnaire(idQuestionnaire);
                int d2 = databaseModule.deleteCharacterizationAnketPatients(idQuestionnaire);
            }

            databaseModule.setTransactionSuccessful();
            return "Успешно";
        } catch (Exception e) {
            return e.getMessage();
        } finally {
            databaseModule.endTransaction();
        }
    }

    public String submitQuestionnaire(List<Questionnaire> questionnaires, List<Patient> patients) {
        try {
            User user = User.getInstance();
            List<Boolean> uploadResults = new ArrayList<>();

            // Проверяем, что списки имеют одинаковый размер
            if (questionnaires.size() != patients.size()) {
                return "Ошибка: количество опросников и пациентов не совпадает";
            }

            // Создаем и загружаем отдельный Update для каждой пары
            for (int i = 0; i < patients.size(); i++) {
                Patient patient = patients.get(i);
                Questionnaire questionnaire = questionnaires.get(i);

                // Шифруем данные пациента
                encryptPatient(patient);

                // Создаем DTO для опросника
                QuestionnaireDTO questionnaireDTO = buildQuestionnaireDTO(questionnaire);

                // Создаем Update объект
                Update update = new Update();
                update.setPatient(patient);
                update.setQuestionnaireDTO(questionnaireDTO);

                // Загружаем отдельный файл для этой пары
                boolean result = cloudStorageModule.uploadQuestionnaire(update, user.getLogin());
                uploadResults.add(result);
            }

            // Проверяем результаты всех загрузок
            boolean allSuccessful = uploadResults.stream().allMatch(result -> result);
            return allSuccessful ? "Успешно" : "Частичная ошибка при загрузке";

        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private void encryptPatient(Patient patient) throws Exception {
        SecretKey key = User.getInstance().getKey();
        String encyprtFio = safetyModule.encryptString(patient.getFio(), key);
        patient.setFio(encyprtFio);
    }

    private QuestionnaireDTO buildQuestionnaireDTO(Questionnaire questionnaire){
        QuestionnaireDTO questionnaireDTO = new QuestionnaireDTO(questionnaire);
        List<CharacterizationAnketPatient> characterizationAnketPatients = databaseModule.getCharacterizationsForAnket(questionnaireDTO.getId());
        questionnaireDTO.setCharacteristicValues(characterizationAnketPatients);
        return questionnaireDTO;
    }
}
