package com.pancreatitis.modules.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String OLD_DB_NAME = "pancreatitis_v6.5.db";   // существующая БД на устройстве
    private static final String NEW_DB_NAME = "mobile.db";     // имя БД в assets
    private static final int SCHEMA = 7;

    private final Context myContext;
    private final String oldDbPath;
    private final String newDbPath;

    public DatabaseHelper(Context context) {
        super(context, NEW_DB_NAME, null, SCHEMA);
        this.myContext = context;
        this.newDbPath = context.getDatabasePath(NEW_DB_NAME).getPath();
        this.oldDbPath = context.getDatabasePath(OLD_DB_NAME).getPath();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Новая БД будет скопирована из assets в create_db()
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // не используется
    }

    /**
     * Копирует новую БД из assets (если отсутствует),
     * затем переносит данные из старой БД (если существует).
     */
    public void create_db() {
        // 1. Копируем новую БД из assets, если её ещё нет
        File newDbFile = new File(newDbPath);
        if (!newDbFile.exists()) {
            copyDatabaseFromAssets(NEW_DB_NAME, newDbPath);
        }

        // 2. Если есть старая БД – выполняем миграцию
        File oldDbFile = new File(oldDbPath);
        if (oldDbFile.exists()) {
            migrateDataFromOldToNew();
            // 3. Удаляем старую БД, чтобы не повторять миграцию
            if (oldDbFile.delete()) {
                Log.d("DatabaseHelper", "Old database deleted");
            } else {
                Log.w("DatabaseHelper", "Could not delete old database");
            }
        }
    }

    private void copyDatabaseFromAssets(String assetName, String destPath) {
        try (InputStream in = myContext.getAssets().open(assetName);
             OutputStream out = new FileOutputStream(destPath)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.flush();
            Log.d("DatabaseHelper", "Copied " + assetName + " to " + destPath);
        } catch (IOException e) {
            Log.e("DatabaseHelper", "Failed to copy database from assets", e);
        }
    }

    private void migrateDataFromOldToNew() {
        SQLiteDatabase oldDb = null;
        SQLiteDatabase newDb = null;
        try {
            oldDb = SQLiteDatabase.openDatabase(oldDbPath, null, SQLiteDatabase.OPEN_READONLY);
            newDb = SQLiteDatabase.openDatabase(newDbPath, null, SQLiteDatabase.OPEN_READWRITE);

            newDb.beginTransaction();

            // Очищаем все таблицы в новой БД перед вставкой
            clearAllTables(newDb);

            // Копируем таблицы без изменений
            copyTable(oldDb, newDb, "characteristic_type");
            copyTable(oldDb, newDb, "characteristic");
            copyTable(oldDb, newDb, "ankets_values");
            copyTable(oldDb, newDb, "patients");
            copyTable(oldDb, newDb, "ankets");
            copyTable(oldDb, newDb, "characterization_anket_patient");

            // Копируем doctors с преобразованием полей
            copyDoctorsTable(oldDb, newDb);

            newDb.setTransactionSuccessful();
            Log.d("DatabaseHelper", "Migration succeeded");
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Migration failed", e);
        } finally {
            if (newDb != null) {
                newDb.endTransaction();
                newDb.close();
            }
            if (oldDb != null) {
                oldDb.close();
            }
        }
    }

    private void clearAllTables(SQLiteDatabase db) {
        String[] tables = {"characteristic_type", "characteristic", "ankets_values",
                "doctors", "patients", "ankets", "characterization_anket_patient"};
        for (String table : tables) {
            db.execSQL("DELETE FROM " + table);
        }
    }

    /**
     * Универсальное копирование таблицы со всеми столбцами (структура идентична).
     */
    private void copyTable(SQLiteDatabase oldDb, SQLiteDatabase newDb, String tableName) {
        // Получаем список столбцов из старой таблицы
        Cursor columnCursor = oldDb.rawQuery("SELECT * FROM " + tableName + " LIMIT 0", null);
        String[] columns = columnCursor.getColumnNames();
        columnCursor.close();
        if (columns.length == 0) return;

        // Формируем INSERT
        StringBuilder insertSql = new StringBuilder("INSERT INTO " + tableName + " (");
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                insertSql.append(",");
                placeholders.append(",");
            }
            insertSql.append(columns[i]);
            placeholders.append("?");
        }
        insertSql.append(") VALUES (").append(placeholders).append(")");

        Cursor dataCursor = oldDb.rawQuery("SELECT * FROM " + tableName, null);
        try {
            while (dataCursor.moveToNext()) {
                Object[] values = new Object[columns.length];
                for (int i = 0; i < columns.length; i++) {
                    switch (dataCursor.getType(i)) {
                        case Cursor.FIELD_TYPE_INTEGER:
                            values[i] = dataCursor.getLong(i);
                            break;
                        case Cursor.FIELD_TYPE_FLOAT:
                            values[i] = dataCursor.getDouble(i);
                            break;
                        case Cursor.FIELD_TYPE_STRING:
                            values[i] = dataCursor.getString(i);
                            break;
                        case Cursor.FIELD_TYPE_BLOB:
                            values[i] = dataCursor.getBlob(i);
                            break;
                        default:
                            values[i] = null;
                    }
                }
                newDb.execSQL(insertSql.toString(), values);
            }
        } finally {
            dataCursor.close();
        }
    }

    /**
     * Специальное копирование таблицы doctors с преобразованием схемы:
     * Старые поля: id, fio, status, last_modified
     * Новые поля: id, login, fio, phone, email, status, created_at
     * <p>
     * Логика переноса: login = fio, created_at = last_modified, phone/email = null
     */
    private void copyDoctorsTable(SQLiteDatabase oldDb, SQLiteDatabase newDb) {
        Cursor cursor = oldDb.rawQuery("SELECT id, fio, status, last_modified FROM doctors", null);
        try {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String fio = cursor.getString(1);
                byte[] status = cursor.getBlob(2);
                String lastModified = cursor.getString(3);

                String sql = "INSERT INTO doctors (id, login, fio, phone, email, status, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";
                newDb.execSQL(sql, new Object[]{id, fio, fio, null, null, status, lastModified});
            }
        } finally {
            cursor.close();
        }
    }
}