package com.pancreatitis.core.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColumnValuesTest {

    @Test
    @DisplayName("Put and get values from ColumnValues")
    void testColumnValuesOperations() {
        ColumnValues values = new ColumnValues();
        assertTrue(values.isEmpty());
        assertEquals(0, values.size());

        values.put("name", "John Doe");
        values.put("age", 42);
        values.put("weight", 80.5f);
        values.putNull("notes");

        assertFalse(values.isEmpty());
        assertEquals(4, values.size());
        assertTrue(values.containsKey("name"));
        assertTrue(values.containsKey("notes"));
        assertNull(values.get("notes"));

        assertEquals("John Doe", values.get("name"));
        assertEquals(42, values.get("age"));
        assertEquals(80.5f, values.get("weight"));

        values.clear();
        assertTrue(values.isEmpty());
    }
}
