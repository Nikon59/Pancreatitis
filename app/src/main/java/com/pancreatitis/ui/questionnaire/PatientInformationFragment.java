package com.pancreatitis.ui.questionnaire;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pancreatitis.adapters.DiagnosisItemAdapter;
import com.pancreatitis.adapters.QuestionnaireInformationItemAdapter;
import com.pancreatitis.adapters.TitleAdapter;
import com.pancreatitis.models.*;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.adapters.CharacteristicItemAdapter;
import com.pancreatitis.R;

import java.util.*;
import java.util.stream.Collectors;

public class PatientInformationFragment extends Fragment {

    private RecyclerView recyclerView;
    private Button btnPredict, btnSave;
    private CharacteristicItemAdapter characteristicItemAdapter;
    private QuestionnaireInformationItemAdapter questionnaireInformationItemAdapter;
    private DiagnosisItemAdapter diagnosisItemAdapter;
    private int idQuestionnaire, idPatient;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_questionnaire, container, false);
        receiveArguments();
        initViews(view);
        setupClickListeners();
        return view;
    }
    private void receiveArguments() {
        Bundle args = getArguments();
        if (args != null) {
            idQuestionnaire = args.getInt("idQuestionnaire", -1);
            idPatient = args.getInt("idPatient", -1);
        }
    }
    private void initViews(View view) {
        try {
            recyclerView = view.findViewById(R.id.recycler_view_characteristics);
            btnSave = view.findViewById(R.id.btn_save);
            btnPredict = view.findViewById(R.id.btn_predict);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            loadAndDisplayData();
        } catch (Exception e) {
            Toast.makeText(getContext(), e.toString(), Toast.LENGTH_LONG).show();
        }
    }
    private void setupClickListeners() {
//        btnPredict.setOnClickListener(v -> handlePrediction());
//        btnSave.setOnClickListener(v -> handleSave());
    }

    // ========== PREDICTION HANDLING ==========

//    private void handlePrediction() {
//        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
//        try (SQLiteDatabase database = dbHelper.getReadableDatabase()) {
//            Map<String, DiagnosisPredictor.ValueWithType> patientChars = getFormDataForPrediction(clinicalInfoAdapter);
//            DiagnosisPredictor predictor = new DiagnosisPredictor();
//            Map<Integer, Float> probs = predictor.predict(getContext(), database, patientChars);
//            displayPredictionResults(probs);
//        } catch (Exception e) {
//            showError("Ошибка предсказания: " + e.getMessage());
//        } finally {
//            dbHelper.close();
//        }
//    }
//    private void displayPredictionResults(Map<Integer, Float> probs) {
//        float p1 = probs.getOrDefault(1, 0f);
//        float p5 = probs.getOrDefault(5, 0f);
//        float p6 = probs.getOrDefault(6, 0f);
//
//        String result = String.format("Отечный панкреатит: %.5f\nСреднетяжелый панкреонекроз: %.5f\nТяжелый панкреонекроз: %.5f", p1, p5, p6);
//        showHelpDialog("Предсказание", result);
//    }

    // ========== SAVE HANDLING ==========

