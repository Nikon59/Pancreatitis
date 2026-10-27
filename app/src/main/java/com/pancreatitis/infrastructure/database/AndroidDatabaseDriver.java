package com.pancreatitis.infrastructure.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pancreatitis.core.database.ColumnValues;
import com.pancreatitis.core.database.DatabaseDriver;
import com.pancreatitis.core.database.RowAccessor;
import com.pancreatitis.core.database.RowMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AndroidDatabaseDriver implements DatabaseDriver {

    private final SQLiteDatabase database;

    public AndroidDatabaseDriver(SQLiteDatabase database) {
        this.database = database;
    }

    @Override
    public void execute(String sql, Object... bindArgs) {
        if (bindArgs == null || bindArgs.length == 0) {
            database.execSQL(sql);
        } else {
            String[] stringArgs = new String[bindArgs.length];
            for (int i = 0; i < bindArgs.length; i++) {
                stringArgs[i] = bindArgs[i] != null ? bindArgs[i].toString() : null;
            }
            database.execSQL(sql, stringArgs);
        }
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... bindArgs) {
        String[] stringArgs = null;
        if (bindArgs != null && bindArgs.length > 0) {
            stringArgs = new String[bindArgs.length];
            for (int i = 0; i < bindArgs.length; i++) {
                stringArgs[i] = bindArgs[i] != null ? bindArgs[i].toString() : "";
            }
        }

        List<T> result = new ArrayList<>();
        try (Cursor cursor = database.rawQuery(sql, stringArgs)) {
            RowAccessor accessor = new AndroidRowAccessor(cursor);
            while (cursor.moveToNext()) {
                result.add(mapper.mapRow(accessor));
            }
        }
        return result;
    }

    @Override
    public <T> Optional<T> queryForObject(String sql, RowMapper<T> mapper, Object... bindArgs) {
        List<T> list = query(sql, mapper, bindArgs);
        if (list.isEmpty() || list.get(0) == null) {
            return Optional.empty();
        }
        return Optional.of(list.get(0));
    }

    @Override
    public long insertOrReplace(String table, ColumnValues values) {
        ContentValues contentValues = toContentValues(values);
        return database.insertWithOnConflict(table, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Override
    public int update(String table, ColumnValues values, String whereClause, Object... whereArgs) {
        ContentValues contentValues = toContentValues(values);
        String[] stringWhereArgs = null;
        if (whereArgs != null && whereArgs.length > 0) {
            stringWhereArgs = new String[whereArgs.length];
            for (int i = 0; i < whereArgs.length; i++) {
                stringWhereArgs[i] = whereArgs[i] != null ? whereArgs[i].toString() : "";
            }
        }
        return database.update(table, contentValues, whereClause, stringWhereArgs);
    }

    @Override
    public int delete(String table, String whereClause, Object... whereArgs) {
        String[] stringWhereArgs = null;
        if (whereArgs != null && whereArgs.length > 0) {
            stringWhereArgs = new String[whereArgs.length];
            for (int i = 0; i < whereArgs.length; i++) {
                stringWhereArgs[i] = whereArgs[i] != null ? whereArgs[i].toString() : "";
            }
        }
        return database.delete(table, whereClause, stringWhereArgs);
    }

    @Override
    public void beginTransaction() {
        database.beginTransaction();
    }

    @Override
    public void setTransactionSuccessful() {
        database.setTransactionSuccessful();
    }

    @Override
    public void endTransaction() {
        database.endTransaction();
    }

    @Override
    public void close() {
        if (database.isOpen()) {
            database.close();
        }
    }

    private ContentValues toContentValues(ColumnValues values) {
        ContentValues contentValues = new ContentValues();
        if (values == null) {
            return contentValues;
        }
        for (Map.Entry<String, Object> entry : values.asMap().entrySet()) {
            String key = entry.getKey();
            Object v = entry.getValue();
            if (v == null) {
                contentValues.putNull(key);
            } else if (v instanceof String) {
                contentValues.put(key, (String) v);
            } else if (v instanceof Integer) {
                contentValues.put(key, (Integer) v);
            } else if (v instanceof Long) {
                contentValues.put(key, (Long) v);
            } else if (v instanceof Float) {
                contentValues.put(key, (Float) v);
            } else if (v instanceof Double) {
                contentValues.put(key, (Double) v);
            } else if (v instanceof Boolean) {
                contentValues.put(key, (Boolean) v ? 1 : 0);
            } else if (v instanceof byte[]) {
                contentValues.put(key, (byte[]) v);
            } else {
                contentValues.put(key, v.toString());
            }
        }
        return contentValues;
    }
}
