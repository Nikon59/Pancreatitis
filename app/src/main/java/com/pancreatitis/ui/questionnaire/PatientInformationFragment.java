package com.pancreatitis.ui.questionnaire;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pancreatitis.adapters.DiagnosisItemAdapter;
import com.pancreatitis.adapters.QuestionnaireInformationItemAdapter;
import com.pancreatitis.adapters.TitleAdapter;
import com.pancreatitis.adapters.UnifiedCharacteristicAdapter;
import com.pancreatitis.models.*;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.adapters.CharacteristicItemTitleAdapter;
import com.pancreatitis.R;
import com.pancreatitis.modules.prediction.PredictionModule;
import com.pancreatitis.modules.prediction.PredictionResult;
import com.pancreatitis.modules.prediction.TrainingDataException;
import com.pancreatitis.modules.questionnairemanager.QuestionnaireManagerModule;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class PatientInformationFragment extends Fragment {

    private RecyclerView recyclerView;
    private Button btnPredict, btnSave;
    private UnifiedCharacteristicAdapter characteristicItemTitleAdapter;
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
            loadDisplayData();
            loadDataQuestionnaire();
        } catch (Exception e) {
            showMessage(e.getMessage());
        }
    }
    private void setupClickListeners() {
        btnPredict.setOnClickListener(v -> handlePrediction());
        btnSave.setOnClickListener(v -> showSaveConfirmationDialog());
    }

    // ========== PREDICTION HANDLING ==========

    private void handlePrediction() {
        try {
            PredictionModule predictionModule = PredictionModule.getInstance();
            predictionModule.setContext(getContext());
            List<CharasteristicDTO> charasteristicDTOS = getAllCharasteristicDTO();
            PredictionResult predictionResult = predictionModule.predict(charasteristicDTOS);
            displayPredictionResults(predictionResult.getProbabilities(), charasteristicDTOS.size(), loadAllCharacteristic().size());
        } catch (Exception e) {
            showMessage(e.getMessage());
        }
    }

    private List<CharasteristicDTO> getAllCharasteristicDTO(){
        List<CharacterizationAnketPatient> characterizationAnketPatients = createCharacterizationAnketPatients();
        List<Characteristic> characteristics = loadAllCharacteristic();
        List<CharasteristicDTO> charasteristicDTOS = new ArrayList<>();
        for (int i = 0; i < characteristics.size(); i++) {
            CharacterizationAnketPatient characterizationAnketPatient = characterizationAnketPatients.get(i);
            CharasteristicDTO charasteristicDTO = new CharasteristicDTO(characteristics.get(i), characterizationAnketPatient.getIdValue());
            if (charasteristicDTO.getIntValue() != 0) charasteristicDTOS.add(charasteristicDTO);
        }
        return charasteristicDTOS;
    }
    private List<Characteristic> loadAllCharacteristic(){
        DatabaseModule databaseModule = DatabaseModule.getInstance();
        return databaseModule.getAllCharacteristics();
    }
    private void displayPredictionResults(Map<Integer, Float> probs, int current, int max) {
        float p1 = probs.getOrDefault(1, 0f) * 100;
        float p5 = probs.getOrDefault(5, 0f) * 100;
        float p6 = probs.getOrDefault(6, 0f) * 100;

        String result = String.format("На основе %d из %d признаков:\n\nОтечный панкреатит: %.3f%%\nСреднетяжелый панкреонекроз: %.3f%%\nТяжелый панкреонекроз: %.3f%%", current, max, p1, p5, p6);
        showHelpDialog("Прогноз", result);
    }

    // ========== SAVE HANDLING ==========

    private void handleSave() {
        QuestionnaireManagerModule questionnaireManagerModule = QuestionnaireManagerModule.getInstance();

        Patient patient = createPatient();
        Questionnaire questionnaire = createQuetionnaire();

        String result = questionnaireManagerModule.saveQuestionnaire(questionnaire, patient);
        showMessage(result);
    }
    private Patient createPatient(){
        Patient patient = new Patient();
        List<QuestionnaireInformationItem> questionnaireInformationItems = questionnaireInformationItemAdapter.getCharacteristics();

        QuestionnaireInformationItem fioPatient = questionnaireInformationItems.get(0);
        patient.setFio(fioPatient.getValue());

        patient.setId(idPatient);

        return patient;
    }
    private Questionnaire createQuetionnaire(){
        Questionnaire questionnaire = new Questionnaire();
        List<QuestionnaireInformationItem> questionnaireInformationItems = questionnaireInformationItemAdapter.getCharacteristics();
        List<DiagnosisItem> diagnosisItems = diagnosisItemAdapter.getDiagnosisItems();

        QuestionnaireInformationItem admittedFrom = questionnaireInformationItems.get(1);
        questionnaire.setAdmittedFrom(admittedFrom.getValue());

        DiagnosisItem diagnosisItem = diagnosisItems.get(0);
        questionnaire.setDiagnosis(diagnosisItem.getCodeDiagnosis());

        questionnaire.setId(idQuestionnaire);
        questionnaire.setIdPatient(idPatient);
        questionnaire.setIdDoctor(User.getInstance().getIdDoctor());

        return questionnaire;
    }

    private List<CharacterizationAnketPatient> createCharacterizationAnketPatients(){
        List<CharacterizationAnketPatient> characterizationAnketPatients = new ArrayList<>();
        List<CharacteristicItem> characteristicItems = characteristicItemTitleAdapter.getCharacteristics();
        for(int i = 0; i < characteristicItems.size(); i++){
            CharacterizationAnketPatient characterizationAnketPatient = new CharacterizationAnketPatient();
            CharacteristicItem characteristicItem = characteristicItems.get(i);

            characterizationAnketPatient.setIdCharacteristic(i + 1);
            if (characteristicItem instanceof CharacteristicItemNonNumeric) {
                characterizationAnketPatient.setIdValue(((CharacteristicItemNonNumeric) characteristicItem).getValue());
            } else if (characteristicItem instanceof CharacteristicItemNumeric) {
                characterizationAnketPatient.setValue(((CharacteristicItemNumeric) characteristicItem).getValue());
            }
            characterizationAnketPatient.setIdAnket(idQuestionnaire);

            characterizationAnketPatients.add(characterizationAnketPatient);
        }
        return characterizationAnketPatients;
    }

    // ========== DATA LOADING ==========

    private void loadDisplayData() {
        List<QuestionnaireInformationItem> questionnaireInformationItems = loadQuestuonnaireInformations();
        List<CharacteristicItem> characteristicItems = loadCharacteristicItems();
        List<DiagnosisItem> diagnosisItem = loadDiagnosis();

        characteristicItemTitleAdapter = new UnifiedCharacteristicAdapter(characteristicItems, idQuestionnaire);
        questionnaireInformationItemAdapter = new QuestionnaireInformationItemAdapter(questionnaireInformationItems);
        diagnosisItemAdapter = new DiagnosisItemAdapter(diagnosisItem);

        setupRecyclerView();
    }
    private List<QuestionnaireInformationItem> loadQuestuonnaireInformations(){
        List<QuestionnaireInformationItem> questionnaireInformationItems = new ArrayList<>();

        // 1)
        QuestionnaireInformationItem fioPatient = new QuestionnaireInformationItem();
        fioPatient.setName("Имя пациента");
        fioPatient.setValue("");

        // 2)
        QuestionnaireInformationItem admittedFrom = new QuestionnaireInformationItem();
        admittedFrom.setName("Откуда поступил");
        admittedFrom.setValue("");

        // 3)
        QuestionnaireInformationItem lastModify = new QuestionnaireInformationItem();
        lastModify.setName("Дата создания");
        lastModify.setValue("-");

        questionnaireInformationItems.add(fioPatient);
        questionnaireInformationItems.add(admittedFrom);
        questionnaireInformationItems.add(lastModify);
        return questionnaireInformationItems;
    }
    private List<CharacteristicItem> loadCharacteristicItems(){
        List<CharacteristicItem> characteristicItems = new ArrayList<>();
        DatabaseModule databaseModule = DatabaseModule.getInstance();

        List<Characteristic> characteristics = databaseModule.getAllCharacteristics();
        for(int i = 0; i < characteristics.size(); i++){
            Characteristic characteristic = characteristics.get(i);

            CharacteristicItem characteristicItem = loadValueCharacteristicItems(characteristic);
            characteristicItems.add(characteristicItem);
        }

        return characteristicItems;
    }
    private CharacteristicItem loadValueCharacteristicItems(Characteristic characteristic) {
        CharacteristicItem characteristicItems = null;
        DatabaseModule databaseModule = DatabaseModule.getInstance();
        List<CharacterizationAnketPatient> characterizationAnketPatients = databaseModule.getCharacterizationsForAnket(idQuestionnaire);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (CharacterizationAnketPatient characterizationAnketPatient : characterizationAnketPatients) {
            if (characterizationAnketPatient.getIdCharacteristic() == characteristic.getId()) {
                if (characteristic.getIdType() == 1 || characteristic.getIdType() == 2) {
                    List<CharacterizationValue> characterizationValues = databaseModule.getValuesForCharacteristic(characteristic.getId());
                    List<String> options = new ArrayList<>(Arrays.asList("Нет данных"));
                    options.addAll(characterizationValues.stream()
                            .map(CharacterizationValue::getValue)
                            .collect(Collectors.toList()));

                    CharacteristicItemNonNumeric characteristicItem = new CharacteristicItemNonNumeric();
                    characteristicItem.setIdCharacteristic(characteristic.getId());
                    characteristicItem.setIdType(characteristic.getIdType());
                    characteristicItem.setName(characteristic.getOpis());
                    characteristicItem.setHint(characteristic.getHints());
                    characteristicItem.setCreatedAt(characterizationAnketPatient.getCreatedAt());
                    characteristicItem.setValue(characterizationAnketPatient.getIdValue());
                    characteristicItem.setOptions(options);

                    if (characteristicItems == null || LocalDateTime.parse(characteristicItem.getCreatedAt(), formatter).isAfter(LocalDateTime.parse(characteristicItems.getCreatedAt(), formatter))) {
                        characteristicItems = characteristicItem;
                    }
                } else if (characteristic.getIdType() == 3) {
                    CharacteristicItemNumeric characteristicItem = new CharacteristicItemNumeric();
                    characteristicItem.setIdCharacteristic(characteristic.getId());
                    characteristicItem.setIdType(characteristic.getIdType());
                    characteristicItem.setName(characteristic.getOpis());
                    characteristicItem.setHint(characteristic.getHints());
                    characteristicItem.setCreatedAt(characterizationAnketPatient.getCreatedAt());
                    characteristicItem.setValue(characterizationAnketPatient.getValue());

                    if (characteristicItems == null || LocalDateTime.parse(characteristicItem.getCreatedAt(), formatter).isAfter(LocalDateTime.parse(characteristicItems.getCreatedAt(), formatter))) {
                        characteristicItems = characteristicItem;
                    }
                }
            }
        }
        if (characteristicItems == null){
            if (characteristic.getIdType() == 1 || characteristic.getIdType() == 2){
                List<CharacterizationValue> characterizationValues = databaseModule.getValuesForCharacteristic(characteristic.getId());
                List<String> options = new ArrayList<>(Arrays.asList("Нет данных"));
                options.addAll(characterizationValues.stream()
                        .map(CharacterizationValue::getValue)
                        .collect(Collectors.toList()));

                CharacteristicItemNonNumeric characteristicItem = new CharacteristicItemNonNumeric();
                characteristicItem.setIdCharacteristic(characteristic.getId());
                characteristicItem.setIdType(characteristic.getIdType());
                characteristicItem.setName(characteristic.getOpis());
                characteristicItem.setHint(characteristic.getHints());
                characteristicItem.setOptions(options);

                characteristicItems = characteristicItem;
            } else if (characteristic.getIdType() == 3) {
                CharacteristicItemNumeric characteristicItem = new CharacteristicItemNumeric();
                characteristicItem.setIdCharacteristic(characteristic.getId());
                characteristicItem.setIdType(characteristic.getIdType());
                characteristicItem.setName(characteristic.getOpis());
                characteristicItem.setHint(characteristic.getHints());

                characteristicItems = characteristicItem;
            }
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

        ConcatAdapter concatAdapter =
                new ConcatAdapter(title1, questionnaireInformationItemAdapter,
                        title2, characteristicItemTitleAdapter, title3, diagnosisItemAdapter);

        recyclerView.setAdapter(concatAdapter);
    }

    private void loadDataQuestionnaire(){
        if (idQuestionnaire != -1){
            //loadCharacteristicValues();
            loadQuestionnaireInformation();
            loadQuestionnaireDiagnosis();
        }
        if (idPatient != -1){
            loadPatient();
        }
    }
    private void loadCharacteristicValues(){
        DatabaseModule databaseModule = DatabaseModule.getInstance();
        List<CharacteristicItem> characteristicItems = characteristicItemTitleAdapter.getCharacteristics();

        List<CharacterizationAnketPatient> characterizationAnketPatients = databaseModule.getCharacterizationsForAnket(idQuestionnaire);
        for(int i = 0; i < characterizationAnketPatients.size(); i++){
            CharacterizationAnketPatient characterizationAnketPatient = characterizationAnketPatients.get(i);
            CharacteristicItem characteristicItem = characteristicItems.get(i);

            //characteristicItem.setValue(characterizationAnketPatient.getIdValue());
        }
    }
    private void loadQuestionnaireDiagnosis(){
        DatabaseModule databaseModule = DatabaseModule.getInstance();
        List<DiagnosisItem> diagnosisItems = diagnosisItemAdapter.getDiagnosisItems();

        DiagnosisItem diagnosisItem = diagnosisItems.get(0);
        Questionnaire questionnaire = databaseModule.getQuestionnaireById(idQuestionnaire);
        diagnosisItem.setValue(getIndexCodeDiagnosis(questionnaire.getDiagnosis()));
    }
    private void loadQuestionnaireInformation(){
        DatabaseModule databaseModule = DatabaseModule.getInstance();
        List<QuestionnaireInformationItem> questionnaireInformationItems = questionnaireInformationItemAdapter.getCharacteristics();

        QuestionnaireInformationItem admittedFrom = questionnaireInformationItems.get(1);
        QuestionnaireInformationItem lastModify = questionnaireInformationItems.get(2);
        Questionnaire questionnaire = databaseModule.getQuestionnaireById(idQuestionnaire);
        admittedFrom.setValue(questionnaire.getAdmittedFrom());
        lastModify.setValue(questionnaire.getDateOfCompletion());
    }
    private void loadPatient(){
        DatabaseModule databaseModule = DatabaseModule.getInstance();
        List<QuestionnaireInformationItem> questionnaireInformationItems = questionnaireInformationItemAdapter.getCharacteristics();

        QuestionnaireInformationItem infoPatient = questionnaireInformationItems.get(0);
        Patient patient = databaseModule.getPatientById(idPatient);
        infoPatient.setValue(patient.getFio());
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

    // ========== UTILITY METHODS ==========

    public int getIndexCodeDiagnosis(String diagnosis){
        Map<String, Integer> map = new HashMap<>();
        map.put("Отечный панкреатит", 1);
        map.put("Панкреонекроз среднетяжелое течение", 2);
        map.put("Панкреонекроз тяжелое течение", 3);
        return map.getOrDefault(diagnosis, 0);
    }
    public int getCodeDiagnosis(String diagnosis){
        Map<String, Integer> map = new HashMap<>();
        map.put("Отечный панкреатит", 1);
        map.put("Панкреонекроз среднетяжелое течение", 5);
        map.put("Панкреонекроз тяжелое течение", 6);
        return map.get(diagnosis);
    }
    private void showMessage(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
    private void showError(String error) {
        Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
    }
    private void showSaveConfirmationDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Подтверждение сохранения")
                .setMessage("Вы уверены, что хотите сохранить изменения?")
                .setPositiveButton("Да", (dialog, which) -> {
                    handleSave();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
    private void showHelpDialog(String title, String message) {
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}