//    private void handleSave() {
//        Modules modules = (Modules) requireActivity().getApplication();
//        DatabaseModule databaseModule = modules.getDatabaseModule();
//        databaseModule.insertQuestionnaire(questionnaire);
//
//        if (questionnaireId == -1) {
//            questionnaireId = (int) anketId;
//            questionnairePatientId = (int) patientId;
//            patientInfoAdapter.lockFieldByTitle("Full Name");
//        }
//    }
//    private boolean validateSaveData() {
//        // Validate required patient information
//        List<Characteristic> patientInfo = patientInfoAdapter.getCharacteristics();
//        for (Characteristic characteristic : patientInfo) {
//            String value = characteristic.getValue();
//            if (value == null || value.trim().isEmpty()) {
//                showError("Пожалуйста, заполните всю информацию о пациенте");
//                return false;
//            }
//        }
//        return true;
//    }
//    private long saveOrUpdatePatient(SQLiteDatabase database) {
//        Map<String, Characteristic> patientInfo = getFormData(patientInfoAdapter);
//        String patientName = patientInfo.get("Full Name").getValue();
//
//        // Check if patient exists
//        long patientId = getPatientIdByName(database, patientName);
//        if (patientId != -1) {
//            return patientId;
//        }
//
//        // Create new patient
//        return createNewPatient(database, patientName);
//    }
//    private long saveOrUpdateAnket(SQLiteDatabase database, long patientId) {
//        Map<String, Characteristic> patientInfo = getFormData(patientInfoAdapter);
//        Map<String, Characteristic> diagnosisInfo = getFormData(diagnosisAdapter);
//
//        ContentValues anketValues = createAnketValues(patientInfo, diagnosisInfo, patientId);
//
//        if (questionnaireId != -1) {
//            // Update existing anket
//            int rowsAffected = database.update("ankets", anketValues, "id = ?",
//                    new String[]{String.valueOf(questionnaireId)});
//            if (rowsAffected == 0) {
//                throw new SQLException("Failed to update anket");
//            }
//            return questionnaireId;
//        } else {
//            // Create new anket
//            long newAnketId = database.insert("ankets", null, anketValues);
//            if (newAnketId == -1) {
//                throw new SQLException("Failed to insert anket");
//            }
//            return newAnketId;
//        }
//    }
//    private void saveOrUpdateCharacteristics(SQLiteDatabase database, long anketId) {
//        Map<String, Characteristic> clinicalInfo = getFormData(clinicalInfoAdapter);
//
//        if (questionnaireId != -1) {
//            // Delete existing characteristics for update
//            database.delete("characterization_anket_patient", "id_anket = ?",
//                    new String[]{String.valueOf(anketId)});
//        }
//
//        // Insert only filled characteristics (но все характеристики отображаются)
//        for (Map.Entry<String, Characteristic> entry : clinicalInfo.entrySet()) {
//            Characteristic characteristic = entry.getValue();
//            String value = characteristic.getValue();
//
//            // Save only if value is not empty
//            if (value != null && !value.trim().isEmpty()) {
//                ContentValues values = createCharacteristicValues(characteristic, anketId);
//                database.insert("characterization_anket_patient", null, values);
//            }
//            // Пустые характеристики не сохраняются в БД, но остаются в интерфейсе
//        }
//    }

    // ========== DATA LOADING ==========

    private void loadAndDisplayData() {
        List<QuestionnaireInformationItem> questionnaireInformationItems = loadQuestuonnaireInformations();
        List<CharacteristicItem> characteristicItems = loadCharacteristicItems();
        List<DiagnosisItem> diagnosisItem = loadDiagnosis();

        characteristicItemAdapter = new CharacteristicItemAdapter(characteristicItems);
        questionnaireInformationItemAdapter = new QuestionnaireInformationItemAdapter(questionnaireInformationItems);
        diagnosisItemAdapter = new DiagnosisItemAdapter(diagnosisItem);

        setupRecyclerView();
    }
    private List<QuestionnaireInformationItem> loadQuestuonnaireInformations(){
        List<QuestionnaireInformationItem> questionnaireInformationItems = new ArrayList<>();

        // 1)
        QuestionnaireInformationItem fioPatient = new QuestionnaireInformationItem();
        fioPatient.setName("Имя пациента");

        // 2)
        QuestionnaireInformationItem admittedFrom = new QuestionnaireInformationItem();
        admittedFrom.setName("Откуда поступил");

        // 3)
        QuestionnaireInformationItem lastModify = new QuestionnaireInformationItem();
        lastModify.setName("Дата создания");

        return questionnaireInformationItems;
    }
    private List<CharacteristicItem> loadCharacteristicItems(){
        List<CharacteristicItem> characteristicItems = new ArrayList<>();
        DatabaseModule databaseModule = DatabaseModule.getInstance();

        List<Characteristic> characteristics = databaseModule.getAllCharacteristics();
        for(int i = 0; i < characteristics.size(); i++){
            Characteristic characteristic = characteristics.get(i);
            List<CharacterizationValue> characterizationValues = databaseModule.getValuesForCharacteristic(characteristic.getId());
            List<String> options = new ArrayList<>(Arrays.asList("Нет данных"));
            options.addAll(characterizationValues.stream()
                    .map(CharacterizationValue::getValue)
                    .collect(Collectors.toList()));

            CharacteristicItem characteristicItem = new CharacteristicItem();
            characteristicItem.setName(characteristic.getOpis());
            characteristicItem.setHint(characteristic.getHints());
            characteristicItem.setOptions(options);
        }

        return characteristicItems;
    }
    private List<DiagnosisItem> loadDiagnosis(){
        List<DiagnosisItem> diagnosisItems = new ArrayList<>();
        diagnosisItems.add(new DiagnosisItem());
        return diagnosisItems;
    }
    private void setupRecyclerView() {
        TitleAdapter title1 = new TitleAdapter("Информация о пациенте");
        TitleAdapter title2 = new TitleAdapter("Клинические характеристики");
        TitleAdapter title3 = new TitleAdapter("Диагноз");

        androidx.recyclerview.widget.ConcatAdapter concatAdapter =
                new androidx.recyclerview.widget.ConcatAdapter(title1, questionnaireInformationItemAdapter,
                        title2, characteristicItemAdapter, title3, diagnosisItemAdapter);

        recyclerView.setAdapter(concatAdapter);
    }

