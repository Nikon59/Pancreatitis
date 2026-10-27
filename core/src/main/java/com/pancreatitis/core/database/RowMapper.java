package com.pancreatitis.core.database;

@FunctionalInterface
public interface RowMapper<T> {

    T mapRow(RowAccessor row);
}
