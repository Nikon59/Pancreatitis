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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PatientInformationFragment extends Fragment {

    private RecyclerView recyclerView;
    private Button btnPredict, btnSave;
    private CharacteristicAdapter patientInfoAdapter, clinicalInfoAdapter, diagnosisAdapter;
    private int questionnaireId = -1;
    private int questionnairePatientId = -1;

    private static final Map<String, String> DIAGNOSIS_MAP = createDiagnosisMap();
    private static final Map<String, String> DIAGNOSIS_CODE_MAP = createDiagnosisCodeMap();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_questionnaire, container, false);
        receiveArguments();
        initViews(view);
        setupClickListeners();
        return view;
    }

    private void receiveArguments() {
        Bundle args = getArguments();
        if (args != null) {
            questionnaireId = args.getInt("questionnaireId", -1);
            questionnairePatientId = args.getInt("questionnairePatientId", -1);
        }
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_characteristics);
        btnSave = view.findViewById(R.id.btn_save);
        btnPredict = view.findViewById(R.id.btn_predict);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        loadAndDisplayData();
    }

    private void setupClickListeners() {
        btnPredict.setOnClickListener(v -> handlePrediction());
        btnSave.setOnClickListener(v -> handleSave());
    }

    // ========== PREDICTION HANDLING ==========

    private void handlePrediction() {
        if (!validateClinicalDataForPrediction()) {
            return;
        }

        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        try (SQLiteDatabase database = dbHelper.getReadableDatabase()) {
            Map<String, DiagnosisPredictor.ValueWithType> patientChars = getFormDataForPrediction(clinicalInfoAdapter);
            DiagnosisPredictor predictor = new DiagnosisPredictor();
            Map<Integer, Float> probs = predictor.predict(getContext(), database, patientChars);
            displayPredictionResults(probs);
        } catch (Exception e) {
            showError("Prediction error: " + e.getMessage());
        } finally {
            dbHelper.close();
        }
    }

    private boolean validateClinicalDataForPrediction() {
        List<Characteristic> clinicalCharacteristics = clinicalInfoAdapter.getCharacteristics();

        for (Characteristic characteristic : clinicalCharacteristics) {
            String value = characteristic.getValue();

            if (value == null || value.trim().isEmpty()) {
                showError("Please fill all clinical characteristics for prediction");
                return false;
            }

            try {
                int numericValue = Integer.parseInt(value);
                if (!isValueInRange(numericValue, characteristic.getMin_value(), characteristic.getMax_value())) {
                    showError(String.format("Value for %s must be between %d and %d",
                            characteristic.getTitle(), characteristic.getMin_value(), characteristic.getMax_value()));
                    return false;
                }
            } catch (NumberFormatException e) {
                showError("Please enter valid numeric values for all clinical characteristics");
                return false;
            }
        }
        return true;
    }

    private void displayPredictionResults(Map<Integer, Float> probs) {
        float p1 = probs.getOrDefault(1, 0f);
        float p5 = probs.getOrDefault(5, 0f);
        float p6 = probs.getOrDefault(6, 0f);

        String result = String.format("Prediction results:\nEdematous: %.5f\nModerate: %.5f\nSevere: %.5f", p1, p5, p6);
        showMessage(result);
    }

    // ========== SAVE HANDLING ==========

    private void handleSave() {
        if (!validateSaveData()) {
            return;
        }

        DatabaseHelper dbHelper = null;
        SQLiteDatabase database = null;

        try {
            dbHelper = new DatabaseHelper(getContext());
            database = dbHelper.getWritableDatabase();
            database.beginTransaction();

            long patientId = saveOrUpdatePatient(database);
            long anketId = saveOrUpdateAnket(database, patientId);
            saveOrUpdateCharacteristics(database, anketId);

            Toast.makeText(getContext(), String.format("%d %d", patientId, anketId), Toast.LENGTH_LONG).show();

            database.setTransactionSuccessful();
            showMessage("Data saved successfully!");

            // Update IDs for future operations
            if (questionnaireId == -1) {
                questionnaireId = (int) anketId;
            }

        } catch (Exception e) {
            showError("Save error: " + e.getMessage());
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

    private boolean validateSaveData() {
        // Validate patient information
        List<Characteristic> patientInfo = patientInfoAdapter.getCharacteristics();
        for (Characteristic characteristic : patientInfo) {
            if (characteristic.getValue() == null || characteristic.getValue().trim().isEmpty()) {
                showError("Please fill all patient information fields");
                return false;
            }
        }

        // Validate clinical characteristics
        List<Characteristic> clinicalInfo = clinicalInfoAdapter.getCharacteristics();
        for (Characteristic characteristic : clinicalInfo) {
            String value = characteristic.getValue();
            if (value != null && !value.trim().isEmpty()) {
                try {
                    int numericValue = Integer.parseInt(value);
                    if (!isValueInRange(numericValue, characteristic.getMin_value(), characteristic.getMax_value())) {
                        showError(String.format("Value for %s must be between %d and %d",
                                characteristic.getTitle(), characteristic.getMin_value(), characteristic.getMax_value()));
                        return false;
                    }
                } catch (NumberFormatException e) {
                    showError("Please enter valid numeric values for clinical characteristics");
                    return false;
                }
            }
        }

        return true;
    }

    private long saveOrUpdatePatient(SQLiteDatabase database) {
        Map<String, Characteristic> patientInfo = getFormData(patientInfoAdapter);
        String patientName = patientInfo.get("Full Name").getValue();

        if (questionnairePatientId != -1) {
            return questionnairePatientId; // Use existing patient ID
        }

        // Check if patient exists
        long patientId = getPatientIdByName(database, patientName);
        if (patientId != -1) {
            return patientId;
        }

        // Create new patient
        return createNewPatient(database, patientName);
    }

    private long saveOrUpdateAnket(SQLiteDatabase database, long patientId) {
        Map<String, Characteristic> patientInfo = getFormData(patientInfoAdapter);
        Map<String, Characteristic> diagnosisInfo = getFormData(diagnosisAdapter);

        ContentValues anketValues = createAnketValues(patientInfo, diagnosisInfo, patientId);

        if (questionnaireId != -1) {
            // Update existing anket
            database.update("ankets", anketValues, "id = ?", new String[]{String.valueOf(questionnaireId)});
            return questionnaireId;
        } else {
            // Create new anket
            long newAnketId = database.insert("ankets", null, anketValues);
            if (newAnketId == -1) {
                throw new SQLException("Failed to insert anket");
            }
            return newAnketId;
        }
    }

    private void saveOrUpdateCharacteristics(SQLiteDatabase database, long anketId) {
        Map<String, Characteristic> clinicalInfo = getFormData(clinicalInfoAdapter);

        if (questionnaireId != -1) {
            // Delete existing characteristics for update
            database.delete("characterization_anket_patient", "id_anket = ?",
                    new String[]{String.valueOf(anketId)});
        }

        // Insert all valid characteristics
        for (Map.Entry<String, Characteristic> entry : clinicalInfo.entrySet()) {
            Characteristic characteristic = entry.getValue();
            if (isValidCharacteristicForSave(characteristic)) {
                ContentValues values = createCharacteristicValues(characteristic, anketId);
                database.insert("characterization_anket_patient", null, values);
            }
        }
    }

    // ========== DATA LOADING ==========

    private void loadAndDisplayData() {
        List<Characteristic> patientInfo = loadPatientInfoFromDB();
        List<Characteristic> clinicalInfo = loadClinicalInfoFromDB();
        List<Characteristic> diagnosisInfo = loadDiagnosisInfoFromDB();

        patientInfoAdapter = new CharacteristicAdapter(patientInfo);
        clinicalInfoAdapter = new CharacteristicAdapter(clinicalInfo);
        diagnosisAdapter = new CharacteristicAdapter(diagnosisInfo);

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        TitleAdapter title1 = new TitleAdapter("Patient Information");
        TitleAdapter title2 = new TitleAdapter("Clinical Characteristics");
        TitleAdapter title3 = new TitleAdapter("Predict Diagnosis");

        androidx.recyclerview.widget.ConcatAdapter concatAdapter =
                new androidx.recyclerview.widget.ConcatAdapter(title1, patientInfoAdapter,
                        title2, clinicalInfoAdapter, title3, diagnosisAdapter);

        recyclerView.setAdapter(concatAdapter);
    }

    // ========== DATABASE OPERATIONS ==========

    private long getPatientIdByName(SQLiteDatabase database, String patientName) {
        try (Cursor cursor = database.rawQuery("SELECT id FROM patients WHERE fio = ?",
                new String[]{patientName})) {
            return (cursor != null && cursor.moveToFirst()) ?
                    cursor.getLong(cursor.getColumnIndexOrThrow("id")) : -1;
        }
    }

    private long createNewPatient(SQLiteDatabase database, String patientName) {
        ContentValues patientValues = new ContentValues();
        patientValues.put("fio", patientName);

        long patientId = database.insert("patients", null, patientValues);
        if (patientId == -1) {
            throw new SQLException("Failed to insert patient");
        }
        return patientId;
    }

    private ContentValues createAnketValues(Map<String, Characteristic> patientInfo,
                                            Map<String, Characteristic> diagnosisInfo,
                                            long patientId) {
        ContentValues values = new ContentValues();
        values.put("id_patient", patientId);
        values.put("id_doctor", 2); // Default doctor ID
        values.put("id_expert", 1); // Default expert ID
        values.put("diagnosis", diagnosisToCode(diagnosisInfo.get("Diagnosis").getValue()));
        values.put("date_of_completion", patientInfo.get("Date of completion").getValue());
        values.put("admitted_from", patientInfo.get("Admitted from").getValue());
        return values;
    }

    private ContentValues createCharacteristicValues(Characteristic characteristic, long anketId) {
        ContentValues values = new ContentValues();
        values.put("id_characteristic", characteristic.getId());
        values.put("id_anket", anketId);
        values.put("value", characteristic.getValue());
        return values;
    }

    // ========== DATA TRANSFORMATION ==========

    private Map<String, DiagnosisPredictor.ValueWithType> getFormDataForPrediction(CharacteristicAdapter adapter) {
        Map<String, DiagnosisPredictor.ValueWithType> formData = new HashMap<>();
        for (Characteristic characteristic : adapter.getCharacteristics()) {
            if (isValidCharacteristicForPrediction(characteristic)) {
                formData.put(characteristic.getTitle(),
                        new DiagnosisPredictor.ValueWithType(characteristic.getType(),
                                Integer.parseInt(characteristic.getValue())));
            }
        }
        return formData;
    }

    private Map<String, Characteristic> getFormData(CharacteristicAdapter adapter) {
        Map<String, Characteristic> formData = new HashMap<>();
        for (Characteristic characteristic : adapter.getCharacteristics()) {
            formData.put(characteristic.getTitle(), characteristic);
        }
        return formData;
    }

    // ========== VALIDATION METHODS ==========

    private boolean isValidCharacteristicForPrediction(Characteristic characteristic) {
        return characteristic.getValue() != null &&
                !characteristic.getValue().trim().isEmpty() &&
                isNumeric(characteristic.getValue());
    }

    private boolean isValidCharacteristicForSave(Characteristic characteristic) {
        String value = characteristic.getValue();
        return value != null && !value.trim().isEmpty() && isNumeric(value);
    }

    private boolean isValueInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    private boolean isNumeric(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ========== LOAD DATA METHODS (keep your existing implementation with minor improvements) ==========

    private List<Characteristic> loadPatientInfoFromDB() {
        // Your existing implementation with added null safety
        List<Characteristic> list = new ArrayList<>();

        if (questionnaireId != -1) {
            DatabaseHelper dbHelper = new DatabaseHelper(getContext());
            try (SQLiteDatabase database = dbHelper.getReadableDatabase();
                 Cursor cursor = database.rawQuery(
                         "SELECT patients.fio, ankets.date_of_completion, ankets.admitted_from " +
                                 "FROM ankets JOIN patients ON ankets.id_patient = patients.id " +
                                 "WHERE ankets.id = ?", new String[]{String.valueOf(questionnaireId)})) {

                if (cursor != null && cursor.moveToFirst()) {
                    list.add(createCharacteristic("Full Name", cursor.getString(0)));
                    list.add(createCharacteristic("Date of completion", cursor.getString(1)));
                    list.add(createCharacteristic("Admitted from", cursor.getString(2)));
                    return list;
                }
            } catch (Exception e) {
                // Log error
            } finally {
                dbHelper.close();
            }
        }

        // Default values
        addDefaultPatientInfo(list);
        return list;
    }

    private Characteristic createCharacteristic(String title, String value) {
        return new Characteristic(title, "", "", value, 0, 0,
                android.text.InputType.TYPE_CLASS_TEXT, -1);
    }

    private void addDefaultPatientInfo(List<Characteristic> list) {
        list.add(createCharacteristic("Full Name", ""));
        list.add(createCharacteristic("Date of completion", ""));
        list.add(createCharacteristic("Admitted from", ""));
    }

    private List<Characteristic> loadClinicalInfoFromDB() {
        // Your existing implementation
        List<Characteristic> list = new ArrayList<>();
        DatabaseHelper dbHelper = new DatabaseHelper(getContext());

        try (SQLiteDatabase database = dbHelper.getReadableDatabase()) {
            if (questionnaireId != -1) {
                // Load existing clinical data
                try (Cursor cursor = database.rawQuery(
                        "SELECT c.opis, c.hints, c.type, cap.value, c.id, c.min_value, c.max_value " +
                                "FROM ankets a JOIN characterization_anket_patient cap ON cap.id_anket = a.id " +
                                "JOIN characteristic c ON cap.id_characteristic = c.id " +
                                "WHERE a.id = ? ORDER BY c.id", new String[]{String.valueOf(questionnaireId)})) {

                    if (cursor != null && cursor.moveToFirst()) {
                        do {
                            list.add(createClinicalCharacteristicFromCursor(cursor));
                        } while (cursor.moveToNext());
                        return list;
                    }
                }
            }

            // Load base characteristics
            loadBaseCharacteristics(database, list);

        } catch (Exception e) {
            // Log error
        } finally {
            dbHelper.close();
        }

        return list;
    }

    private Characteristic createClinicalCharacteristicFromCursor(Cursor cursor) {
        return new Characteristic(
                cursor.getString(0), // name
                cursor.getString(1), // hint
                cursor.getString(2), // type
                cursor.getString(3), // value
                cursor.getInt(5),    // min_value
                cursor.getInt(6),    // max_value
                android.text.InputType.TYPE_CLASS_TEXT,
                cursor.getLong(4)    // id
        );
    }

    private void loadBaseCharacteristics(SQLiteDatabase database, List<Characteristic> list) {
        try (Cursor baseCursor = database.rawQuery(
                "SELECT id, opis, hints, type, min_value, max_value FROM characteristic ORDER BY id", null)) {

            if (baseCursor != null && baseCursor.moveToFirst()) {
                do {
                    list.add(new Characteristic(
                            baseCursor.getString(1), // name
                            baseCursor.getString(2), // hint
                            baseCursor.getString(3), // type
                            "", // empty value
                            baseCursor.getInt(4), // min_value
                            baseCursor.getInt(5), // max_value
                            android.text.InputType.TYPE_CLASS_TEXT,
                            baseCursor.getLong(0) // id
                    ));
                } while (baseCursor.moveToNext());
            }
        }
    }

    private List<Characteristic> loadDiagnosisInfoFromDB() {
        List<Characteristic> list = new ArrayList<>();

        if (questionnaireId != -1) {
            DatabaseHelper dbHelper = new DatabaseHelper(getContext());
            try (SQLiteDatabase database = dbHelper.getReadableDatabase();
                 Cursor cursor = database.rawQuery(
                         "SELECT diagnosis FROM ankets WHERE id = ?",
                         new String[]{String.valueOf(questionnaireId)})) {

                if (cursor != null && cursor.moveToFirst()) {
                    String diagnosisCode = cursor.getString(0);
                    String diagnosis = codeToDiagnosis(diagnosisCode);
                    list.add(createCharacteristic("Diagnosis", diagnosis));
                    return list;
                }
            } catch (Exception e) {
                // Log error
            } finally {
                dbHelper.close();
            }
        }

        list.add(createCharacteristic("Diagnosis", ""));
        return list;
    }

    // ========== DIAGNOSIS MAPPING ==========

    private static Map<String, String> createDiagnosisMap() {
        Map<String, String> map = new HashMap<>();
        map.put("1", "Отечный панкреатит");
        map.put("5", "Панкреонекроз среднетяжелое течение");
        map.put("6", "Панкреонекроз тяжелое течение");
        return map;
    }

    private static Map<String, String> createDiagnosisCodeMap() {
        Map<String, String> map = new HashMap<>();
        map.put("Отечный панкреатит", "1");
        map.put("Панкреонекроз среднетяжелое течение", "5");
        map.put("Панкреонекроз тяжелое течение", "6");
        return map;
    }

    private String codeToDiagnosis(String code) {
        return DIAGNOSIS_MAP.getOrDefault(code, "-");
    }

    private String diagnosisToCode(String diagnosis) {
        return DIAGNOSIS_CODE_MAP.getOrDefault(diagnosis, "-");
    }

    // ========== UTILITY METHODS ==========

    private void showMessage(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void showError(String error) {
        Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
    }
}