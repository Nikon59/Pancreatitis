package com.pancreatitis.modules.database;

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

    private static final String OLD_DB_NAME = "mobile_v6.5.db";
    private static final String NEW_DB_NAME = "mobile_v7.db";

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

            // 1. Очищаем все таблицы в новой БД
            clearAllTables(newDb);

            // 2. Копируем независимые таблицы (не ссылающиеся на doctors)
            copyTableWithLog(oldDb, newDb, "characteristic_type");
            copyTableWithLog(oldDb, newDb, "characteristic");
            copyTableWithLog(oldDb, newDb, "ankets_values");
            copyTableWithLog(oldDb, newDb, "patients");

            // 3. Создаём временную таблицу для старых doctors (чтобы ankets могла ссылаться)
            newDb.execSQL("CREATE TEMP TABLE temp_doctors (id INTEGER PRIMARY KEY, fio TEXT, status, last_modified TEXT)");
            copyDoctorsIntoTemp(oldDb, newDb);

            // 4. Копируем ankets (ссылается на doctors)
            copyTableWithLog(oldDb, newDb, "ankets");
            copyTableWithLog(oldDb, newDb, "characterization_anket_patient");

            // 5. Преобразуем temp_doctors в новую таблицу doctors (с добавленными полями)
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

    private void clearAllTables(SQLiteDatabase db) {
        String[] tables = {"characteristic_type", "characteristic", "ankets_values",
                "doctors", "patients", "ankets", "characterization_anket_patient"};
        for (String table : tables) {
            try {
                db.execSQL("DELETE FROM " + table);
            } catch (Exception e) {
                Log.w("Migration", "Could not clear table " + table + ": " + e.getMessage());
            }
        }
    }

    /**
     * Универсальное копирование таблицы, учитывая разные типы данных (в т.ч. BLOB/INTEGER).
     */
    private void copyTableWithLog(SQLiteDatabase oldDb, SQLiteDatabase newDb, String tableName) {
        try {
            // Проверяем существование таблицы в старой БД
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

            // Получаем колонки из новой таблицы
            Cursor newColCursor = newDb.rawQuery("SELECT * FROM " + tableName + " LIMIT 0", null);
            String[] newColumns = newColCursor.getColumnNames();
            newColCursor.close();

            // Находим пересечение колонок
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

            // Строим INSERT
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
                    String columnName = commonColumns.get(i);
                    int index = dataCursor.getColumnIndex(columnName);
                    if (index == -1) {
                        values[i] = null;
                        continue;
                    }
                    // Универсальное чтение значения любого типа
                    values[i] = getValueFromCursor(dataCursor, index);
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

    /**
     * Безопасно читает значение из курсора, не вызывая исключений при несоответствии типов.
     */
    private Object getValueFromCursor(Cursor cursor, int columnIndex) {
        switch (cursor.getType(columnIndex)) {
            case Cursor.FIELD_TYPE_NULL:
                return null;
            case Cursor.FIELD_TYPE_INTEGER:
                return cursor.getLong(columnIndex);
            case Cursor.FIELD_TYPE_FLOAT:
                return cursor.getDouble(columnIndex);
            case Cursor.FIELD_TYPE_STRING:
                return cursor.getString(columnIndex);
            case Cursor.FIELD_TYPE_BLOB:
                return cursor.getBlob(columnIndex);
            default:
                return null;
        }
    }

    private void copyDoctorsIntoTemp(SQLiteDatabase oldDb, SQLiteDatabase newDb) {
        Cursor c = oldDb.rawQuery("SELECT id, fio, status, last_modified FROM doctors", null);
        int count = 0;
        while (c.moveToNext()) {
            long id = c.getLong(0);
            String fio = c.getString(1);
            Object status = getValueFromCursor(c, 2);  // безопасное чтение (BLOB или INTEGER)
            String lastModified = c.getString(3);
            newDb.execSQL("INSERT INTO temp_doctors (id, fio, status, last_modified) VALUES (?,?,?,?)",
                    new Object[]{id, fio, status, lastModified});
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
            Object status = getValueFromCursor(c, 2);  // может быть Long или byte[] или другое
            String lastModified = c.getString(3);

            // status в старой БД хранился как BLOB или INTEGER. В новой БД тип BLOB,
            // но SQLite позволяет положить туда любой тип, так что оставляем как есть.
            newDb.execSQL("INSERT INTO doctors (id, login, fio, phone, email, status, created_at) VALUES (?,?,?,?,?,?,?)",
                    new Object[]{id, fio, fio, null, null, status, lastModified});
            count++;
        }
        c.close();
        Log.d("Migration", "Inserted " + count + " doctors into final doctors table");
    }
}