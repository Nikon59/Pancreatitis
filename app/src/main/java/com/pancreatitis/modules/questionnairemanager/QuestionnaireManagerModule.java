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
    private int idQuestionnaire = -1;
    private List<CharacterizationAnketPatient> characterizationAnketPatients = new ArrayList<>();
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

    public List<CharacterizationAnketPatient> getCharacterizationAnketPatients() {
        return characterizationAnketPatients;
    }

    public void setCharacterizationAnketPatients(List<CharacterizationAnketPatient> characterizationAnketPatients) {
        this.characterizationAnketPatients = characterizationAnketPatients;
    }

    public int getIdQuestionnaire() {
        return idQuestionnaire;
    }

    public void setIdQuestionnaire(int idQuestionnaire) {
        this.idQuestionnaire = idQuestionnaire;
    }

    public String saveQuestionnaire(Questionnaire questionnaire, Patient patient){
        try {
            databaseModule.beginTransaction();

            int idPatient = questionnaire.getIdPatient();
            if (idPatient == -1){
                idPatient = databaseModule.insertPatient(patient);
                questionnaire.setIdPatient(idPatient);
                patient.setId(idPatient);
            }

            int idQuestionnaire = questionnaire.getId();
            if (idQuestionnaire == -1){
                idQuestionnaire = databaseModule.insertQuestionnaire(questionnaire);
                questionnaire.setId(idQuestionnaire);
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
            return e.toString();
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
        List<Pair<Characteristic, CharacterizationAnketPatient>> charasteristicDTOS = buildCharacterusticDTO(questionnaire.getId());
        questionnaireDTO.setCharacteristicValues(charasteristicDTOS);
        return questionnaireDTO;
    }

    private List<Pair<Characteristic, CharacterizationAnketPatient>> buildCharacterusticDTO(long idQuestionnaire){
        List<Pair<Characteristic, CharacterizationAnketPatient>> charasteristicDTOS = new ArrayList<>();
        List<Characteristic> charasteristics = databaseModule.getAllCharacteristics();

        List<CharacterizationAnketPatient> characterizationAnketPatients = databaseModule.getCharacterizationsForAnket(idQuestionnaire);
        for(CharacterizationAnketPatient characterizationAnketPatient: characterizationAnketPatients){
            Characteristic characteristic = charasteristics.get(characterizationAnketPatient.getIdCharacteristic() - 1);
            charasteristicDTOS.add(new Pair<>(characteristic, characterizationAnketPatient));
        }
        return charasteristicDTOS;
    }
}
