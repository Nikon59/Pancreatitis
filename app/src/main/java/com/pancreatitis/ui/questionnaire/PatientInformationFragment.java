package com.pancreatitis.ui.questionnaire;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import androidx.core.content.ContextCompat;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import com.pancreatitis.adapters.DiagnosisItemAdapter;
import com.pancreatitis.adapters.QuestionnaireInformationItemAdapter;
import com.pancreatitis.adapters.TitleAdapter;
import com.pancreatitis.adapters.UnifiedCharacteristicAdapter;
import com.pancreatitis.models.*;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.R;
import com.pancreatitis.modules.prediction.PredictionModule;
import com.pancreatitis.modules.prediction.PredictionResult;
import com.pancreatitis.modules.questionnairemanager.QuestionnaireManagerModule;
import com.pancreatitis.core.model.ml.ModelManifest;
import com.pancreatitis.core.model.ml.ModelMetadata;
import com.pancreatitis.infrastructure.clinical.ClinicalTerminology;
import com.pancreatitis.infrastructure.clinical.MedicalProtocolGenerator;
import com.google.android.material.snackbar.Snackbar;
import android.util.Log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class PatientInformationFragment extends Fragment {

    private RecyclerView recyclerView;
    private Button btnPredict, btnSave, btnDelete, btnGenerateProtocol;
    private UnifiedCharacteristicAdapter characteristicItemTitleAdapter;
    private QuestionnaireInformationItemAdapter questionnaireInformationItemAdapter;
    private DiagnosisItemAdapter diagnosisItemAdapter;
    private PredictionResult lastPredictionResult;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_questionnaire, container, false);
        initViews(view);
        setupClickListeners();
        return view;
    }

    private void initViews(View view) {
        try {
            recyclerView = view.findViewById(R.id.recycler_view_characteristics);
            btnSave = view.findViewById(R.id.btn_save);
            btnPredict = view.findViewById(R.id.btn_predict);
            btnDelete = view.findViewById(R.id.btn_delete);
            btnGenerateProtocol = view.findViewById(R.id.btn_generate_protocol);
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
        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
        if (btnGenerateProtocol != null) {
            btnGenerateProtocol.setOnClickListener(v -> handleGenerateProtocol(null));
        }
    }

    // ========== DELETE HANDLING ==========
    private void handleDelete() {
        QuestionnaireManagerModule questionnaireManagerModule = QuestionnaireManagerModule.getInstance();
        String result = questionnaireManagerModule.deleteQuestionnaire();
        if (Objects.equals(result, "Успешно")) {
            questionnaireManagerModule.setQuestionnaire(new Questionnaire());
            questionnaireManagerModule.setPatient(new Patient());
            questionnaireManagerModule.setCharacterizationAnketPatients(new ArrayList<>());
            returnFragmentAllAnkets();
        }
        showMessage(result);
    }
    private void returnFragmentAllAnkets() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.navigation_questionnaires);
    }

    // ========== PREDICTION HANDLING ==========

    private void handlePrediction() {
        if (getContext() == null) {
            return;
        }
        PredictionModule predictionModule = PredictionModule.getInstance();
        predictionModule.setContext(getContext());

        List<CharasteristicDTO> charasteristicDTOS = getAllCharasteristicDTO();
        if (charasteristicDTOS.isEmpty()) {
            showMessage(getString(R.string.error_empty_features));
            return;
        }

        btnPredict.setEnabled(false);
        predictionModule.predictAsync(charasteristicDTOS, new PredictionModule.PredictionCallback() {
            @Override
            public void onSuccess(PredictionResult result) {
                if (!isAdded()) return;
                btnPredict.setEnabled(true);
                lastPredictionResult = result;
                displayPredictionDialog(result, charasteristicDTOS.size(), loadAllCharacteristic().size());
            }

            @Override
            public void onError(Throwable error) {
                if (!isAdded()) return;
                btnPredict.setEnabled(true);
                Log.e("PatientInfoFragment", "Inference error", error);
                if (getView() != null) {
                    Snackbar.make(getView(), R.string.error_inference_failed, Snackbar.LENGTH_LONG).show();
                } else {
                    showError(getString(R.string.error_inference_failed));
                }
            }
        });
    }

    private void handleGenerateProtocol(PredictionResult explicitResult) {
        if (getContext() == null || !isAdded()) return;
        List<CharasteristicDTO> dtos = getAllCharasteristicDTO();
        QuestionnaireManagerModule qm = QuestionnaireManagerModule.getInstance();
        Questionnaire questionnaire = qm.getQuestionnaire();
        Patient patient = qm.getPatient();
        View anchorView = (getView() != null) ? getView() : recyclerView;

        if (explicitResult != null) {
            MedicalProtocolGenerator.copyProtocolToClipboardAndNotify(
                    requireContext(), anchorView, questionnaire, patient, dtos, explicitResult);
            return;
        }

        if (lastPredictionResult != null) {
            MedicalProtocolGenerator.copyProtocolToClipboardAndNotify(
                    requireContext(), anchorView, questionnaire, patient, dtos, lastPredictionResult);
            return;
        }

        if (dtos.isEmpty()) {
            MedicalProtocolGenerator.copyProtocolToClipboardAndNotify(
                    requireContext(), anchorView, questionnaire, patient, dtos, null);
            return;
        }

        PredictionModule predictionModule = PredictionModule.getInstance();
        predictionModule.setContext(getContext());
        predictionModule.predictAsync(dtos, new PredictionModule.PredictionCallback() {
            @Override
            public void onSuccess(PredictionResult result) {
                if (!isAdded()) return;
                lastPredictionResult = result;
                MedicalProtocolGenerator.copyProtocolToClipboardAndNotify(
                        requireContext(), anchorView, questionnaire, patient, dtos, result);
            }

            @Override
            public void onError(Throwable error) {
                if (!isAdded()) return;
                MedicalProtocolGenerator.copyProtocolToClipboardAndNotify(
                        requireContext(), anchorView, questionnaire, patient, dtos, null);
            }
        });
    }

    private void displayPredictionDialog(PredictionResult predictionResult, int currentFeatures, int maxFeatures) {
        if (getContext() == null || !isAdded()) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_prediction_result, null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setView(dialogView);
        androidx.appcompat.app.AlertDialog dialog = builder.create();

        final PredictionResult[] currentResult = new PredictionResult[]{predictionResult};
        populatePredictionViews(dialogView, predictionResult, currentFeatures, maxFeatures);

        Spinner spinnerModelSelector = dialogView.findViewById(R.id.spinner_model_selector);
        Button btnClose = dialogView.findViewById(R.id.btn_dialog_close);
        Button btnApply = dialogView.findViewById(R.id.btn_dialog_apply);
        Button btnDialogProtocol = dialogView.findViewById(R.id.btn_dialog_protocol);

        if (btnDialogProtocol != null) {
            btnDialogProtocol.setOnClickListener(v -> handleGenerateProtocol(currentResult[0]));
        }

        PredictionModule predictionModule = PredictionModule.getInstance();
        List<ModelMetadata> models = predictionModule.getAvailableModels();
        List<String> modelDisplayNames = new ArrayList<>();
        int selectedIndex = 0;
        String activeId = predictionModule.getActiveModelId();

        for (int i = 0; i < models.size(); i++) {
            ModelMetadata meta = models.get(i);
            modelDisplayNames.add(meta.getName());
            if (meta.getId().equalsIgnoreCase(activeId)) {
                selectedIndex = i;
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                modelDisplayNames
        );
        spinnerModelSelector.setAdapter(adapter);
        spinnerModelSelector.setSelection(selectedIndex, false);

        final int[] lastIndex = new int[]{selectedIndex};
        spinnerModelSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == lastIndex[0]) return;
                lastIndex[0] = position;
                ModelMetadata chosen = models.get(position);
                try {
                    predictionModule.selectModel(chosen.getId());
                    List<CharasteristicDTO> dtos = getAllCharasteristicDTO();
                    predictionModule.predictAsync(dtos, new PredictionModule.PredictionCallback() {
                        @Override
                        public void onSuccess(PredictionResult newResult) {
                            if (!isAdded()) return;
                            currentResult[0] = newResult;
                            lastPredictionResult = newResult;
                            populatePredictionViews(dialogView, newResult, dtos.size(), maxFeatures);
                        }

                        @Override
                        public void onError(Throwable error) {
                            Log.e("PatientInfoFragment", "Inference error during model switch", error);
                            if (getView() != null) {
                                Snackbar.make(getView(), R.string.error_inference_failed, Snackbar.LENGTH_LONG).show();
                            } else {
                                showError(getString(R.string.error_inference_failed));
                            }
                        }
                    });
                } catch (Exception e) {
                    showError(getString(R.string.error_inference_failed));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnApply.setOnClickListener(v -> {
            applyPredictedDiagnosis(currentResult[0]);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void populatePredictionViews(View dialogView, PredictionResult predictionResult, int currentFeatures, int maxFeatures) {
        MaterialCardView cardPrimary = dialogView.findViewById(R.id.card_primary_diagnosis);
        TextView tvRiskBadge = dialogView.findViewById(R.id.tv_risk_badge);
        TextView tvDiagnosisName = dialogView.findViewById(R.id.tv_predicted_diagnosis_name);
        TextView tvConfidence = dialogView.findViewById(R.id.tv_predicted_confidence);
        LinearLayout layoutProbabilities = dialogView.findViewById(R.id.layout_probabilities_container);
        TextView tvModelMetrics = dialogView.findViewById(R.id.tv_model_metrics);
        TextView tvFeaturesCoverage = dialogView.findViewById(R.id.tv_features_coverage);

        ModelManifest manifest = PredictionModule.getInstance().getManifest();
        String predictedName = predictionResult.getPredictedClassName();
        if ((predictedName == null || predictedName.isEmpty()) && manifest != null) {
            predictedName = manifest.getTargetClassName(predictionResult.getPredictedClass());
        }
        if (predictedName == null || predictedName.isEmpty()) {
            predictedName = "Диагноз #" + predictionResult.getPredictedClass();
        }

        tvDiagnosisName.setText(predictedName);

        Map<Integer, Float> probs = predictionResult.getProbabilities();
        float topProb = probs.getOrDefault(predictionResult.getPredictedClass(), 0f);
        tvConfidence.setText(getString(R.string.probability_confidence_format, ClinicalTerminology.formatPercentage(topProb)));

        boolean isHighRisk = predictionResult.getPredictedClass() > 0
                || predictedName.toLowerCase().contains("некроз");

        int badgeBgColor = ContextCompat.getColor(
                requireContext(),
                isHighRisk ? R.color.clinical_risk_high_container : R.color.clinical_risk_low_container
        );
        int badgeTextColor = ContextCompat.getColor(
                requireContext(),
                isHighRisk ? R.color.clinical_risk_high_on_container : R.color.clinical_risk_low_on_container
        );
        int cardStrokeColor = ContextCompat.getColor(
                requireContext(),
                isHighRisk ? R.color.clinical_risk_high : R.color.clinical_risk_low
        );

        tvRiskBadge.setText(isHighRisk ? getString(R.string.risk_high_label) : getString(R.string.risk_low_label));
        tvRiskBadge.setBackgroundColor(badgeBgColor);
        tvRiskBadge.setTextColor(badgeTextColor);
        cardPrimary.setStrokeColor(cardStrokeColor);

        layoutProbabilities.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (Map.Entry<Integer, Float> entry : probs.entrySet()) {
            int classIdx = entry.getKey();
            float prob = entry.getValue();

            String className = (manifest != null)
                    ? manifest.getTargetClassName(classIdx)
                    : ClinicalTerminology.getCanonicalClassName(classIdx, null);

            View row = inflater.inflate(R.layout.item_prediction_probability_row, layoutProbabilities, false);
            TextView tvClassName = row.findViewById(R.id.tv_class_name);
            TextView tvClassPercentage = row.findViewById(R.id.tv_class_percentage);
            LinearProgressIndicator progress = row.findViewById(R.id.progress_probability);

            tvClassName.setText(className);
            tvClassPercentage.setText(ClinicalTerminology.formatPercentage(prob));

            int progressVal = Math.min(100, Math.max(0, Math.round(prob * 100f)));
            progress.setProgress(progressVal);

            boolean isClassHighRisk = classIdx > 0 || className.toLowerCase().contains("некроз");
            int barColor = ContextCompat.getColor(
                    requireContext(),
                    isClassHighRisk ? R.color.clinical_risk_high : R.color.clinical_risk_low
            );
            progress.setIndicatorColor(barColor);

            layoutProbabilities.addView(row);
        }

        ModelMetadata activeMeta = PredictionModule.getInstance().getActiveModelMetadata();
        if (activeMeta != null && activeMeta.getMetrics() != null) {
            StringBuilder sb = new StringBuilder();
            activeMeta.getMetrics().forEach((k, v) -> {
                if (sb.length() > 0) sb.append(" | ");
                String formattedVal;
                if (v instanceof Number) {
                    formattedVal = ClinicalTerminology.formatDecimal(((Number) v).floatValue(), 3);
                } else {
                    formattedVal = String.valueOf(v);
                }
                sb.append(k).append(": ").append(formattedVal);
            });
            tvModelMetrics.setText(sb.toString());
        } else {
            tvModelMetrics.setText(getString(R.string.active_model_label) + " " + predictionResult.getModelId());
        }

        tvFeaturesCoverage.setText(getString(
                R.string.features_coverage_format,
                currentFeatures,
                maxFeatures
        ));
    }

    private void applyPredictedDiagnosis(PredictionResult predictionResult) {
        String predictedName = predictionResult.getPredictedClassName();
        if (predictedName == null || predictedName.isEmpty()) {
            ModelManifest manifest = PredictionModule.getInstance().getManifest();
            if (manifest != null) {
                predictedName = manifest.getTargetClassName(predictionResult.getPredictedClass());
            }
        }

        if (diagnosisItemAdapter != null && !diagnosisItemAdapter.getDiagnosisItems().isEmpty()) {
            DiagnosisItem item = diagnosisItemAdapter.getDiagnosisItems().get(0);
            List<String> options = item.getOptions();
            int matchedIndex = -1;
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).equalsIgnoreCase(predictedName)) {
                    matchedIndex = i;
                    break;
                }
            }
            if (matchedIndex != -1) {
                item.setValue(matchedIndex);
                diagnosisItemAdapter.notifyDataSetChanged();
            }
        }

        QuestionnaireManagerModule qm = QuestionnaireManagerModule.getInstance();
        if (qm.getQuestionnaire() != null) {
            qm.getQuestionnaire().setDiagnosis(String.valueOf(predictionResult.getPredictedClass()));
        }
        showMessage(getString(R.string.diagnosis_applied_format, predictedName));
    }

    private List<CharasteristicDTO> getAllCharasteristicDTO(){
        List<CharacterizationAnketPatient> characterizationAnketPatients = createCharacterizationAnketPatients();
        List<Characteristic> characteristics = loadAllCharacteristic();
        List<CharasteristicDTO> charasteristicDTOS = new ArrayList<>();
        int count = Math.min(characteristics.size(), characterizationAnketPatients.size());
        for (int i = 0; i < count; i++) {
            Characteristic characteristic = characteristics.get(i);
            CharacterizationAnketPatient characterizationAnketPatient = characterizationAnketPatients.get(i);
            float val;
            if (characteristic.getIdType() == 3) {
                val = characterizationAnketPatient.getValue();
                if (val >= 0f) {
                    charasteristicDTOS.add(new CharasteristicDTO(characteristic, val));
                }
            } else {
                val = characterizationAnketPatient.getIdValue();
                if (val > 0f) {
                    charasteristicDTOS.add(new CharasteristicDTO(characteristic, val));
                }
            }
        }
        return charasteristicDTOS;
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

            characterizationAnketPatients.add(characterizationAnketPatient);
        }
        return characterizationAnketPatients;
    }

    private List<Characteristic> loadAllCharacteristic(){
        DatabaseModule databaseModule = DatabaseModule.getInstance();
        return databaseModule.getAllCharacteristics();
    }

    // ========== SAVE HANDLING ==========

    private void handleSave() {
        QuestionnaireManagerModule questionnaireManagerModule = QuestionnaireManagerModule.getInstance();
        Questionnaire questionnaire = questionnaireManagerModule.getQuestionnaire();
        questionnaire.setIdDoctor(User.getInstance().getId());
        Patient patient = questionnaireManagerModule.getPatient();

        if (checkValidForSave(patient, questionnaire)) {
            String result = questionnaireManagerModule.saveQuestionnaire();
            if (Objects.equals(result, "Успешно")) {
                questionnaireManagerModule.setCharacterizationAnketPatients(new ArrayList<>());
                reloadFragmentWithNewIds();
            }
            showMessage(result);
        }
    }
    private void reloadFragmentWithNewIds() {
        NavController navController = Navigation.findNavController(requireView());
        NavOptions options = new NavOptions.Builder()
                .setPopUpTo(R.id.navigation_anket, true)
                .build();
        navController.navigate(R.id.navigation_anket, null, options);
    }
    private boolean checkValidForSave(Patient patient, Questionnaire questionnaire) {
        if (patient.getFio() == null || Objects.equals(patient.getFio().trim(), "")) {
            showMessage(getString(R.string.error_empty_patient_name));
            return false;
        }

        if (questionnaire.getAdmittedFrom() == null || Objects.equals(questionnaire.getAdmittedFrom().trim(), "")) {
            showMessage(getString(R.string.error_empty_admitted_from));
            return false;
        }

        return true;
    }

    // ========== DATA LOADING ==========

    private void loadDisplayData() {
        QuestionnaireManagerModule questionnaireManagerModule = QuestionnaireManagerModule.getInstance();

        List<QuestionnaireInformationItem> questionnaireInformationItems = loadQuestuonnaireInformations();
        List<CharacteristicItem> characteristicItems = loadCharacteristicItems();
        List<DiagnosisItem> diagnosisItem = loadDiagnosis();

        characteristicItemTitleAdapter = new UnifiedCharacteristicAdapter(characteristicItems, questionnaireManagerModule.getQuestionnaire().getId());
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
        QuestionnaireManagerModule questionnaireManagerModule = QuestionnaireManagerModule.getInstance();
        List<CharacterizationAnketPatient> characterizationAnketPatients = questionnaireManagerModule.getCharacterizationAnketPatients();

        if (questionnaireManagerModule.getQuestionnaire() != null && questionnaireManagerModule.getQuestionnaire().getId() != -1) {
            characterizationAnketPatients = databaseModule.getCharacterizationsForAnket(questionnaireManagerModule.getQuestionnaire().getId());
        }

        String clinicalName = ClinicalTerminology.formatClinicalFeatureName(characteristic.getName(), characteristic.getOpis());
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
                    characteristicItem.setName(clinicalName);
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
                    characteristicItem.setName(clinicalName);
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
                characteristicItem.setName(clinicalName);
                characteristicItem.setHint(characteristic.getHints());
                characteristicItem.setOptions(options);

                characteristicItems = characteristicItem;
            } else if (characteristic.getIdType() == 3) {
                CharacteristicItemNumeric characteristicItem = new CharacteristicItemNumeric();
                characteristicItem.setIdCharacteristic(characteristic.getId());
                characteristicItem.setIdType(characteristic.getIdType());
                characteristicItem.setName(clinicalName);
                characteristicItem.setHint(characteristic.getHints());

                characteristicItems = characteristicItem;
            }
        }
        return characteristicItems;
    }
    private List<DiagnosisItem> loadDiagnosis(){
        List<DiagnosisItem> diagnosisItems = new ArrayList<>();
        DiagnosisItem item = new DiagnosisItem();
        ModelManifest manifest = PredictionModule.getInstance().getManifest();
        if (manifest != null && manifest.getTargetClasses() != null && !manifest.getTargetClasses().isEmpty()) {
            item.setOptions(new ArrayList<>(manifest.getTargetClasses().values()));
        }
        diagnosisItems.add(item);
        return diagnosisItems;
    }
    private void setupRecyclerView() {
        TitleAdapter title1 = new TitleAdapter(getString(R.string.title_patient_information));
        TitleAdapter title2 = new TitleAdapter(getString(R.string.title_clinical_characteristics));
        TitleAdapter title3 = new TitleAdapter(getString(R.string.title_diagnosis));

        ConcatAdapter concatAdapter =
                new ConcatAdapter(title1, questionnaireInformationItemAdapter,
                        title2, characteristicItemTitleAdapter, title3, diagnosisItemAdapter);

        recyclerView.setAdapter(concatAdapter);
    }

    private void loadDataQuestionnaire(){
        loadQuestionnaireInformation();
        loadQuestionnaireDiagnosis();
        loadPatient();
    }
    private void loadQuestionnaireDiagnosis(){
        QuestionnaireManagerModule questionnaireManagerModule = QuestionnaireManagerModule.getInstance();
        List<DiagnosisItem> diagnosisItems = diagnosisItemAdapter.getDiagnosisItems();

        DiagnosisItem diagnosisItem = diagnosisItems.get(0);
        Questionnaire questionnaire = questionnaireManagerModule.getQuestionnaire();
        if (questionnaire.getDiagnosis() != null) {
            diagnosisItem.setValue(getIndexCodeDiagnosis(questionnaire.getTextDiagnosis()));
        } else {
            diagnosisItem.setValue(getIndexCodeDiagnosis(""));
        }
    }
    private void loadQuestionnaireInformation(){
        QuestionnaireManagerModule questionnaireManagerModule = QuestionnaireManagerModule.getInstance();
        List<QuestionnaireInformationItem> questionnaireInformationItems = questionnaireInformationItemAdapter.getCharacteristics();

        QuestionnaireInformationItem admittedFrom = questionnaireInformationItems.get(1);
        QuestionnaireInformationItem lastModify = questionnaireInformationItems.get(2);
        Questionnaire questionnaire = questionnaireManagerModule.getQuestionnaire();
        if (questionnaire.getAdmittedFrom() != null) {
            admittedFrom.setValue(questionnaire.getAdmittedFrom());
        } else {
            admittedFrom.setValue("");
        }
        if (questionnaire.getDateOfCompletion() != null) {
            lastModify.setValue(questionnaire.getDateOfCompletion());
        } else {
            lastModify.setValue("");
        }
    }
    private void loadPatient(){
        QuestionnaireManagerModule questionnaireManagerModule = QuestionnaireManagerModule.getInstance();
        List<QuestionnaireInformationItem> questionnaireInformationItems = questionnaireInformationItemAdapter.getCharacteristics();

        QuestionnaireInformationItem infoPatient = questionnaireInformationItems.get(0);
        Patient patient = questionnaireManagerModule.getPatient();
        if (patient.getFio() != null) {
            infoPatient.setValue(patient.getFio());
        } else {
            infoPatient.setValue("");
        }
    }

    // ========== DIAGNOSIS MAPPING ==========

    private static Map<String, String> createDiagnosisMap() {
        Map<String, String> map = new HashMap<>();
        ModelManifest manifest = PredictionModule.getInstance().getManifest();
        if (manifest != null && manifest.getTargetClasses() != null && !manifest.getTargetClasses().isEmpty()) {
            manifest.getTargetClasses().forEach((k, v) -> map.put(String.valueOf(k), v));
        } else {
            map.put("0", "Отёчный панкреатит");
            map.put("1", "Панкрионекроз");
            map.put("5", "Панкреонекроз среднетяжелое течение");
            map.put("6", "Панкреонекроз тяжелое течение");
        }
        return map;
    }
    private static Map<String, String> createDiagnosisCodeMap() {
        Map<String, String> map = new HashMap<>();
        Map<String, String> diagMap = createDiagnosisMap();
        for (Map.Entry<String, String> entry : diagMap.entrySet()) {
            map.put(entry.getValue(), entry.getKey());
        }
        return map;
    }

    // ========== UTILITY METHODS ==========

    public int getIndexCodeDiagnosis(String diagnosis){
        if (diagnosis == null || diagnosis.isEmpty()) return 0;
        DiagnosisItem item = (diagnosisItemAdapter != null && !diagnosisItemAdapter.getDiagnosisItems().isEmpty())
                ? diagnosisItemAdapter.getDiagnosisItems().get(0)
                : null;
        if (item != null) {
            List<String> options = item.getOptions();
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).equalsIgnoreCase(diagnosis)) {
                    return i;
                }
            }
        }
        Map<String, Integer> map = new HashMap<>();
        map.put("Отечный панкреатит", 1);
        map.put("Отёчный панкреатит", 1);
        map.put("Панкрионекроз", 2);
        map.put("Панкреонекроз среднетяжелое течение", 2);
        map.put("Панкреонекроз тяжелое течение", 3);
        return map.getOrDefault(diagnosis, 0);
    }
    public int getCodeDiagnosis(String diagnosis){
        if (diagnosis == null || diagnosis.isEmpty()) return 0;
        Map<String, String> codeMap = createDiagnosisCodeMap();
        if (codeMap.containsKey(diagnosis)) {
            try {
                return Integer.parseInt(codeMap.get(diagnosis));
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }
    private void showMessage(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
    private void showError(String error) {
        Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
    }
    private void showSaveConfirmationDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.dialog_save_confirm_title)
                .setMessage(R.string.dialog_save_confirm_message)
                .setPositiveButton(R.string.action_yes, (dialog, which) -> {
                    handleSave();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.dialog_delete_confirm_title)
                .setMessage(R.string.dialog_delete_confirm_message)
                .setPositiveButton(R.string.action_yes, (dialog, which) -> {
                    showUndoCountdownDialog();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showUndoCountdownDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_undo_countdown, null);

        TextView timerText = dialogView.findViewById(R.id.timerText);
        Button undoButton = dialogView.findViewById(R.id.undoButton);

        AlertDialog countdownDialog = builder.setView(dialogView)
                .setCancelable(false)  // Запрещаем закрытие по нажатию вне диалога
                .create();

        // Таймер обратного отсчета
        CountDownTimer timer = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerText.setText("Отмена доступна: " + millisUntilFinished / 1000 + "с");
            }

            @Override
            public void onFinish() {
                countdownDialog.dismiss();
                handleDelete();  // Запускаем после 10 секунд
            }
        };

        // Кнопка отмены
        undoButton.setOnClickListener(v -> {
            timer.cancel();
            countdownDialog.dismiss();
            // Ничего не делаем - просто закрываем диалог
        });

        countdownDialog.setOnDismissListener(dialog -> {
            timer.cancel();  // Отменяем таймер, если диалог закрыт другим способом
        });

        countdownDialog.show();
        timer.start();
    }
    private void showHelpDialog(String title, String message) {
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}