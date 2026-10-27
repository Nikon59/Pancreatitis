package com.pancreatitis.core.database;

import java.util.List;
import java.util.Optional;

public interface DatabaseDriver extends AutoCloseable {

    void execute(String sql, Object... bindArgs);

    <T> List<T> query(String sql, RowMapper<T> mapper, Object... bindArgs);

    <T> Optional<T> queryForObject(String sql, RowMapper<T> mapper, Object... bindArgs);

    long insertOrReplace(String table, ColumnValues values);

    int update(String table, ColumnValues values, String whereClause, Object... whereArgs);

    int delete(String table, String whereClause, Object... whereArgs);

    void beginTransaction();

    void setTransactionSuccessful();

    void endTransaction();

    @Override
    void close();
}
