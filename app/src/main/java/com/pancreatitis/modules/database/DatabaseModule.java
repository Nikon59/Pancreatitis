package com.pancreatitis.modules.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.pancreatitis.models.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DatabaseModule {
    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public DatabaseModule(Context context){
        dbHelper = new DatabaseHelper(context);
        database = dbHelper.getReadableDatabase();
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
                characteristic.setWeight(cursor.getInt(cursor.getColumnIndexOrThrow("weight")));
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
    public long insertCharacteristic(Characteristic characteristic) {
        try {
            ContentValues values = new ContentValues();
            values.put("name", characteristic.getName());
            values.put("opis", characteristic.getOpis());
            values.put("type", characteristic.getType());
            values.put("hints", characteristic.getHints());
            values.put("weight", characteristic.getWeight());
            values.put("min_value", characteristic.getMinValue());
            values.put("max_value", characteristic.getMaxValue());
            values.put("last_modified", characteristic.getLastModified() != null ?
                    characteristic.getLastModified().format(formatter) : null);

            return database.insert("characteristic", null, values);
        } catch (Exception e) {
            Log.e("DatabaseModule", "Error inserting characteristic", e);
            return -1;
        }
    }

    // ========== Doctor ==========
    public Doctor getDoctorById(Integer id_doctor) {
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
    public long insertDoctor(Doctor doctor) {
        try {
            ContentValues values = new ContentValues();
            values.put("fio", doctor.getFio());
            values.put("status", doctor.getStatus() != null ? (doctor.getStatus() ? 1 : 0) : null);
            values.put("last_modified", doctor.getLastModified() != null ?
                    doctor.getLastModified().format(formatter) : null);

            return database.insert("doctors", null, values);
        } catch (Exception e) {
            Log.e("DatabaseModule", "Error inserting doctor", e);
            return -1;
        }
    }

    // ========== Patient ==========
    public Patient getPatientById(Integer id_patient) {
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
        try {
            ContentValues values = new ContentValues();
            values.put("fio", patient.getFio());
            values.put("last_modified", patient.getLastModified() != null ?
                    patient.getLastModified().format(formatter) : null);

            return database.insert("patients", null, values);
        } catch (Exception e) {
            Log.e("DatabaseModule", "Error inserting patient", e);
            return -1;
        }
    }

    // ========== Questionnaire ==========
    // не доделано!
    public Questionnaire getQuestionnaireById(Integer id_questionnaire) {
        Cursor cursor = null;
        Questionnaire questionnaire = null;

        try {
            cursor = database.query("ankets", null, "id = ?",
                    new String[]{String.valueOf(id_questionnaire)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                questionnaire = new Questionnaire();
                questionnaire.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                Integer id_patient = cursor.getInt(cursor.getColumnIndexOrThrow("id_patient"));
                questionnaire.setPatient(getPatientById(id_patient));
                Integer id_doctor = cursor.getInt(cursor.getColumnIndexOrThrow("id_doctor"));
                questionnaire.setDoctor(getDoctorById(id_doctor));
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

                List<CharacterizationValue> characterizationValues = getCharacterizationValuesForCharacteristic(id_questionnaire);
                questionnaire.setCharacterizationValue(characterizationValues);
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
    public long insertQuestionnaire(Questionnaire questionnaire) {
        try {
            ContentValues values = new ContentValues();
            values.put("id_patient", questionnaire.getPatient().getId());
            values.put("id_doctor", questionnaire.getDoctor().getId());
            values.put("id_expert", questionnaire.getIdExpert());
            values.put("diagnosis", questionnaire.getDiagnosis());
            values.put("date_of_completion", questionnaire.getDateOfCompletion() != null ?
                    questionnaire.getDateOfCompletion().format(formatter) : null);
            values.put("admitted_from", questionnaire.getAdmittedFrom());
            values.put("last_modified", questionnaire.getLastModified() != null ?
                    questionnaire.getLastModified().format(formatter) : null);

            return database.insert("ankets", null, values);
        } catch (Exception e) {
            Log.e("DatabaseModule", "Error inserting anket", e);
            return -1;
        }
    }
    public List<Questionnaire> getAllQuestionnaires() {
        List<Questionnaire> questionnaires = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = database.query("ankets", null, null, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Questionnaire questionnaire = new Questionnaire();
                    questionnaire.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                    Integer idPatient = cursor.getInt(cursor.getColumnIndexOrThrow("id_patient"));
                    Integer idDoctor = cursor.getInt(cursor.getColumnIndexOrThrow("id_doctor"));
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

                    questionnaires.add(questionnaire);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DatabaseModule", "Error getting ankets", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return questionnaires;
    }

    // ========== CharacterizationAnketPatient ==========
    public CharacterizationAnketPatient getCharacterizationByCompositeKey(int characteristicId, int anketId) {
        Cursor cursor = null;
        CharacterizationAnketPatient characterization = null;

        try {
            cursor = database.query("characterization_anket_patient", null,
                    "id_characteristic = ? AND id_anket = ?",
                    new String[]{String.valueOf(characteristicId), String.valueOf(anketId)},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                characterization = new CharacterizationAnketPatient();
                characterization.setIdCharacteristic(cursor.getInt(cursor.getColumnIndexOrThrow("id_characteristic")));
                characterization.setIdAnket(cursor.getInt(cursor.getColumnIndexOrThrow("id_anket")));
                characterization.setValue(cursor.getInt(cursor.getColumnIndexOrThrow("value")));

                String lastModifiedStr = cursor.getString(cursor.getColumnIndexOrThrow("last_modified"));
                if (lastModifiedStr != null) {
                    characterization.setLastModified(LocalDateTime.parse(lastModifiedStr, formatter));
                }
            }
        } catch (Exception e) {
            Log.e("DatabaseModule", "Error getting characterization by keys: " +
                    characteristicId + ", " + anketId, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return characterization;
    }
    public long insertCharacterization(CharacterizationAnketPatient characterization) {
        try {
            ContentValues values = new ContentValues();
            values.put("id_characteristic", characterization.getIdCharacteristic());
            values.put("id_anket", characterization.getIdAnket());
            values.put("value", characterization.getValue());
            values.put("last_modified", characterization.getLastModified() != null ?
                    characterization.getLastModified().format(formatter) : null);

            return database.insert("characterization_anket_patient", null, values);
        } catch (Exception e) {
            Log.e("DatabaseModule", "Error inserting characterization", e);
            return -1;
        }
    }
    public List<CharacterizationAnketPatient> getCharacterizationsForAnket(int anketId) {
        List<CharacterizationAnketPatient> characterizations = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = database.query("characterization_anket_patient", null,
                    "id_anket = ?", new String[]{String.valueOf(anketId)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    CharacterizationAnketPatient characterization = new CharacterizationAnketPatient();
                    characterization.setIdCharacteristic(cursor.getInt(cursor.getColumnIndexOrThrow("id_characteristic")));
                    characterization.setIdAnket(cursor.getInt(cursor.getColumnIndexOrThrow("id_anket")));
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
    public long insertAnketValue(CharacterizationValue anketValue) {
        try {
            ContentValues values = new ContentValues();
            values.put("id_characteristic", anketValue.getIdCharacteristic());
            values.put("id_value", anketValue.getIdValue());
            values.put("value", anketValue.getValue());
            values.put("last_modified", anketValue.getLastModified() != null ?
                    anketValue.getLastModified().format(formatter) : null);

            return database.insert("ankets_values", null, values);
        } catch (Exception e) {
            Log.e("DatabaseModule", "Error inserting anket value", e);
            return -1;
        }
    }
    public List<CharacterizationValue> getCharacterizationValuesForCharacteristic(int characteristicId) {
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

//    public List<Questionnaire> getAllQuestionnaires() {
//        List<Questionnaire> questionnaireItems = new ArrayList<>();
//        String query = "select ankets.id,patients.id,patients.fio,ankets.diagnosis from ankets join patients on ankets.id_patient = patients.id where ankets.id_doctor = ? order by patients.fio";
//        Cursor cursor = database.rawQuery(query, new String[]{String.valueOf(3)});
//        if (cursor.moveToFirst()) {
//            do {
//                int id = cursor.getInt(0);
//                int patientId = cursor.getInt(1);
//                String fio = cursor.getString(2);
//                String diagnosis = cursor.getString(3);
//                questionnaireItems.add(new Questionnaire(id, patientId, fio, codeToDiagnosis(diagnosis)));
//            } while (cursor.moveToNext());
//        }
//        cursor.close();
//        return questionnaireItems;
//    }
//
//    private String codeToDiagnosis(String code){
//        Map<String, String> diagnosisMap = new HashMap<>();
//        diagnosisMap.put("1", "Отечный панкреатит");
//        diagnosisMap.put("5", "Панкреонекроз среднетяжелое течение");
//        diagnosisMap.put("6", "Панкреонекроз тяжелое течение");
//        return diagnosisMap.getOrDefault(code, "-");
//    }
}
