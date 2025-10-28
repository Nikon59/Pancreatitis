package com.pancreatitis;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

public class DiagnosisPredictor {

    public static class ValueWithType {
        public final String type;
        public final int value;

        public ValueWithType(String type, int value) {
            this.type = type;
            this.value = value;
        }
    }

    // Вспомогательные внутренние классы
    private static class CharacteristicPredict {
        int id;
        String name;
        String type;
        int weight;
        int minValue;
        int maxValue;
    }

    private static class TrainingRecord {
        public int diagnosisCode;
        public int[] values;
        public int anketId; // ID анкеты
        public int patientId;
    }

    private static class ClassStat {
        int count = 0;
        double totalSimilarity = 0.0;
    }

    // Основной публичный метод: принимает открытое SQLiteDatabase и карту характеристик пациента
    public Map<Integer, Float> predict(SQLiteDatabase db, Map<String, ValueWithType> patientCharacteristics) {
        // 1. Загрузить характеристики
        List<CharacteristicPredict> characteristicPredicts = loadCharacteristics(db);
        Map<String, CharacteristicPredict> charByName = new HashMap<>();
        for (CharacteristicPredict c : characteristicPredicts) charByName.put(c.name, c);

        // 2. Загрузить обучающую выборку
        List<TrainingRecord> trainingData = loadTrainingData(db, characteristicPredicts);

        // 3. Подготовить значения пациента по порядку характеристик
        int[] patientValues = new int[characteristicPredicts.size()];
        for (int i = 0; i < characteristicPredicts.size(); i++) {
            CharacteristicPredict ch = characteristicPredicts.get(i);
            ValueWithType vt = patientCharacteristics.get(ch.name);
            if (vt == null) {
                throw new IllegalArgumentException("Missing characteristic: " + ch.name);
            }
            patientValues[i] = vt.value;
        }

        // 4. Инициализация классов (1,5,6)
        ClassStat[] classes = new ClassStat[3];
        for (int i = 0; i < 3; i++) classes[i] = new ClassStat();

        // 5. Сравнение с обучающей выборкой
        for (TrainingRecord record : trainingData) {
            double similarity = 0.0;
            for (int i = 0; i < characteristicPredicts.size(); i++) {
                CharacteristicPredict ch = characteristicPredicts.get(i);
                int trainVal = record.values[i];
                int patVal = patientValues[i];

                double contribution;
                if ("кат.".equals(ch.type)) {
                    contribution = (trainVal == patVal) ? ch.weight : 0.0;
                } else {
                    double diff = Math.abs(trainVal - patVal);
                    double range = ch.maxValue - ch.minValue;
                    if (range <= 0) contribution = ch.weight;
                    else {
                        contribution = ch.weight * (range - diff) / range;
                        contribution = Math.max(0.0, contribution);
                    }
                }
                similarity += contribution;
            }

            int classIndex = getClassIndex(record.diagnosisCode);
            if (classIndex != -1) {
                classes[classIndex].count++;
                classes[classIndex].totalSimilarity += similarity;
            }
        }

        // 6. Нормализация в вероятности
        float total = 0f;
        float[] probs = new float[3];
        for (int i = 0; i < 3; i++) {
            probs[i] = (classes[i].count > 0) ? (float)(classes[i].totalSimilarity / classes[i].count) : 0f;
            total += probs[i];
        }

        Map<Integer, Float> result = new HashMap<>();
        if (total > 0f) {
            result.put(1, probs[0] / total);
            result.put(5, probs[1] / total);
            result.put(6, probs[2] / total);
        } else {
            result.put(1, 0f);
            result.put(5, 0f);
            result.put(6, 0f);
        }
        return result;
    }

    // Вспомогательный класс для хранения информации об анкете
    private static class AnketInfo {
        int patientId;
        int diagnosis;

        AnketInfo(int patientId, int diagnosis) {
            this.patientId = patientId;
            this.diagnosis = diagnosis;
        }
    }

