package com.pancreatitis.core.database;

public interface RowAccessor {

    int getColumnCount();

    String getColumnName(int columnIndex);

    int getColumnIndex(String columnName);

    boolean isNull(int columnIndex);

    boolean isNull(String columnName);

    String getString(int columnIndex);

    String getString(String columnName);

    int getInt(int columnIndex);

    int getInt(String columnName);

    long getLong(int columnIndex);

    long getLong(String columnName);

    float getFloat(int columnIndex);

    float getFloat(String columnName);

    double getDouble(int columnIndex);

    double getDouble(String columnName);

    byte[] getBlob(int columnIndex);

    byte[] getBlob(String columnName);
}
