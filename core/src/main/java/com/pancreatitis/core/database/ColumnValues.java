package com.pancreatitis.core.database;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ColumnValues {

    private final Map<String, Object> values = new LinkedHashMap<>();

    public ColumnValues put(String key, Object value) {
        values.put(key, value);
        return this;
    }

    public ColumnValues putNull(String key) {
        values.put(key, null);
        return this;
    }

    public Object get(String key) {
        return values.get(key);
    }

    public boolean containsKey(String key) {
        return values.containsKey(key);
    }

    public Set<String> keySet() {
        return values.keySet();
    }

    public int size() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(values);
    }

    public void clear() {
        values.clear();
    }
}
