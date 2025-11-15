package com.pancreatitis;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
import java.io.*;

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
    }

    private static class ClassStat {
        int count = 0;
        double totalSimilarity = 0.0;
    }

    private Context context;

    // Основной публичный метод: принимает открытое SQLiteDatabase и карту характеристик пациента
    public Map<Integer, Float> predict(Context context, SQLiteDatabase db, Map<String, ValueWithType> patientCharacteristics) {
        this.context = context;

        // 1. Загрузить характеристики из БД
        List<CharacteristicPredict> characteristicPredicts = loadCharacteristics(db);
        Map<String, CharacteristicPredict> charByName = new HashMap<>();
        for (CharacteristicPredict c : characteristicPredicts) charByName.put(c.name, c);

        // 2. Загрузить обучающую выборку из файла
        List<TrainingRecord> trainingData = loadTrainingDataFromFile(characteristicPredicts);

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

    // --- Загрузка характеристик из SQLiteDatabase ---
    private List<CharacteristicPredict> loadCharacteristics(SQLiteDatabase db) {
        List<CharacteristicPredict> list = new ArrayList<>();
        String sql = "SELECT id, opis, type, weight, min_value, max_value FROM characteristic ORDER BY id";
        Cursor c = null;
        try {
            c = db.rawQuery(sql, null);
            if (c.moveToFirst()) {
                do {
                    CharacteristicPredict ch = new CharacteristicPredict();
                    ch.id = c.getInt(c.getColumnIndexOrThrow("id"));
                    ch.name = c.getString(c.getColumnIndexOrThrow("opis"));
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

    // --- Загрузка обучающей выборки из файла ---
    private List<TrainingRecord> loadTrainingDataFromFile(List<CharacteristicPredict> characteristics) {
        List<TrainingRecord> records = new ArrayList<>();

        try {
            // Чтение файла из assets
            InputStream inputStream = context.getAssets().open("algorithm.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(" ");
                // Проверяем, что количество характеристик совпадает + 1 для диагноза
                if (parts.length != characteristics.size() + 1) {
                    continue; // Пропускаем строки с неправильным количеством данных
                }

                TrainingRecord record = new TrainingRecord();
                record.values = new int[characteristics.size()];

                // Читаем значения характеристик в правильном порядке (по id характеристики)
                for (int i = 0; i < characteristics.size(); i++) {
                    try {
                        record.values[i] = Integer.parseInt(parts[i]);
                    } catch (NumberFormatException e) {
                        record.values[i] = 0; // значение по умолчанию при ошибке
                    }
                }

                // Последнее число - код диагноза
                try {
                    record.diagnosisCode = Integer.parseInt(parts[parts.length - 1]);
                } catch (NumberFormatException e) {
                    continue; // пропускаем записи с некорректным диагнозом
                }

                // Добавляем только записи с допустимыми диагнозами (1,5,6)
                if (record.diagnosisCode == 1 || record.diagnosisCode == 5 || record.diagnosisCode == 6) {
                    records.add(record);
                }
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
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