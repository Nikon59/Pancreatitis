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
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String OLD_DB_NAME = "pancreatitis_v6.6.db";   // существующая БД на устройстве
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
            newDb.execSQL("PRAGMA foreign_keys=OFF;");
            newDb.beginTransaction();

            // 1. Очищаем все таблицы
            clearAllTables(newDb);

            // 2. Таблицы без внешних ключей на doctors
            copyTableWithLog(oldDb, newDb, "characteristic_type");
            copyTableWithLog(oldDb, newDb, "characteristic");
            copyTableWithLog(oldDb, newDb, "ankets_values");
            copyTableWithLog(oldDb, newDb, "patients");

            // 3. Создаём временную таблицу для старых doctors (чтобы ankets могла на неё ссылаться)
            newDb.execSQL("CREATE TEMP TABLE temp_doctors (id INTEGER PRIMARY KEY, fio TEXT, status BLOB, last_modified TEXT)");
            copyDoctorsIntoTemp(oldDb, newDb);  // заполняем temp_doctors данными из старой БД

            // 4. Копируем ankets (ссылается на doctors через id_doctor, id_expert)
            copyTableWithLog(oldDb, newDb, "ankets");
            copyTableWithLog(oldDb, newDb, "characterization_anket_patient");

            // 5. Теперь преобразуем temp_doctors в новую таблицу doctors (с добавленными колонками)
            convertTempDoctorsToNewDoctors(newDb);

            // 6. Удаляем временную таблицу
            newDb.execSQL("DROP TABLE temp_doctors");

            newDb.setTransactionSuccessful();
            Log.d("Migration", "All data migrated successfully");
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

    private void copyTableWithLog(SQLiteDatabase oldDb, SQLiteDatabase newDb, String tableName) {
        try {
            // Проверяем, существует ли таблица в старой БД
            Cursor check = oldDb.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[]{tableName});
            boolean exists = check.moveToFirst();
            check.close();
            if (!exists) {
                Log.w("Migration", "Table " + tableName + " does not exist in old DB, skipping");
                return;
            }

            // Получаем колонки из старой таблицы
            Cursor colCursor = oldDb.rawQuery("SELECT * FROM " + tableName + " LIMIT 0", null);
            String[] oldColumns = colCursor.getColumnNames();
            colCursor.close();
            if (oldColumns.length == 0) return;

            // Получаем колонки из новой таблицы (чтобы вставлять только те, что есть)
            Cursor newColCursor = newDb.rawQuery("SELECT * FROM " + tableName + " LIMIT 0", null);
            String[] newColumns = newColCursor.getColumnNames();
            newColCursor.close();

            // Находим пересечение колонок (старые, которые существуют в новой)
            List<String> commonColumns = new ArrayList<>();
            for (String col : oldColumns) {
                for (String newCol : newColumns) {
                    if (col.equals(newCol)) {
                        commonColumns.add(col);
                        break;
                    }
                }
            }
            if (commonColumns.isEmpty()) {
                Log.w("Migration", "No common columns for table " + tableName);
                return;
            }

            // Строим INSERT запрос
            StringBuilder insertSql = new StringBuilder("INSERT INTO " + tableName + " (");
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < commonColumns.size(); i++) {
                if (i > 0) {
                    insertSql.append(",");
                    placeholders.append(",");
                }
                insertSql.append(commonColumns.get(i));
                placeholders.append("?");
            }
            insertSql.append(") VALUES (").append(placeholders).append(")");

            Cursor dataCursor = oldDb.rawQuery("SELECT * FROM " + tableName, null);
            int count = 0;
            while (dataCursor.moveToNext()) {
                Object[] values = new Object[commonColumns.size()];
                for (int i = 0; i < commonColumns.size(); i++) {
                    int columnIndex = dataCursor.getColumnIndex(commonColumns.get(i));
                    if (columnIndex == -1) {
                        values[i] = null;
                        continue;
                    }
                    switch (dataCursor.getType(columnIndex)) {
                        case Cursor.FIELD_TYPE_NULL:
                            values[i] = null;
                            break;
                        case Cursor.FIELD_TYPE_INTEGER:
                            values[i] = dataCursor.getLong(columnIndex);
                            break;
                        case Cursor.FIELD_TYPE_FLOAT:
                            values[i] = dataCursor.getDouble(columnIndex);
                            break;
                        case Cursor.FIELD_TYPE_STRING:
                            values[i] = dataCursor.getString(columnIndex);
                            break;
                        case Cursor.FIELD_TYPE_BLOB:
                            values[i] = dataCursor.getBlob(columnIndex);
                            break;
                        default:
                            values[i] = null;
                    }
                }
                newDb.execSQL(insertSql.toString(), values);
                count++;
            }
            dataCursor.close();
            Log.d("Migration", "Copied " + count + " rows into " + tableName);
        } catch (Exception e) {
            Log.e("Migration", "Error copying table " + tableName, e);
        }
    }

    private void copyDoctorsIntoTemp(SQLiteDatabase oldDb, SQLiteDatabase newDb) {
        Cursor c = oldDb.rawQuery("SELECT id, fio, status, last_modified FROM doctors", null);
        int count = 0;
        while (c.moveToNext()) {
            newDb.execSQL("INSERT INTO temp_doctors (id, fio, status, last_modified) VALUES (?,?,?,?)",
                    new Object[]{c.getLong(0), c.getString(1), c.getBlob(2), c.getString(3)});
            count++;
        }
        c.close();
        Log.d("Migration", "Copied " + count + " doctors into temp_doctors");
    }

    private void convertTempDoctorsToNewDoctors(SQLiteDatabase newDb) {
        Cursor c = newDb.rawQuery("SELECT id, fio, status, last_modified FROM temp_doctors", null);
        int count = 0;
        while (c.moveToNext()) {
            long id = c.getLong(0);
            String fio = c.getString(1);
            byte[] status = c.getBlob(2);
            String lastModified = c.getString(3);
            newDb.execSQL("INSERT INTO doctors (id, login, fio, phone, email, status, created_at) VALUES (?,?,?,?,?,?,?)",
                    new Object[]{id, fio, fio, null, null, status, lastModified});
            count++;
        }
        c.close();
        Log.d("Migration", "Inserted " + count + " doctors into final doctors table");
    }

    private void clearAllTables(SQLiteDatabase db) {
        String[] tables = {"characteristic_type", "characteristic", "ankets_values",
                "doctors", "patients", "ankets", "characterization_anket_patient"};
        for (String table : tables) {
            db.execSQL("DELETE FROM " + table);
        }
    }
}