package com.pancreatitis.modules.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper {

    private static final String OLD_DB_NAME = "pancreatitis_v6.6.db";
    private static final String NEW_DB_NAME = "mobile.db";
    private static final String TARGET_DOCTOR_FIO = "Роман Святославович";
    private static final int TARGET_VERSION = 8;

    private final Context context;
    private final String oldDbPath;
    private final String newDbPath;

    public DatabaseHelper(Context context) {
        this.context = context.getApplicationContext();
        this.oldDbPath = context.getDatabasePath(OLD_DB_NAME).getPath();
        this.newDbPath = context.getDatabasePath(NEW_DB_NAME).getPath();
    }

    /**
     * Вызывать в фоновом потоке.
     * Управление миграцией:
     * - если есть старая БД – выполняется миграция в новую (при необходимости новая копируется из assets);
     * - если старой нет, а новой нет – копируется из assets;
     * - если старой нет, а новая есть – ничего не делается.
     */
    public void create_db() {
        File oldDbFile = new File(oldDbPath);
        File newDbFile = new File(newDbPath);
        boolean oldExists = oldDbFile.exists();
        boolean newExists = newDbFile.exists();

        if (oldExists) {
            // Если есть старая БД – мигрируем в новую
            if (!newExists) {
                copyDatabaseFromAssets(NEW_DB_NAME, newDbPath);
            }
            migrateDataFrom6To7();
            // Удаляем старую БД после успешной миграции
            if (oldDbFile.delete()) {
                Log.d("DatabaseHelper", "Old database deleted");
            } else {
                Log.w("DatabaseHelper", "Could not delete old database");
            }
        } else {
            // Старой нет – просто копируем новую, если её ещё нет
            if (!newExists) {
                copyDatabaseFromAssets(NEW_DB_NAME, newDbPath);
            }
            // иначе новая уже существует – ничего не делаем
        }

        setDatabaseVersion();
    }

    public SQLiteDatabase getReadableDatabase() {
        File dbFile = new File(newDbPath);
        if (!dbFile.exists()) {
            create_db();
        }
        return SQLiteDatabase.openDatabase(newDbPath, null, SQLiteDatabase.OPEN_READONLY);
    }

    public SQLiteDatabase getWritableDatabase() {
        File dbFile = new File(newDbPath);
        if (!dbFile.exists()) {
            create_db();
        }
        return SQLiteDatabase.openDatabase(newDbPath, null, SQLiteDatabase.OPEN_READWRITE);
    }

    private void copyDatabaseFromAssets(String assetName, String destPath) {
        try (InputStream in = context.getAssets().open(assetName);
             OutputStream out = new FileOutputStream(destPath)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.flush();
            Log.d("DatabaseHelper", "Copied " + assetName);
        } catch (IOException e) {
            Log.e("DatabaseHelper", "Failed to copy database from assets", e);
            throw new RuntimeException("Cannot copy database from assets", e);
        }
    }

    // ======================== МИГРАЦИЯ ========================

    private void migrateDataFrom6To7() {
        SQLiteDatabase oldDb = null;
        SQLiteDatabase newDb = null;
        try {
            oldDb = SQLiteDatabase.openDatabase(oldDbPath, null, SQLiteDatabase.OPEN_READONLY);
            newDb = SQLiteDatabase.openDatabase(newDbPath, null, SQLiteDatabase.OPEN_READWRITE);

            newDb.execSQL("PRAGMA foreign_keys=OFF;");
            newDb.beginTransaction();

            // 1. Найти старого доктора по fio
            Cursor doctorCursor = oldDb.rawQuery(
                    "SELECT id, fio, status, last_modified FROM doctors WHERE fio = ?",
                    new String[]{TARGET_DOCTOR_FIO});
            if (!doctorCursor.moveToFirst()) {
                Log.w("Migration", "Doctor with fio=" + TARGET_DOCTOR_FIO + " not found in old DB");
                doctorCursor.close();
                newDb.setTransactionSuccessful();
                return;
            }
            long oldDoctorId = doctorCursor.getLong(0);
            String fio = doctorCursor.getString(1);
            String status = doctorCursor.getString(2);
            String lastModified = doctorCursor.getString(3);
            doctorCursor.close();

            // 2. Создать нового доктора в новой БД
            ContentValues cvDoctor = new ContentValues();
            cvDoctor.put("login", "dr_roman");
            cvDoctor.put("fio", fio);
            cvDoctor.putNull("phone");
            cvDoctor.putNull("email");
            cvDoctor.put("status", status);
            cvDoctor.put("created_at", lastModified);
            long newDoctorId = newDb.insert("doctors", null, cvDoctor);
            Log.d("Migration", "Created new doctor with id=" + newDoctorId + " for fio=" + fio);

            // 3. Найти всех пациентов, на которых ссылаются анкеты этого доктора
            Cursor patientCursor = oldDb.rawQuery(
                    "SELECT DISTINCT id_patient FROM ankets WHERE id_doctor = ?",
                    new String[]{String.valueOf(oldDoctorId)});
            while (patientCursor.moveToNext()) {
                long patientId = patientCursor.getLong(0);
                Cursor pData = oldDb.rawQuery(
                        "SELECT fio, last_modified FROM patients WHERE id = ?",
                        new String[]{String.valueOf(patientId)});
                if (pData.moveToFirst()) {
                    String patientFio = pData.getString(0);
                    String patientLastModified = pData.getString(1);
                    ContentValues cvPatient = new ContentValues();
                    cvPatient.put("id", patientId);       // сохраняем тот же id, чтобы ссылки в анкетах работали
                    cvPatient.put("fio", patientFio);
                    cvPatient.put("last_modified", patientLastModified);
                    newDb.insertWithOnConflict("patients", null, cvPatient, SQLiteDatabase.CONFLICT_REPLACE);
                    Log.d("Migration", "Copied patient id=" + patientId);
                }
                pData.close();
            }
            patientCursor.close();

            // 4. Перенести все анкеты этого доктора и сохранить маппинг старых id -> новые id
            Map<Long, Long> anketIdMap = new HashMap<>();
            Cursor ankCursor = oldDb.rawQuery(
                    "SELECT id, id_patient, id_doctor, id_expert, diagnosis, date_of_completion, admitted_from, last_modified " +
                            "FROM ankets WHERE id_doctor = ?",
                    new String[]{String.valueOf(oldDoctorId)});
            int ankCount = 0;
            while (ankCursor.moveToNext()) {
                long oldAnkId = ankCursor.getLong(0);
                long idPatient = ankCursor.getLong(1);
                long idExpertOld = ankCursor.getLong(3);
                String diagnosis = ankCursor.getString(4);
                String dateOfCompletion = ankCursor.getString(5);
                String admittedFrom = ankCursor.getString(6);
                String lastModifiedAnk = ankCursor.getString(7);

                ContentValues cvAnk = new ContentValues();
                cvAnk.put("id_patient", idPatient);
                cvAnk.put("id_doctor", newDoctorId);
                if (idExpertOld == oldDoctorId) {
                    cvAnk.put("id_expert", newDoctorId);
                } else {
                    cvAnk.putNull("id_expert");
                }
                cvAnk.put("diagnosis", diagnosis);
                cvAnk.put("date_of_completion", dateOfCompletion);
                cvAnk.put("admitted_from", admittedFrom);
                cvAnk.put("last_modified", lastModifiedAnk);

                long newAnkId = newDb.insert("ankets", null, cvAnk);
                anketIdMap.put(oldAnkId, newAnkId);
                ankCount++;
            }
            ankCursor.close();
            Log.d("Migration", "Migrated " + ankCount + " ankets for doctor " + fio);

            // 5. Перенести characterization_anket_patient для этих анкет
            if (!anketIdMap.isEmpty()) {
                List<String> oldAnkIds = new ArrayList<>();
                for (Long id : anketIdMap.keySet()) {
                    oldAnkIds.add(String.valueOf(id));
                }
                String idsIn = TextUtils.join(",", oldAnkIds);
                Cursor capCursor = oldDb.rawQuery(
                        "SELECT id_characteristic, id_anket, id_value, value, created_at, last_modified " +
                                "FROM characterization_anket_patient WHERE id_anket IN (" + idsIn + ")",
                        null);
                int capCount = 0;
                while (capCursor.moveToNext()) {
                    long idCharacteristic = capCursor.getLong(0);
                    long oldAnkId = capCursor.getLong(1);
                    long idValue = capCursor.isNull(2) ? 0 : capCursor.getLong(2);
                    Double value = capCursor.isNull(3) ? null : capCursor.getDouble(3);
                    String createdAt = capCursor.getString(4);
                    String lastModifiedCap = capCursor.getString(5);

                    Long newAnkId = anketIdMap.get(oldAnkId);
                    if (newAnkId == null) continue;

                    ContentValues cvCap = new ContentValues();
                    cvCap.put("id_characteristic", idCharacteristic);
                    cvCap.put("id_anket", newAnkId);
                    if (idValue != 0) {
                        cvCap.put("id_value", idValue);
                    } else {
                        cvCap.putNull("id_value");
                    }
                    cvCap.put("value", value);
                    cvCap.put("created_at", createdAt);
                    cvCap.put("last_modified", lastModifiedCap);

                    newDb.insert("characterization_anket_patient", null, cvCap);
                    capCount++;
                }
                capCursor.close();
                Log.d("Migration", "Migrated " + capCount + " characterization_anket_patient records");
            }

            newDb.setTransactionSuccessful();
            newDb.execSQL("PRAGMA user_version = 7;");
            Log.d("Migration", "Migration for single doctor completed successfully");
        } catch (Exception e) {
            Log.e("Migration", "Migration failed", e);
        } finally {
            if (newDb != null) {
                newDb.execSQL("PRAGMA foreign_keys=ON;");
                newDb.endTransaction();
                newDb.close();
            }
            if (oldDb != null) oldDb.close();
        }
    }

    /**
     * Устанавливает пользовательскую версию для новой базы данных.
     * Вызывать после успешной миграции или копирования из assets.
     */
    private void setDatabaseVersion() {
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(newDbPath, null, SQLiteDatabase.OPEN_READWRITE);
            db.execSQL("PRAGMA user_version = " + TARGET_VERSION + ";");
            Log.d("DatabaseHelper", "Database version set to " + TARGET_VERSION);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Failed to set database version", e);
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }
}