package com.pancreatitis.modules.questionnairemanager;

import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pancreatitis.models.CharacterizationAnketPatient;
import com.pancreatitis.models.Characteristic;
import com.pancreatitis.models.Doctor;
import com.pancreatitis.models.Patient;
import com.pancreatitis.models.Questionnaire;
import com.pancreatitis.models.QuestionnaireDTO;
import com.pancreatitis.models.Update;
import com.pancreatitis.models.User;
import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.safety.SafetyModule;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.nio.charset.StandardCharsets;

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

    /**
     * Создаёт ZIP-архив (в памяти) с файлом questionnaires.json внутри.
     * @return байты ZIP-архива
     */
    private byte[] createZipArchive(String jsonContent) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("questionnaires.json");
            zos.putNextEntry(entry);
            zos.write(jsonContent.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    private String buildArchiveJson(List<QuestionnaireDTO> questionnaires) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(questionnaires);
    }

    public String uploadAllQuestionnairesAsArchive() {
        User user = User.getInstance();
        if (user == null || user.getLogin() == null || user.getLogin().isEmpty()) {
            return "Пользователь не авторизован";
        }

        Doctor doctor = databaseModule.getDoctorByLogin(user.getLogin());
        if (doctor == null) {
            return "Врач не найден в БД";
        }

        List<QuestionnaireDTO> questionnaires = databaseModule.getAllFullQuestionnairesForDoctor(doctor.getId());
        if (questionnaires.isEmpty()) {
            return "Нет анкет для выгрузки";
        }

        try {
            // 1. Преобразуем в JSON
            String jsonData = buildArchiveJson(questionnaires);

            // 2. Упаковываем в ZIP
            byte[] zipData = createZipArchive(jsonData);

            // 3. Шифруем ZIP-данные (общий ключ пользователя)
            SecretKey key = user.getKey();
            byte[] encryptedData = safetyModule.encryptData(zipData, key);

            // 4. Формируем имя файла с текущей датой
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"));
            String fileName = "archive_" + timestamp + ".zip.enc";

            // 5. Загружаем в облако (папка fetch/upload)
            boolean success = cloudStorageModule.uploadArchive(encryptedData, fileName);
            return success ? "Архив успешно отправлен" : "Ошибка при загрузке архива";

        } catch (Exception e) {
            Log.e("QuestionnaireManager", "Archive upload error", e);
            return "Ошибка: " + e.getMessage();
        }
    }

    private QuestionnaireDTO buildQuestionnaireDTO(Questionnaire questionnaire){
        QuestionnaireDTO questionnaireDTO = new QuestionnaireDTO(questionnaire);
        List<CharacterizationAnketPatient> characterizationAnketPatients = databaseModule.getCharacterizationsForAnket(questionnaireDTO.getId());
        questionnaireDTO.setCharacteristicValues(characterizationAnketPatients);
        return questionnaireDTO;
    }
}
