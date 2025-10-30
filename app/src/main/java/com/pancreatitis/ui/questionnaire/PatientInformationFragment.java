package com.pancreatitis.ui.questionnaire;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pancreatitis.DatabaseHelper;
import com.pancreatitis.DiagnosisPredictor;
import com.pancreatitis.adapters.CharacteristicAdapter;
import com.pancreatitis.adapters.TitleAdapter;
import com.pancreatitis.models.Characteristic;
import com.pancreatitis.R;
import com.pancreatitis.models.QuestionnaireItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PatientInformationFragment extends Fragment {

    private RecyclerView recyclerView;
    private Button btnPredict, btnSave;
    private CharacteristicAdapter chars1, chars2, chars3;
    private int questionnaireId = -1;
    private int questionnairePatientId = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_questionnaire, container, false);

        receiveArguments();
        initRecyclerView(view);

        return view;
    }

    private void receiveArguments() {
        Bundle args = getArguments();
        if (args != null) {
            questionnaireId = args.getInt("questionnaireId");
            questionnairePatientId = args.getInt("questionnairePatientId");
        }
    }

    private void initRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_characteristics);
        btnSave = view.findViewById(R.id.btn_save);
        btnPredict = view.findViewById(R.id.btn_predict);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Загружаем данные с учетом questionnaireId
        loadAndDisplayData();

        // Устанавливаем обработчики кликов
        btnPredict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseHelper dbHelper = new DatabaseHelper(getContext());
                SQLiteDatabase database = dbHelper.getReadableDatabase();

                Map<String, DiagnosisPredictor.ValueWithType> patientChars = getFormDataChar(chars2);

                DiagnosisPredictor predictor = new DiagnosisPredictor();
                try {
                    Map<Integer, Float> probs = predictor.predict(getContext(), database, patientChars);

                    float p1 = probs.get(1);
                    float p5 = probs.get(5);
                    float p6 = probs.get(6);

                    //String diagnosis = diagnosisMap.get(Integer.toString(max_key));
                    Toast.makeText(getContext(), String.format("%.5f\n%.5f\n%.5f", p1, p5, p6), Toast.LENGTH_SHORT).show();
                } catch (Exception e){
                    Toast.makeText(getContext(), e.toString(), Toast.LENGTH_SHORT).show();
                } finally {
                    database.close();
                    dbHelper.close();
                }
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, Characteristic> anketsChars = getFormData(chars1);
                Map<String, Characteristic> patientChars = getFormData(chars2);
                Map<String, Characteristic> diagnosisChars = getFormData(chars3);

                DatabaseHelper dbHelper = null;
                SQLiteDatabase database = null;

                try {
                    dbHelper = new DatabaseHelper(getContext());
                    database = dbHelper.getWritableDatabase();
                    database.beginTransaction();

                    // 1. Вставляем пациента
                    String patientName = anketsChars.get("Full Name").getValue();

                    // Сначала проверяем существование пациента
                    String query = "SELECT id FROM patients WHERE fio = ?";
                    Cursor cursor = database.rawQuery(query, new String[]{patientName});

                    long patientRowId;
                    if (cursor != null && cursor.moveToFirst()) {
                        // Пациент существует - используем существующий ID
                        patientRowId = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
                        cursor.close();
                    } else {
                        // Пациент не существует - вставляем нового
                        if (cursor != null) cursor.close();

                        ContentValues patientValues = new ContentValues();
                        patientValues.put("fio", patientName);

                        patientRowId = database.insert("patients", null, patientValues);
                        if (patientRowId == -1 && questionnairePatientId != patientRowId) {
                            throw new SQLException("Failed to insert patient");
                        }
                    }


                    // 2. Вставляем анкету
                    long anketRowId = questionnaireId;
                    if (questionnaireId != -1) {
                        int id_doctor = 2;
                        int id_expert = 1;
                        String diagnosis = diagnosisToCode(diagnosisChars.get("Diagnosis").getValue());
                        String date_of_completion = anketsChars.get("Date of completion").getValue();
                        String admitted_from = anketsChars.get("Admitted from").getValue();

                        ContentValues anketValues = new ContentValues();
                        anketValues.put("id_patient", patientRowId);
                        anketValues.put("id_doctor", id_doctor);
                        anketValues.put("id_expert", id_expert);
                        anketValues.put("diagnosis", diagnosis);
                        anketValues.put("date_of_completion", date_of_completion);
                        anketValues.put("admitted_from", admitted_from);

                        anketRowId = database.insert("ankets", null, anketValues);
                        if (anketRowId == -1) {
                            throw new SQLException("Failed to insert anket");
                        }
                    }
                    
                    // 3. Сохраняем характеристики
                    for (Map.Entry<String, Characteristic> entry : patientChars.entrySet()) {
                        ContentValues values = new ContentValues();
                        values.put("id_characteristic", entry.getValue().getId());
                        values.put("id_anket", anketRowId); // Используем реальный ID
                        values.put("value", entry.getValue().getValue());

                        database.insert("characterization_anket_patient", null, values);
                    }

                    Toast.makeText(getContext(), "Luck!", Toast.LENGTH_SHORT).show();
                    database.setTransactionSuccessful();
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Ошибка сохранения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                } finally {
                    if (database != null) {
                        database.endTransaction();
                        database.close();
                    }
                    if (dbHelper != null) {
                        dbHelper.close();
                    }
                }
            }
        });
    }
    private Map<String, DiagnosisPredictor.ValueWithType> getFormDataChar(CharacteristicAdapter chars) {
        Map<String, DiagnosisPredictor.ValueWithType> formData = new HashMap<>();

        if (chars != null) {
            for (Characteristic characteristic : chars.getCharacteristics()) {
                formData.put(characteristic.getTitle(), new DiagnosisPredictor.ValueWithType(characteristic.getType(), Integer.parseInt(characteristic.getValue())));
            }
        }

        return formData;
    }

    private Map<String, Characteristic> getFormData(CharacteristicAdapter chars) {
        Map<String, Characteristic> formData = new HashMap<>();

        if (chars != null) {
            for (Characteristic characteristic : chars.getCharacteristics()) {
                formData.put(characteristic.getTitle(), characteristic);
            }
        }

        return formData;
    }

    private void loadAndDisplayData() {
        List<Characteristic> patientInfo = loadPatientInfoFromDB();
        List<Characteristic> clinicalInfo = loadClinicalInfoFromDB();
        List<Characteristic> diagnosisInfo = loadDiagnosisInfoFromDB();

        // Сохраняем ссылки на адаптеры
        chars1 = new CharacteristicAdapter(patientInfo);
        chars2 = new CharacteristicAdapter(clinicalInfo);
        chars3 = new CharacteristicAdapter(diagnosisInfo);

        TitleAdapter title1 = new TitleAdapter("Patient Information");
        TitleAdapter title2 = new TitleAdapter("Clinical Characteristics");
        TitleAdapter title3 = new TitleAdapter("Predict Diagnosis");

        androidx.recyclerview.widget.ConcatAdapter concat =
                new androidx.recyclerview.widget.ConcatAdapter(title1, chars1, title2, chars2, title3, chars3);

        recyclerView.setAdapter(concat);
    }

    private List<Characteristic> loadPatientInfoFromDB() {
        List<Characteristic> list = new ArrayList<>();

        if (questionnaireId != -1) {
            DatabaseHelper dbHelper = new DatabaseHelper(getContext());
            SQLiteDatabase database = dbHelper.getReadableDatabase();
            Cursor cursor = null;

            try {
                String query = "SELECT patients.fio, ankets.date_of_completion, ankets.admitted_from " +
                        "FROM ankets JOIN patients ON ankets.id_patient = patients.id " +
                        "WHERE ankets.id = ?";
                cursor = database.rawQuery(query, new String[]{String.valueOf(questionnaireId)});

                // ПРОВЕРЯЕМ есть ли данные в курсоре!
                if (cursor != null && cursor.moveToFirst()) {
                    String fullName = cursor.getString(cursor.getColumnIndexOrThrow("fio"));
                    String completionDate = cursor.getString(cursor.getColumnIndexOrThrow("date_of_completion"));
                    String admittedFrom = cursor.getString(cursor.getColumnIndexOrThrow("admitted_from"));

                    list.add(new Characteristic("Full Name", "","", fullName, 0, 0, android.text.InputType.TYPE_CLASS_TEXT, -1));
                    list.add(new Characteristic("Date of completion","", "", completionDate, 0, 0, android.text.InputType.TYPE_CLASS_TEXT, -1));
                    list.add(new Characteristic("Admitted from", "","", admittedFrom, 0, 0, android.text.InputType.TYPE_CLASS_TEXT, -1));
                } else {
                    // Если данных нет - пустые поля
                    addDefaultPatientInfo(list);
                }
            } catch (Exception e) {
                addDefaultPatientInfo(list);
            } finally {
                if (cursor != null) cursor.close();
                database.close();
                dbHelper.close();
            }
        } else {
            addDefaultPatientInfo(list);
        }
        return list;
    }

    private void addDefaultPatientInfo(List<Characteristic> list) {
        list.add(new Characteristic("Full Name", "","", "...", 0,0, android.text.InputType.TYPE_CLASS_TEXT, -1));
        list.add(new Characteristic("Date of completion","", "", "...", 0, 0, android.text.InputType.TYPE_CLASS_TEXT, -1));
        list.add(new Characteristic("Admitted from", "","", "...", 0, 0, android.text.InputType.TYPE_CLASS_TEXT, -1));
    }

    private List<Characteristic> loadClinicalInfoFromDB() {
        List<Characteristic> list = new ArrayList<>();
        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            if (questionnaireId != -1) {
                String query = "SELECT characteristic.opis, characteristic.hints, characteristic.type, characterization_anket_patient.value, characteristic.id, characteristic.min_value, characteristic.max_value " +
                        "FROM ankets JOIN characterization_anket_patient ON characterization_anket_patient.id_anket = ankets.id " +
                        "JOIN characteristic ON characterization_anket_patient.id_characteristic = characteristic.id " +
                        "WHERE ankets.id = ? ORDER BY characteristic.id";
                cursor = database.rawQuery(query, new String[]{String.valueOf(questionnaireId)});

                // ПРОВЕРКА на наличие данных
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        String name = cursor.getString(0);
                        String hint = cursor.getString(1);
                        String type = cursor.getString(2);
                        String value = cursor.getString(3);
                        long id = cursor.getLong(4);
                        int min_value = cursor.getInt(5);
                        int max_value = cursor.getInt(6);
                        list.add(new Characteristic(name, hint, type, value, min_value, max_value, android.text.InputType.TYPE_CLASS_TEXT, id));
                    } while (cursor.moveToNext());
                } else {
                    // Если нет данных для этого questionnaireId, загружаем базовый список
                    loadBaseCharacteristics(database, list);
                }
            } else {
                loadBaseCharacteristics(database, list);
            }
        } catch (Exception e) {
            loadBaseCharacteristics(database, list);
        } finally {
            if (cursor != null) cursor.close();
            database.close();
            dbHelper.close();
        }
        return list;
    }

    private void loadBaseCharacteristics(SQLiteDatabase database, List<Characteristic> list) {
        Cursor baseCursor = null;
        try {
            String baseQuery = "SELECT id, opis, hints, type, min_value, max_value  FROM characteristic ORDER BY id";
            baseCursor = database.rawQuery(baseQuery, null);
            if (baseCursor != null && baseCursor.moveToFirst()) {
                do {
                    long id = baseCursor.getLong(0);
                    String name = baseCursor.getString(1);
                    String hint = baseCursor.getString(2);
                    String type = baseCursor.getString(3);
                    int min_value = baseCursor.getInt(4);
                    int max_value = baseCursor.getInt(5);
                    list.add(new Characteristic(name, hint, type, "...", min_value, max_value, android.text.InputType.TYPE_CLASS_TEXT, id));
                } while (baseCursor.moveToNext());
            }
        } finally {
            if (baseCursor != null) baseCursor.close();
        }
    }

    private List<Characteristic> loadDiagnosisInfoFromDB() {
        List<Characteristic> list = new ArrayList<>();

        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            if (questionnaireId != -1) {
                String query = "SELECT diagnosis FROM ankets WHERE id = ?";
                cursor = database.rawQuery(query, new String[]{String.valueOf(questionnaireId)});

                // ПРОВЕРКА на наличие данных
                if (cursor != null && cursor.moveToFirst()) {
                    String diagnosis = cursor.getString(cursor.getColumnIndexOrThrow("diagnosis"));
                    diagnosis = codeToDiagnosis(diagnosis);
                    list.add(new Characteristic("Diagnosis", "", "", diagnosis, 0,0, android.text.InputType.TYPE_CLASS_TEXT, -1));
                } else {
                    list.add(new Characteristic("Diagnosis","", "", "...", 0, 0, android.text.InputType.TYPE_CLASS_TEXT, -1));
                }
            } else {
                list.add(new Characteristic("Diagnosis", "","", "...", 0, 0, android.text.InputType.TYPE_CLASS_TEXT, -1));
            }
        } catch (Exception e) {
            list.add(new Characteristic("Diagnosis","", "", "...", 0, 0, android.text.InputType.TYPE_CLASS_TEXT, -1));
        } finally {
            if (cursor != null) cursor.close();
            database.close();
            dbHelper.close();
        }
        return list;
    }
    private String codeToDiagnosis(String code){
        Map<String, String> diagnosisMap = new HashMap<>();
        diagnosisMap.put("1", "Отечный панкреатит");
        diagnosisMap.put("5", "Панкреонекроз среднетяжелое течение");
        diagnosisMap.put("6", "Панкреонекроз тяжелое течение");
        return diagnosisMap.get(code);
    }
    private String diagnosisToCode(String diagnosis){
        Map<String, String> diagnosisMap = new HashMap<>();
        diagnosisMap.put("Отечный панкреатит", "1");
        diagnosisMap.put("Панкреонекроз среднетяжелое течение", "5");
        diagnosisMap.put("Панкреонекроз тяжелое течение", "6");
        return diagnosisMap.get(diagnosis);
    }
}