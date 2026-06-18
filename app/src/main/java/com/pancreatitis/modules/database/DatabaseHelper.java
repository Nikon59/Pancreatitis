package com.pancreatitis.modules.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {

    private static final String OLD_DB_NAME = "pancreatitis_v6.6.db";
    private static final String NEW_DB_NAME = "mobile.db";
    private static final String TARGET_DOCTOR_FIO = "Роман Святославович"; // замените на нужное ФИО

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
     */
    public void create_db() {
        File newDbFile = new File(newDbPath);
        if (!newDbFile.exists()) {
            copyDatabaseFromAssets(NEW_DB_NAME, newDbPath);
        }

        File oldDbFile = new File(oldDbPath);
        if (oldDbFile.exists()) {
            migrateDataFromOldToNew();
            if (oldDbFile.delete()) {
                Log.d("DatabaseHelper", "Old database deleted");
            } else {
                Log.w("DatabaseHelper", "Could not delete old database");
            }
        }
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

    private void migrateDataFromOldToNew() {
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
                // Получить данные пациента
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
                    // Вставляем или заменяем, если пациент с таким id уже существует
                    newDb.insertWithOnConflict("patients", null, cvPatient, SQLiteDatabase.CONFLICT_REPLACE);
                    Log.d("Migration", "Copied patient id=" + patientId);
                }
                pData.close();
            }
            patientCursor.close();

            // 4. Перенести все анкеты этого доктора
            Cursor ankCursor = oldDb.rawQuery(
                    "SELECT id, id_patient, id_doctor, id_expert, diagnosis, date_of_completion, admitted_from, last_modified " +
                            "FROM ankets WHERE id_doctor = ?",
                    new String[]{String.valueOf(oldDoctorId)});
            int ankCount = 0;
            while (ankCursor.moveToNext()) {
                long oldAnkId = ankCursor.getLong(0);
                long idPatient = ankCursor.getLong(1);
                long idExpertOld = ankCursor.getLong(3);  // id_doctor на позиции 2, id_expert на 3
                String diagnosis = ankCursor.getString(4);
                String dateOfCompletion = ankCursor.getString(5);
                String admittedFrom = ankCursor.getString(6);
                String lastModifiedAnk = ankCursor.getString(7);

                ContentValues cvAnk = new ContentValues();
                cvAnk.put("id_patient", idPatient);
                cvAnk.put("id_doctor", newDoctorId);
                cvAnk.putNull("id_expert");
                cvAnk.put("diagnosis", diagnosis);
                cvAnk.put("date_of_completion", dateOfCompletion);
                cvAnk.put("admitted_from", admittedFrom);
                cvAnk.put("last_modified", lastModifiedAnk);
                // Не указываем id явно, чтобы генерировался автоматически
                newDb.insert("ankets", null, cvAnk);
                ankCount++;
            }
            ankCursor.close();
            Log.d("Migration", "Migrated " + ankCount + " ankets for doctor " + fio);

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
}