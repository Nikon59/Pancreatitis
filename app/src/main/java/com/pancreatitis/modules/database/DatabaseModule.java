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
                characteristic.setWeight(cursor.getInt(cursor.getColumnIndexOrThrow("weight")));
                characteristic.setMinValue(cursor.getInt(cursor.getColumnIndexOrThrow("min_value")));
                characteristic.setMaxValue(cursor.getInt(cursor.getColumnIndexOrThrow("max_value")));

                String lastModifiedStr = cursor.getString(cursor.getColumnIndexOrThrow("last_modified"));
                if (lastModifiedStr != null) {
                    characteristic.setLastModified(LocalDateTime.parse(lastModifiedStr, formatter));
                }

                List<CharacterizationValue> characterizationValues = getCharacterizationValuesForCharacteristic(id);
                characteristic.setCharacterizationValue(characterizationValues);
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
                    characteristic.setWeight(cursor.getInt(cursor.getColumnIndexOrThrow("weight")));
                    characteristic.setMinValue(cursor.getInt(cursor.getColumnIndexOrThrow("min_value")));
                    characteristic.setMaxValue(cursor.getInt(cursor.getColumnIndexOrThrow("max_value")));

                    String lastModifiedStr = cursor.getString(cursor.getColumnIndexOrThrow("last_modified"));
                    if (lastModifiedStr != null) {
                        characteristic.setLastModified(LocalDateTime.parse(lastModifiedStr, formatter));
                    }

                    List<CharacterizationValue> characterizationValues = getCharacterizationValuesForCharacteristic(characteristic.getId());
                    characteristic.setCharacterizationValue(characterizationValues);

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
    private long insertDoctor(Doctor doctor) {
        ContentValues values = new ContentValues();
        values.put("fio", doctor.getFio());
        values.put("status", doctor.getStatus() != null ? (doctor.getStatus() ? 1 : 0) : null);
        values.put("last_modified", doctor.getLastModified() != null ?
                doctor.getLastModified().format(formatter) : null);

        return database.insert("doctors", null, values);
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
    private long insertPatient(Patient patient) {
        ContentValues values = new ContentValues();
        values.put("fio", patient.getFio());
        values.put("last_modified", patient.getLastModified() != null ?
                patient.getLastModified().format(formatter) : null);

        return database.insert("patients", null, values);
    }

    // ========== Questionnaire ==========
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

                List<CharacterizationAnketPatient> characterizationAnketPatients = getCharacterizationsForAnket(id_questionnaire);
                questionnaire.setCharacterizationAnketPatients(characterizationAnketPatients);
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
            database.beginTransaction();

            Patient patient = questionnaire.getPatient();
            long id_patient = insertPatient(patient);
            patient.setId(id_patient);

            Doctor doctor = questionnaire.getDoctor();
            long id_doctor = insertDoctor(doctor);
            doctor.setId(id_doctor);

            ContentValues values = new ContentValues();
            values.put("id_patient", id_patient);
            values.put("id_doctor", id_doctor);
            values.put("id_expert", questionnaire.getIdExpert());
            values.put("diagnosis", questionnaire.getDiagnosis());
            values.put("date_of_completion", questionnaire.getDateOfCompletion() != null ?
                    questionnaire.getDateOfCompletion().format(formatter) : null);
            values.put("admitted_from", questionnaire.getAdmittedFrom());
            values.put("last_modified", questionnaire.getLastModified() != null ?
                    questionnaire.getLastModified().format(formatter) : null);

            long id_questionnaire = database.insert("ankets", null, values);
            questionnaire.setId(id_questionnaire);

            questionnaire.setIdCharacterizationAnketPatient();
            insertAllCharacterization(questionnaire.getCharacterizationAnketPatients());

            database.setTransactionSuccessful();
            return id_questionnaire;
        } catch (Exception e) {
            Log.e("DatabaseModule", "Error inserting anket", e);
            return -1;
        } finally {
            database.endTransaction();
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

//                    List<CharacterizationAnketPatient> characterizationAnketPatients = getCharacterizationsForAnket(questionnaire.getId());
//                    questionnaire.setCharacterizationAnketPatients(characterizationAnketPatients);

                    questionnaires.add(questionnaire);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // Сортировка списка опираясь на fio пациента
        Collections.sort(questionnaires, new Comparator<Questionnaire>() {
            @Override
            public int compare(Questionnaire q1, Questionnaire q2) {
                String fio1 = q1.getPatient() != null ? q1.getPatient().getFio() : "";
                String fio2 = q2.getPatient() != null ? q2.getPatient().getFio() : "";
                return fio1.compareTo(fio2);
            }
        });

        return questionnaires;
    }

    // ========== CharacterizationAnketPatient ==========
    private void insertAllCharacterization(List<CharacterizationAnketPatient> characterizationAnketPatients){
        for (CharacterizationAnketPatient cap : characterizationAnketPatients) {
            insertCharacterization(cap);
        }
    }
    private long insertCharacterization(CharacterizationAnketPatient characterization) {
        ContentValues values = new ContentValues();
        values.put("id_characteristic", characterization.getCharacteristic().getId());
        values.put("id_anket", characterization.getIdAnket());
        values.put("value", characterization.getValue());
        values.put("last_modified", characterization.getLastModified() != null ?
                characterization.getLastModified().format(formatter) : null);

        return database.insert("characterization_anket_patient", null, values);
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
                    characterization.setValue(cursor.getInt(cursor.getColumnIndexOrThrow("value")));

                    String lastModifiedStr = cursor.getString(cursor.getColumnIndexOrThrow("last_modified"));
                    if (lastModifiedStr != null) {
                        characterization.setLastModified(LocalDateTime.parse(lastModifiedStr, formatter));
                    }

                    Integer id_characteristic = cursor.getInt(cursor.getColumnIndexOrThrow("id_characteristic"));
                    Characteristic characteristic = getCharacteristicById(id_characteristic);
                    characterization.setCharacteristic(characteristic);

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
}
