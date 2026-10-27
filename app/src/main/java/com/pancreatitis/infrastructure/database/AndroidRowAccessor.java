package com.pancreatitis.infrastructure.database;

import android.database.Cursor;
import com.pancreatitis.core.database.RowAccessor;

public class AndroidRowAccessor implements RowAccessor {

    private final Cursor cursor;

    public AndroidRowAccessor(Cursor cursor) {
        this.cursor = cursor;
    }

    @Override
    public int getColumnCount() {
        return cursor.getColumnCount();
    }

    @Override
    public String getColumnName(int columnIndex) {
        return cursor.getColumnName(columnIndex);
    }

    @Override
    public int getColumnIndex(String columnName) {
        return cursor.getColumnIndexOrThrow(columnName);
    }

    @Override
    public boolean isNull(int columnIndex) {
        return cursor.isNull(columnIndex);
    }

    @Override
    public boolean isNull(String columnName) {
        return cursor.isNull(cursor.getColumnIndexOrThrow(columnName));
    }

    @Override
    public String getString(int columnIndex) {
        return cursor.getString(columnIndex);
    }

    @Override
    public String getString(String columnName) {
        return cursor.getString(cursor.getColumnIndexOrThrow(columnName));
    }

    @Override
    public int getInt(int columnIndex) {
        return cursor.getInt(columnIndex);
    }

    @Override
    public int getInt(String columnName) {
        return cursor.getInt(cursor.getColumnIndexOrThrow(columnName));
    }

    @Override
    public long getLong(int columnIndex) {
        return cursor.getLong(columnIndex);
    }

    @Override
    public long getLong(String columnName) {
        return cursor.getLong(cursor.getColumnIndexOrThrow(columnName));
    }

    @Override
    public float getFloat(int columnIndex) {
        return cursor.getFloat(columnIndex);
    }

    @Override
    public float getFloat(String columnName) {
        return cursor.getFloat(cursor.getColumnIndexOrThrow(columnName));
    }

    @Override
    public double getDouble(int columnIndex) {
        return cursor.getDouble(columnIndex);
    }

    @Override
    public double getDouble(String columnName) {
        return cursor.getDouble(cursor.getColumnIndexOrThrow(columnName));
    }

    @Override
    public byte[] getBlob(int columnIndex) {
        return cursor.getBlob(columnIndex);
    }

    @Override
    public byte[] getBlob(String columnName) {
        return cursor.getBlob(cursor.getColumnIndexOrThrow(columnName));
    }
}
