package com.pancreatitis.modules.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper {

    private static final String TAG = "DatabaseHelper";
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

    public void create_db() {
        File newDbFile = new File(newDbPath);
        boolean newExists = newDbFile.exists();

        if (!newExists) {
            // Нет новой БД — копируем из assets
            copyDatabaseFromAssets(NEW_DB_NAME, newDbPath);
        }

        int currentVersion = getCurrentDatabaseVersion();
        Log.d(TAG, "Current DB version: " + currentVersion);

        if (currentVersion < 7) {
            // Миграция 6 → 7
            migrateDataFrom6To7();
            setDatabaseVersion(7);
            currentVersion = 7;
        }

        if (currentVersion < 8) {
            // Миграция 7 → 8
            migrateDataFrom7to8();
            setDatabaseVersion(8);
        }
    }

    private int getCurrentDatabaseVersion() {
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(newDbPath, null, SQLiteDatabase.OPEN_READONLY);
            return (int) db.getVersion(); // использует PRAGMA user_version
        } catch (Exception e) {
            Log.w(TAG, "Cannot read DB version, assuming 0", e);
            return 0;
        } finally {
            if (db != null) db.close();
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
            Log.d(TAG, "Copied " + assetName);
        } catch (IOException e) {
            Log.e(TAG, "Failed to copy database from assets", e);
            throw new RuntimeException("Cannot copy database from assets", e);
        }
    }

    // ======================== МИГРАЦИЯ 6 → 7 ========================

    private void migrateDataFrom6To7() {
        File oldDbFile = new File(oldDbPath);
        if (!oldDbFile.exists()) {
            Log.w(TAG, "Old DB not found, skipping migration 6→7");
            return;
        }

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
                Log.w(TAG, "Doctor with fio=" + TARGET_DOCTOR_FIO + " not found in old DB");
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
            cvDoctor.put("created_at", lastModified); // last_modified → created_at
            long newDoctorId = newDb.insert("doctors", null, cvDoctor);
            Log.d(TAG, "Created new doctor with id=" + newDoctorId + " for fio=" + fio);

            // 3. Пациенты: связанные с анкетами этого доктора или базовыми (id_doctor = 100)
            Cursor patientCursor = oldDb.rawQuery(
                    "SELECT DISTINCT id_patient FROM ankets WHERE id_doctor IN (?, ?)",
                    new String[]{String.valueOf(oldDoctorId), "100"});
            while (patientCursor.moveToNext()) {
                long patientId = patientCursor.getLong(0);
                Cursor pData = oldDb.rawQuery(
                        "SELECT fio, last_modified FROM patients WHERE id = ?",
                        new String[]{String.valueOf(patientId)});
                if (pData.moveToFirst()) {
                    String patientFio = pData.getString(0);
                    String patientLastModified = pData.getString(1);
                    ContentValues cvPatient = new ContentValues();
                    cvPatient.put("id", patientId);
                    cvPatient.put("fio", patientFio);
                    cvPatient.put("last_modified", patientLastModified);
                    newDb.insertWithOnConflict("patients", null, cvPatient, SQLiteDatabase.CONFLICT_REPLACE);
                    Log.d(TAG, "Copied patient id=" + patientId);
                }
                pData.close();
            }
            patientCursor.close();

            // 4. Анкеты
            Map<Long, Long> anketIdMap = new HashMap<>();
            Cursor ankCursor = oldDb.rawQuery(
                    "SELECT id, id_patient, id_doctor, id_expert, diagnosis, date_of_completion, admitted_from, last_modified " +
                            "FROM ankets WHERE id_doctor IN (?, ?)",
                    new String[]{String.valueOf(oldDoctorId), "100"});
            int ankCount = 0;
            while (ankCursor.moveToNext()) {
                long oldAnkId = ankCursor.getLong(0);
                long idPatient = ankCursor.getLong(1);
                long oldIdDoctor = ankCursor.getLong(2);
                long idExpertOld = ankCursor.getLong(3);
                String diagnosis = ankCursor.getString(4);
                String dateOfCompletion = ankCursor.getString(5);
                String admittedFrom = ankCursor.getString(6);
                String lastModifiedAnk = ankCursor.getString(7);

                Long newIdDoctor = 100L;
                if (oldIdDoctor == oldDoctorId) {
                    newIdDoctor = newDoctorId;
                }

                ContentValues cvAnk = new ContentValues();
                cvAnk.put("id_patient", idPatient);
                cvAnk.put("id_doctor", newIdDoctor);
                cvAnk.put("id_expert", idExpertOld);
                cvAnk.put("diagnosis", diagnosis);
                cvAnk.put("date_of_completion", dateOfCompletion);
                cvAnk.put("admitted_from", admittedFrom);
                cvAnk.put("last_modified", lastModifiedAnk);

                long newAnkId = newDb.insert("ankets", null, cvAnk);
                anketIdMap.put(oldAnkId, newAnkId);
                ankCount++;
            }
            ankCursor.close();
            Log.d(TAG, "Migrated " + ankCount + " ankets");

            // 5. characterization_anket_patient
            if (!anketIdMap.isEmpty()) {
                List<String> oldAnkIds = new ArrayList<>();
                for (Long id : anketIdMap.keySet()) {
                    oldAnkIds.add(String.valueOf(id));
                }
                String idsIn = TextUtils.join(",", oldAnkIds); // ← без пробелов!
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
                Log.d(TAG, "Migrated " + capCount + " characterization records");
            }

            newDb.setTransactionSuccessful();
            Log.d(TAG, "Migration 6→7 completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Migration 6→7 failed", e);
        } finally {
            if (newDb != null) {
                newDb.execSQL("PRAGMA foreign_keys=ON;");
                newDb.endTransaction();
                newDb.close();
            }
            if (oldDb != null) oldDb.close();
        }

        // Удаляем старую БД после успешной миграции
        if (oldDbFile.exists()) {
            if (oldDbFile.delete()) {
                Log.d(TAG, "Old database deleted");
            } else {
                Log.w(TAG, "Could not delete old database");
            }
        }
    }

    // ======================== МИГРАЦИЯ 7 → 8 ========================

    private void migrateDataFrom7to8() {
        String tempDbPath = newDbPath + ".tmp";
        copyDatabaseFromAssets(NEW_DB_NAME, tempDbPath);

        SQLiteDatabase oldDb = null;
        SQLiteDatabase newDb = null;
        try {
            oldDb = SQLiteDatabase.openDatabase(newDbPath, null, SQLiteDatabase.OPEN_READONLY);
            newDb = SQLiteDatabase.openDatabase(tempDbPath, null, SQLiteDatabase.OPEN_READWRITE);

            newDb.execSQL("PRAGMA foreign_keys=OFF;");
            newDb.beginTransaction();

            // Копируем пользовательские таблицы
            copyTable(oldDb, newDb, "doctors");
            copyTable(oldDb, newDb, "patients");
            copyTable(oldDb, newDb, "ankets");
            copyTable(oldDb, newDb, "characterization_anket_patient");
            // ankets_values — справочник, не копируем (берём из assets)

            newDb.setTransactionSuccessful();
            Log.d(TAG, "Migration 7→8 data copied successfully");

        } catch (Exception e) {
            Log.e(TAG, "Migration 7→8 failed", e);
            new File(tempDbPath).delete();
            return;
        } finally {
            if (newDb != null) {
                newDb.execSQL("PRAGMA foreign_keys=ON;");
                newDb.endTransaction();
                newDb.close();
            }
            if (oldDb != null) oldDb.close();
        }

        // Замена БД
        File oldFile = new File(newDbPath);
        File tempFile = new File(tempDbPath);
        boolean replaced = false;

        if (oldFile.exists() && oldFile.delete()) {
            if (tempFile.renameTo(oldFile)) {
                replaced = true;
                Log.d(TAG, "Replaced DB with migrated version");
            }
        }

        if (!replaced) {
            try (FileInputStream in = new FileInputStream(tempFile);
                 FileOutputStream out = new FileOutputStream(oldFile)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
                out.flush();
                replaced = true;
                Log.d(TAG, "Copied temp DB to final location");
            } catch (IOException e) {
                Log.e(TAG, "Failed to copy temp DB", e);
            }
        }

        if (tempFile.exists()) tempFile.delete();
    }

    private void copyTable(SQLiteDatabase source, SQLiteDatabase dest, String tableName) {
        Cursor cursor = source.query(tableName, null, null, null, null, null, null);
        if (cursor == null) {
            Log.w(TAG, "Table " + tableName + " not found in source DB");
            return;
        }
        try {
            String[] columns = cursor.getColumnNames();
            ContentValues values = new ContentValues();
            while (cursor.moveToNext()) {
                values.clear();
                for (String col : columns) {
                    int index = cursor.getColumnIndex(col);
                    if (index == -1) continue;
                    int type = cursor.getType(index);
                    switch (type) {
                        case Cursor.FIELD_TYPE_NULL:
                            values.putNull(col);
                            break;
                        case Cursor.FIELD_TYPE_INTEGER:
                            values.put(col, cursor.getLong(index));
                            break;
                        case Cursor.FIELD_TYPE_FLOAT:
                            values.put(col, cursor.getDouble(index));
                            break;
                        case Cursor.FIELD_TYPE_STRING:
                            values.put(col, cursor.getString(index));
                            break;
                        case Cursor.FIELD_TYPE_BLOB:
                            values.put(col, cursor.getBlob(index));
                            break;
                        default:
                            values.put(col, cursor.getString(index));
                            break;
                    }
                }
                dest.insertWithOnConflict(tableName, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            Log.d(TAG, "Copied " + cursor.getCount() + " rows from " + tableName);
        } finally {
            cursor.close();
        }
    }

    private void setDatabaseVersion(int version) {
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(newDbPath, null, SQLiteDatabase.OPEN_READWRITE);
            db.setVersion(version); // это делает PRAGMA user_version = ?
            Log.d(TAG, "Database version set to " + version);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set database version to " + version, e);
        } finally {
            if (db != null) db.close();
        }
    }
}