//    // ========== DATABASE OPERATIONS ==========
//
//    private long getPatientIdByName(SQLiteDatabase database, String patientName) {
//        try (Cursor cursor = database.rawQuery("SELECT id FROM patients WHERE fio = ?",
//                new String[]{patientName})) {
//            return (cursor != null && cursor.moveToFirst()) ?
//                    cursor.getLong(cursor.getColumnIndexOrThrow("id")) : -1;
//        }
//    }
//    private long createNewPatient(SQLiteDatabase database, String patientName) {
//        ContentValues patientValues = new ContentValues();
//        patientValues.put("fio", patientName);
//
//        long patientId = database.insert("patients", null, patientValues);
//        if (patientId == -1) {
//            throw new SQLException("Failed to insert patient");
//        }
//        return patientId;
//    }
//    private ContentValues createAnketValues(Map<String, Characteristic> patientInfo,
//                                            Map<String, Characteristic> diagnosisInfo,
//                                            long patientId) {
//        ContentValues values = new ContentValues();
//        values.put("id_patient", patientId);
//        values.put("id_doctor", 3); // Default doctor ID
//        values.put("id_expert", -1); // Default expert ID
//
//        // Получаем выбранный диагноз (может быть пустой строкой)
//        String selectedDiagnosis = diagnosisInfo.get("Diagnosis").getValue();
//        // Если диагноз не выбран (пустая строка), сохраняем как NULL или пустую строку
//        if (selectedDiagnosis != null && !selectedDiagnosis.trim().isEmpty()) {
//            values.put("diagnosis", diagnosisToCode(selectedDiagnosis));
//        } else {
//            values.put("diagnosis", ""); // или можно использовать null
//        }
//
//        values.put("date_of_completion", patientInfo.get("Date of completion").getValue());
//        values.put("admitted_from", patientInfo.get("Admitted from").getValue());
//        return values;
//    }
//    private ContentValues createCharacteristicValues(Characteristic characteristic, long anketId) {
//        ContentValues values = new ContentValues();
//        values.put("id_characteristic", characteristic.getId());
//        values.put("id_anket", anketId);
//        values.put("value", characteristic.getValue());
//        return values;
//    }
//
//    // ========== PATIENT INFO LOADING ==========
//
//    private List<Characteristic> loadPatientInfoFromDB() {
//        List<Characteristic> list = new ArrayList<>();
//
//        if (questionnaireId != -1) {
//            DatabaseHelper dbHelper = new DatabaseHelper(getContext());
//            try (SQLiteDatabase database = dbHelper.getReadableDatabase();
//                 Cursor cursor = database.rawQuery(
//                         "SELECT patients.fio, ankets.date_of_completion, ankets.admitted_from " +
//                                 "FROM ankets JOIN patients ON ankets.id_patient = patients.id " +
//                                 "WHERE ankets.id = ?", new String[]{String.valueOf(questionnaireId)})) {
//
//                if (cursor != null && cursor.moveToFirst()) {
//                    // Загружаем данные из БД
//                    list.add(createCharacteristic("Full Name", cursor.getString(0)));
//                    list.add(createCharacteristic("Date of completion", cursor.getString(1)));
//                    list.add(createCharacteristic("Admitted from", cursor.getString(2)));
//                    return list;
//                }
//            } catch (Exception e) {
//                // В случае ошибки создаем поля с пустыми значениями
//            } finally {
//                dbHelper.close();
//            }
//        }
//
//        // Создаем все обязательные поля (пустые или с данными)
//        createAllRequiredPatientFields(list);
//        return list;
//    }
//    private void createAllRequiredPatientFields(List<Characteristic> list) {
//        for (String fieldName : REQUIRED_PATIENT_FIELDS) {
//            list.add(createCharacteristic(fieldName, ""));
//        }
//    }
//    private Characteristic createCharacteristic(String title, String value) {
//        return new Characteristic(title, "", "", value, 0, 0,
//                android.text.InputType.TYPE_CLASS_TEXT, -1);
//    }
//
//    // ========== CLINICAL INFO LOADING ==========
//
//    private List<Characteristic> loadClinicalInfoFromDB() {
//        List<Characteristic> list = new ArrayList<>();
//        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
//
//        try (SQLiteDatabase database = dbHelper.getReadableDatabase()) {
//            // ВСЕГДА загружаем ВСЕ характеристики из базы
//            loadAllBaseCharacteristics(database, list);
//
//            if (questionnaireId != -1) {
//                // Если редактируем существующую анкету - заполняем значениями
//                fillExistingClinicalValues(database, list);
//            }
//
//        } catch (Exception e) {
//            //Toast.makeText(getContext(), e.toString(), Toast.LENGTH_SHORT).show();
//        } finally {
//            dbHelper.close();
//        }
//
//        return list;
//    }
//    private void loadAllBaseCharacteristics(SQLiteDatabase database, List<Characteristic> list) {
//        try (Cursor baseCursor = database.rawQuery(
//                "SELECT id, opis, hints, type, min_value, max_value FROM characteristic ORDER BY id", null)) {
//
//            if (baseCursor != null && baseCursor.moveToFirst()) {
//                do {
//                    // Создаем характеристику с пустым значением по умолчанию
//                    list.add(new Characteristic(
//                            baseCursor.getString(1), // name
//                            baseCursor.getString(2), // hint
//                            baseCursor.getString(3), // type
//                            "", // empty value by default - ВСЕ характеристики создаются
//                            baseCursor.getInt(4), // min_value
//                            baseCursor.getInt(5), // max_value
//                            android.text.InputType.TYPE_CLASS_NUMBER, // для числовых полей
//                            baseCursor.getLong(0) // id
//                    ));
//                } while (baseCursor.moveToNext());
//            }
//        }
//    }
//    private void fillExistingClinicalValues(SQLiteDatabase database, List<Characteristic> characteristics) {
//        // Заполняем значениями из существующей анкеты
//        try (Cursor cursor = database.rawQuery(
//                "SELECT c.id, cap.value " +
//                        "FROM characterization_anket_patient cap " +
//                        "JOIN characteristic c ON cap.id_characteristic = c.id " +
//                        "WHERE cap.id_anket = ?", new String[]{String.valueOf(questionnaireId)})) {
//
//            if (cursor != null && cursor.moveToFirst()) {
//                do {
//                    long charId = cursor.getLong(0);
//                    String value = cursor.getString(1);
//
//                    // Находим характеристику по ID и устанавливаем значение
//                    for (Characteristic characteristic : characteristics) {
//                        if (characteristic.getId() == charId) {
//                            characteristic.setValue(value);
//                            break;
//                        }
//                    }
//                } while (cursor.moveToNext());
//            }
//        }
//    }
//
//    // ========== DIAGNOSIS INFO LOADING ==========
//
//    private List<Characteristic> loadDiagnosisInfoFromDB() {
//        List<Characteristic> list = new ArrayList<>();
//
//        if (questionnaireId != -1) {
//            DatabaseHelper dbHelper = new DatabaseHelper(getContext());
//            try (SQLiteDatabase database = dbHelper.getReadableDatabase();
//                 Cursor cursor = database.rawQuery(
//                         "SELECT diagnosis FROM ankets WHERE id = ?",
//                         new String[]{String.valueOf(questionnaireId)})) {
//
//                if (cursor != null && cursor.moveToFirst()) {
//                    String diagnosisCode = cursor.getString(0);
//                    String diagnosis = codeToDiagnosis(diagnosisCode);
//                    // Если диагноз пустой в БД, оставляем пустым
//                    list.add(createCharacteristic("Diagnosis", diagnosis));
//                    return list;
//                }
//            } catch (Exception e) {
//                // В случае ошибки создаем поле с пустым значением
//            } finally {
//                dbHelper.close();
//            }
//        }
//
//        // Создаем поле диагноза с пустым значением по умолчанию
//        list.add(createCharacteristic("Diagnosis", ""));
//        return list;
//    }
//
//    // ========== DATA TRANSFORMATION ==========
//
//    private Map<String, DiagnosisPredictor.ValueWithType> getFormDataForPrediction(CharacteristicAdapter adapter) {
//        Map<String, DiagnosisPredictor.ValueWithType> formData = new HashMap<>();
//        for (Characteristic characteristic : adapter.getCharacteristics()) {
//
//            if (characteristic.getValue() != null && !characteristic.getValue().trim().isEmpty()) {
//                formData.put(characteristic.getTitle(),
//                        new DiagnosisPredictor.ValueWithType(characteristic.getType(),
//                                Integer.parseInt(characteristic.getValue())));
//            }
//        }
//        return formData;
//    }
//    private Map<String, Characteristic> getFormData(CharacteristicAnketAdapter adapter) {
//        Map<String, Characteristic> formData = new HashMap<>();
//        for (Characteristic characteristic : adapter.getCharacteristics()) {
//            formData.put(characteristic.getTitle(), characteristic);
//        }
//        return formData;
//    }
//    private Map<String, Characteristic> getFormData(CharacteristicAdapter adapter) {
//        Map<String, Characteristic> formData = new HashMap<>();
//        for (Characteristic characteristic : adapter.getCharacteristics()) {
//            formData.put(characteristic.getTitle(), characteristic);
//        }
//        return formData;
//    }
//    private Map<String, Characteristic> getFormData(DiagnosisAdapter adapter) {
//        Map<String, Characteristic> formData = new HashMap<>();
//        for (Characteristic characteristic : adapter.getCharacteristics()) {
//            formData.put(characteristic.getTitle(), characteristic);
//        }
//        return formData;
//    }
//
//    // ========== VALIDATION METHODS ==========
//
//    private boolean isValueInRange(int value, int min, int max) {
//        return value >= min && value <= max;
//    }
//
//    // ========== DIAGNOSIS MAPPING ==========
//
//    private static Map<String, String> createDiagnosisMap() {
//        Map<String, String> map = new HashMap<>();
//        map.put("1", "Отечный панкреатит");
//        map.put("5", "Панкреонекроз среднетяжелое течение");
//        map.put("6", "Панкреонекроз тяжелое течение");
//        return map;
//    }
//    private static Map<String, String> createDiagnosisCodeMap() {
//        Map<String, String> map = new HashMap<>();
//        map.put("Отечный панкреатит", "1");
//        map.put("Панкреонекроз среднетяжелое течение", "5");
//        map.put("Панкреонекроз тяжелое течение", "6");
//        return map;
//    }
//
//    // ========== UTILITY METHODS ==========
//
//    private void showMessage(String message) {
//        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
//    }
//    private void showError(String error) {
//        Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
//    }
//    private void showHelpDialog(String title, String message) {
//        new AlertDialog.Builder(getContext())
//                .setTitle(title)
//                .setMessage(message)
//                .setPositiveButton("OK", null)
//                .show();
//    }
}