    // --- Загрузка характеристик из SQLiteDatabase ---
    private List<CharacteristicPredict> loadCharacteristics(SQLiteDatabase db) {
        List<CharacteristicPredict> list = new ArrayList<>();
        String sql = "SELECT id, name, type, weight, min_value, max_value FROM characteristic";
        Cursor c = null;
        try {
            c = db.rawQuery(sql, null);
            if (c.moveToFirst()) {
                do {
                    CharacteristicPredict ch = new CharacteristicPredict();
                    ch.id = c.getInt(c.getColumnIndexOrThrow("id"));
                    ch.name = c.getString(c.getColumnIndexOrThrow("name"));
                    ch.type = c.getString(c.getColumnIndexOrThrow("type"));
                    ch.weight = c.getInt(c.getColumnIndexOrThrow("weight"));
                    ch.minValue = c.getInt(c.getColumnIndexOrThrow("min_value"));
                    ch.maxValue = c.getInt(c.getColumnIndexOrThrow("max_value"));
                    list.add(ch);
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) c.close();
        }
        return list;
    }

    // --- Загрузка обучающей выборки из SQLiteDatabase ---
    private List<TrainingRecord> loadTrainingData(SQLiteDatabase db, List<CharacteristicPredict> characteristics) {
        List<TrainingRecord> records = new ArrayList<>();
        if (characteristics.isEmpty()) return records;

        // Сопоставление id характеристики -> индекс в массиве
        Map<Integer, Integer> charIdToIndex = new HashMap<>();
        for (int i = 0; i < characteristics.size(); i++) {
            charIdToIndex.put(characteristics.get(i).id, i);
        }

        // Получаем список АНКЕТ с диагнозами 1,5,6 (каждая анкета - отдельная запись)
        String sqlAnkets = "SELECT id, id_patient, diagnosis FROM ankets WHERE diagnosis IN (1, 5, 6)";
        Map<Integer, AnketInfo> anketInfoMap = new LinkedHashMap<>(); // id_anket -> информация об анкете
        Cursor c = null;

        try {
            c = db.rawQuery(sqlAnkets, null);
            if (c.moveToFirst()) {
                do {
                    int anketId = c.getInt(c.getColumnIndexOrThrow("id"));
                    int patientId = c.getInt(c.getColumnIndexOrThrow("id_patient"));
                    int diagnosis = c.getInt(c.getColumnIndexOrThrow("diagnosis"));

                    anketInfoMap.put(anketId, new AnketInfo(patientId, diagnosis));
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) {
                c.close();
                c = null;
            }
        }

        if (anketInfoMap.isEmpty()) return records;

        // Инициализируем для каждой АНКЕТЫ массив значений
        Map<Integer, int[]> anketValuesMap = new HashMap<>();
        for (int anketId : anketInfoMap.keySet()) {
            anketValuesMap.put(anketId, new int[characteristics.size()]);
        }

        // Получаем значения характеристик для этих АНКЕТ
        String sqlValues = "SELECT id_anket, id_characteristic, value FROM characterization_anket_patient";

        try {
            c = db.rawQuery(sqlValues, null);
            if (c.moveToFirst()) {
                do {
                    int anketId = c.getInt(c.getColumnIndexOrThrow("id_anket"));
                    int charId = c.getInt(c.getColumnIndexOrThrow("id_characteristic"));
                    int value = 0;

                    // Обрабатываем значение (может быть int или string)
                    try {
                        value = c.getInt(c.getColumnIndexOrThrow("value"));
                    } catch (Exception e) {
                        String stringValue = c.getString(c.getColumnIndexOrThrow("value"));
                        try {
                            value = Integer.parseInt(stringValue);
                        } catch (Exception ex) {
                            value = 0;
                        }
                    }

                    Integer pos = charIdToIndex.get(charId);
                    if (pos != null && anketValuesMap.containsKey(anketId)) {
                        anketValuesMap.get(anketId)[pos] = value;
                    }
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) c.close();
        }

        // Сформируем TrainingRecord для каждой АНКЕТЫ
        for (Map.Entry<Integer, AnketInfo> entry : anketInfoMap.entrySet()) {
            int anketId = entry.getKey();
            AnketInfo info = entry.getValue();

            TrainingRecord rec = new TrainingRecord();
            rec.diagnosisCode = info.diagnosis;
            rec.values = anketValuesMap.get(anketId);
            rec.anketId = anketId; // сохраняем ID анкеты для отслеживания
            rec.patientId = info.patientId; // сохраняем ID пациента для информации

            records.add(rec);
        }

        return records;
    }

    private int getClassIndex(int diagnosisCode) {
        switch (diagnosisCode) {
            case 1: return 0;
            case 5: return 1;
            case 6: return 2;
            default: return -1;
        }
    }
}
