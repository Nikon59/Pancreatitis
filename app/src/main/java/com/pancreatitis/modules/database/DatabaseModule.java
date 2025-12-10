package com.pancreatitis.modules.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import com.pancreatitis.models.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DatabaseModule {
    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;
    private Context context;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static DatabaseModule instance;

    private DatabaseModule(){
    }

    public static DatabaseModule getInstance(){
        if (instance == null) {
            instance = new DatabaseModule();
        }
        return instance;
    }

    public void initWork(){
        dbHelper = new DatabaseHelper(context);
        database = dbHelper.getReadableDatabase();
        dbHelper.create_db();
    }

    public void setContext(Context context) {
        this.context = context;
    }

    // ========== Characteristic ==========
    public Characteristic getCharacteristicById(int id) {
        Cursor cursor = null;
        Characteristic characteristic = null;

        try {
            cursor = database.query("characteristic", null, "id = ?",
                    new String[]{String.valueOf(id)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                characteristic = new Characteristic();
                characteristic.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                characteristic.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                characteristic.setOpis(cursor.getString(cursor.getColumnIndexOrThrow("opis")));
                characteristic.setType(cursor.getString(cursor.getColumnIndexOrThrow("type")));
                characteristic.setHints(cursor.getString(cursor.getColumnIndexOrThrow("hints")));
                characteristic.setWeight(cursor.getFloat(cursor.getColumnIndexOrThrow("weight")));
                characteristic.setMinValue(cursor.getInt(cursor.getColumnIndexOrThrow("min_value")));
                characteristic.setMaxValue(cursor.getInt(cursor.getColumnIndexOrThrow("max_value")));

                String lastModifiedStr = cursor.getString(cursor.getColumnIndexOrThrow("last_modified"));
                if (lastModifiedStr != null) {
                    characteristic.setLastModified(LocalDateTime.parse(lastModifiedStr, formatter));
                }
            }
        } catch (Exception e) {
            Log.e("DatabaseModule", "Error getting characteristic by id: " + id, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return characteristic;
    }

    public List<Characteristic> getAllCharacteristics(){
        Cursor cursor = null;
        List<Characteristic> characteristics = new ArrayList<>();

        try {
            cursor = database.query("characteristic", null, null,null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Characteristic characteristic = new Characteristic();
                    characteristic.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                    characteristic.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                    characteristic.setOpis(cursor.getString(cursor.getColumnIndexOrThrow("opis")));
                    characteristic.setType(cursor.getString(cursor.getColumnIndexOrThrow("type")));
                    characteristic.setHints(cursor.getString(cursor.getColumnIndexOrThrow("hints")));
                    characteristic.setWeight(cursor.getFloat(cursor.getColumnIndexOrThrow("weight")));
                    characteristic.setMinValue(cursor.getInt(cursor.getColumnIndexOrThrow("min_value")));
                    characteristic.setMaxValue(cursor.getInt(cursor.getColumnIndexOrThrow("max_value")));

                    String lastModifiedStr = cursor.getString(cursor.getColumnIndexOrThrow("last_modified"));
                    if (lastModifiedStr != null) {
                        characteristic.setLastModified(LocalDateTime.parse(lastModifiedStr, formatter));
                    }

                    characteristics.add(characteristic);

                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DatabaseModule", "Error getting all characteristics", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return characteristics;
    }

    // ========== Doctor ==========
    public Doctor getDoctorById(int id_doctor) {
        Cursor cursor = null;
        Doctor doctor = null;

        try {
            cursor = database.query("doctors", null, "id = ?",
                    new String[]{String.valueOf(id_doctor)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                doctor = new Doctor();
                doctor.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                doctor.setFio(cursor.getString(cursor.getColumnIndexOrThrow("fio")));

                int statusInt = cursor.getInt(cursor.getColumnIndexOrThrow("status"));
                doctor.setStatus(statusInt == 1);

                String lastModifiedStr = cursor.getString(cursor.getColumnIndexOrThrow("last_modified"));
                if (lastModifiedStr != null) {
                    doctor.setLastModified(LocalDateTime.parse(lastModifiedStr, formatter));
                }
            }
        } catch (Exception e) {
            Log.e("DatabaseModule", "Error getting doctor by id: " + id_doctor, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return doctor;
    }
    private long insertDoctor(Doctor doctor) {
        ContentValues values = new ContentValues();
        values.put("fio", doctor.getFio());
        values.put("status", doctor.getStatus() != null ? (doctor.getStatus() ? 1 : 0) : 0);
        values.put("last_modified", LocalDateTime.now().format(formatter));

        return database.insert("doctors", null, values);
    }

    // ========== Patient ==========
    public Patient getPatientById(int id_patient) {
        Cursor cursor = null;
        Patient patient = null;

        try {
            cursor = database.query("patients", null, "id = ?",
                    new String[]{String.valueOf(id_patient)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                patient = new Patient();
                patient.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                patient.setFio(cursor.getString(cursor.getColumnIndexOrThrow("fio")));

                String lastModifiedStr = cursor.getString(cursor.getColumnIndexOrThrow("last_modified"));
                if (lastModifiedStr != null) {
                    patient.setLastModified(LocalDateTime.parse(lastModifiedStr, formatter));
                }
            }
        } catch (Exception e) {
            Log.e("DatabaseModule", "Error getting patient by id: " + id_patient, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return patient;
    }
    public long insertPatient(Patient patient) {
        ContentValues values = new ContentValues();
        values.put("fio", patient.getFio());
        values.put("last_modified", LocalDateTime.now().format(formatter));

        return database.insert("patients", null, values);
    }

    // ========== Questionnaire ==========
    public long insertQuestionnaire(Questionnaire questionnaire){
        ContentValues values = new ContentValues();
        values.put("id_patient", questionnaire.getIdPatient());
        values.put("id_doctor", questionnaire.getIdDoctor());
        values.put("id_expert", questionnaire.getIdExpert());
        values.put("diagnosis", questionnaire.getCodeDiagnosis());
        values.put("admitted_from", questionnaire.getAdmittedFrom());
        values.put("date_of_completion", LocalDateTime.now().format(formatter));
        values.put("last_modified", LocalDateTime.now().format(formatter));

        return database.insert("ankets", null, values);
    }
    public long updateQuestionnaire(Questionnaire questionnaire) {
        ContentValues values = new ContentValues();
        values.put("diagnosis", questionnaire.getCodeDiagnosis());
        values.put("admitted_from", questionnaire.getAdmittedFrom());
        values.put("last_modified", LocalDateTime.now().format(formatter));

        // Указание условия для обновления
        String where = "id = ?";
        String[] whereArgs = new String[] { String.valueOf(questionnaire.getId()) };

        return database.update("ankets", values, where, whereArgs);
    }
    public Questionnaire getQuestionnaireById(int id_questionnaire) {
        Cursor cursor = null;
        Questionnaire questionnaire = null;

        try {
            cursor = database.query("ankets", null, "id = ?",
                    new String[]{String.valueOf(id_questionnaire)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                questionnaire = new Questionnaire();
                questionnaire.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                questionnaire.setIdPatient(cursor.getInt(cursor.getColumnIndexOrThrow("id_patient")));
                questionnaire.setIdDoctor(cursor.getInt(cursor.getColumnIndexOrThrow("id_doctor")));
                questionnaire.setIdExpert(cursor.getInt(cursor.getColumnIndexOrThrow("id_expert")));
                questionnaire.setDiagnosis(cursor.getString(cursor.getColumnIndexOrThrow("diagnosis")));
                questionnaire.setAdmittedFrom(cursor.getString(cursor.getColumnIndexOrThrow("admitted_from")));

                String dateOfCompletionStr = cursor.getString(cursor.getColumnIndexOrThrow("date_of_completion"));
                if (dateOfCompletionStr != null) {
                    questionnaire.setDateOfCompletion(LocalDateTime.parse(dateOfCompletionStr, formatter));
                }

                String lastModifiedStr = cursor.getString(cursor.getColumnIndexOrThrow("last_modified"));
                if (lastModifiedStr != null) {
                    questionnaire.setLastModified(LocalDateTime.parse(lastModifiedStr, formatter));
                }
            }
        } catch (Exception e) {
            Log.e("DatabaseModule", "Error getting anket by id: " + id_questionnaire, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return questionnaire;
    }
    public List<QuestionnaireItem> getAllQuestionnaireItems() {
        List<QuestionnaireItem> questionnaires = new ArrayList<>();
        Cursor cursor = null;

        try {
            String query = "select ankets.id as idQuestionnaire,patients.id as idPatient,patients.fio as fioPatient,ankets.diagnosis as diagnosis from ankets join patients on ankets.id_patient = patients.id where ankets.id_doctor = ? order by patients.fio";
            cursor = database.rawQuery(query, new String[]{String.valueOf(2)}); //здесь будет первая сущность Доктора для неавторизованных - проблема с несколькими, но это очень странно

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    QuestionnaireItem questionnaire = new QuestionnaireItem();
                    questionnaire.setIdQuestionnaire(cursor.getInt(cursor.getColumnIndexOrThrow("idQuestionnaire")));
                    questionnaire.setIdPatient(cursor.getInt(cursor.getColumnIndexOrThrow("idPatient")));
                    questionnaire.setFioPatient(cursor.getString(cursor.getColumnIndexOrThrow("fioPatient")));
                    questionnaire.setDiagnosis(cursor.getString(cursor.getColumnIndexOrThrow("diagnosis")));
                    questionnaires.add(questionnaire);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
        } finally {
            cursor.close();
        }

        return questionnaires;
    }

    // ========== CharacterizationAnketPatient ==========
    public void insertAllCharacterizationAnketPatient(List<CharacterizationAnketPatient> characterizationAnketPatients){
        for (CharacterizationAnketPatient cap : characterizationAnketPatients) {
            insertCharacterizationAnketPatient(cap);
        }
    }
    public long insertCharacterizationAnketPatient(CharacterizationAnketPatient characterization) {
        ContentValues values = new ContentValues();
        values.put("id_characteristic", characterization.getIdCharacteristic());
        values.put("id_anket", characterization.getIdAnket());
        values.put("value", characterization.getValue());
        values.put("last_modified", LocalDateTime.now().format(formatter));

        return database.insert("characterization_anket_patient", null, values);
    }
    public void updateAllCharacterizationAnketPatient(List<CharacterizationAnketPatient> characterizationAnketPatients){
        for (CharacterizationAnketPatient cap : characterizationAnketPatients) {
            updateCharacterizationAnketPatient(cap);
        }
    }
    public int updateCharacterizationAnketPatient(CharacterizationAnketPatient characterization) {
        ContentValues values = new ContentValues();
        values.put("value", characterization.getValue());
        values.put("last_modified", LocalDateTime.now().format(formatter));

        // Указание условия для обновления
        String where = "id_anket = ? AND id_characteristic = ?";
        String[] whereArgs = new String[]{
                String.valueOf(characterization.getIdAnket()),
                String.valueOf(characterization.getIdCharacteristic())
        };

        return database.update("characterization_anket_patient", values, where, whereArgs);
    }
    public List<CharacterizationAnketPatient> getCharacterizationsForAnket(long anketId) {
        List<CharacterizationAnketPatient> characterizations = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = database.query("characterization_anket_patient", null,
                    "id_anket = ?", new String[]{String.valueOf(anketId)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    CharacterizationAnketPatient characterization = new CharacterizationAnketPatient();
                    characterization.setIdAnket(cursor.getInt(cursor.getColumnIndexOrThrow("id_anket")));
                    characterization.setIdCharacteristic(cursor.getInt(cursor.getColumnIndexOrThrow("id_characteristic")));
                    characterization.setValue(cursor.getInt(cursor.getColumnIndexOrThrow("value")));

                    String lastModifiedStr = cursor.getString(cursor.getColumnIndexOrThrow("last_modified"));
                    if (lastModifiedStr != null) {
                        characterization.setLastModified(LocalDateTime.parse(lastModifiedStr, formatter));
                    }

                    characterizations.add(characterization);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DatabaseModule", "Error getting characterizations for anket", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return characterizations;
    }

    // ========== QuestionnaireValue ==========
    public List<CharacterizationValue> getValuesForCharacteristic(int characteristicId) {
        List<CharacterizationValue> values = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = database.query("ankets_values", null,
                    "id_characteristic = ?", new String[]{String.valueOf(characteristicId)},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    CharacterizationValue characterizationValue = new CharacterizationValue();
                    characterizationValue.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                    characterizationValue.setIdCharacteristic(cursor.getInt(cursor.getColumnIndexOrThrow("id_characteristic")));
                    characterizationValue.setIdValue(cursor.getInt(cursor.getColumnIndexOrThrow("id_value")));
                    characterizationValue.setValue(cursor.getString(cursor.getColumnIndexOrThrow("value")));

                    String lastModifiedStr = cursor.getString(cursor.getColumnIndexOrThrow("last_modified"));
                    if (lastModifiedStr != null) {
                        characterizationValue.setLastModified(LocalDateTime.parse(lastModifiedStr, formatter));
                    }

                    values.add(characterizationValue);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DatabaseModule", "Error getting anket values for characteristic", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return values;
    }

    // ========== Вспомогательные методы ==========
    public void close() {
        if (database != null && database.isOpen()) {
            database.close();
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    public void beginTransaction() {
        database.beginTransaction();
    }

    public void setTransactionSuccessful() {
        database.setTransactionSuccessful();
    }

    public void endTransaction() {
        database.endTransaction();
    }
}
