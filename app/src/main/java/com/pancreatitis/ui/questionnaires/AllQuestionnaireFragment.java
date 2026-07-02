package com.pancreatitis.ui.questionnaires;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pancreatitis.MainActivity;
import com.pancreatitis.models.Patient;
import com.pancreatitis.models.Questionnaire;
import com.pancreatitis.models.QuestionnaireItem;
import com.pancreatitis.models.User;
import com.pancreatitis.adapters.QuestionnaireItemAdapter;
import com.pancreatitis.R;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.questionnairemanager.QuestionnaireManagerModule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AllQuestionnaireFragment extends Fragment {

    private RecyclerView recyclerView;
    private QuestionnaireItemAdapter adapter;
    private View loading_overlay;
    private Button btn_anket;
    private Button submit_anket;
    private EditText searchBar;
    private ImageButton filterButton;
    private List<QuestionnaireItem> questionnaireList = new ArrayList<>();
    private DatabaseModule databaseModule = DatabaseModule.getInstance();

    // Текущие значения фильтров
    private int stateFilter = 0;     // 0-все, 1-в процессе, 2-завершено
    private int diagnosisFilter = 0; // 0-все, 1,5,6

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_questionnaires, container, false);

        initViews(view);
        initRecyclerView();
        setupListeners(view);

        try {
            loadQuestionnaires();
        } catch (Exception e) {
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_questionnaires);
        btn_anket = view.findViewById(R.id.create_anket);
        submit_anket = view.findViewById(R.id.submit_ankets);
        searchBar = view.findViewById(R.id.search_bar);
        filterButton = view.findViewById(R.id.filter_button);
        loading_overlay = view.findViewById(R.id.loading_overlay);
    }

    private void initRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new QuestionnaireItemAdapter(questionnaireList, item -> { /* можно оставить пустым */ });
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners(View view) {
        // Поиск по имени
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Кнопка фильтра
        filterButton.setOnClickListener(v -> showFilterDialog());

        // Создать анкету
        btn_anket.setOnClickListener(v -> {
            QuestionnaireManagerModule questionnaireManagerModule = QuestionnaireManagerModule.getInstance();
            questionnaireManagerModule.setQuestionnaire(new Questionnaire());
            questionnaireManagerModule.setPatient(new Patient());
            questionnaireManagerModule.setCharacterizationAnketPatients(new ArrayList<>());

            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_questionnaires_to_anket);
        });

        // Отправить анкеты
        submit_anket.setOnClickListener(v -> {
            List<QuestionnaireItem> selected = adapter.getSelectedQuestionnaires();
            if (selected.isEmpty()) {
                Toast.makeText(getContext(), "Выберите хотя бы одну анкету", Toast.LENGTH_SHORT).show();
                return;
            }

            if (User.getInstance() == null) {
                Toast.makeText(getContext(), "Авторизуйтесь для отправки анкет", Toast.LENGTH_SHORT).show();
                return;
            }

            loading_overlay.setVisibility(View.VISIBLE);
            ((MainActivity) requireActivity()).lockNavigation();

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                QuestionnaireManagerModule qm = QuestionnaireManagerModule.getInstance();
                List<Questionnaire> questionnaires = getAllQuestionnaires(selected);
                List<Patient> patients = getAllPatient(selected);

                final String result = qm.submitQuestionnaire(questionnaires, patients);

                new Handler(Looper.getMainLooper()).post(() -> {
                    loading_overlay.setVisibility(View.GONE);
                    ((MainActivity) requireActivity()).unlockNavigation();
                    Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show();
                });
            });
        });
    }

    // Диалог выбора фильтров
    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_filter_questionnaires, null);
        builder.setView(dialogView);

        RadioGroup stateGroup = dialogView.findViewById(R.id.radio_group_state);
        RadioGroup diagnosisGroup = dialogView.findViewById(R.id.radio_group_diagnosis);

        // Устанавливаем текущие значения
        switch (stateFilter) {
            case 1: stateGroup.check(R.id.radio_state_in_progress); break;
            case 2: stateGroup.check(R.id.radio_state_completed); break;
            default: stateGroup.check(R.id.radio_state_all); break;
        }
        switch (diagnosisFilter) {
            case 1: diagnosisGroup.check(R.id.radio_diagnosis_1); break;
            case 5: diagnosisGroup.check(R.id.radio_diagnosis_5); break;
            case 6: diagnosisGroup.check(R.id.radio_diagnosis_6); break;
            default: diagnosisGroup.check(R.id.radio_diagnosis_all); break;
        }

        builder.setPositiveButton("Применить", (dialog, which) -> {
            // Считываем выбранные радио-кнопки
            int selectedStateId = stateGroup.getCheckedRadioButtonId();
            int selectedDiagnosisId = diagnosisGroup.getCheckedRadioButtonId();

            if (selectedStateId == R.id.radio_state_in_progress) stateFilter = 1;
            else if (selectedStateId == R.id.radio_state_completed) stateFilter = 2;
            else stateFilter = 0;

            if (selectedDiagnosisId == R.id.radio_diagnosis_1) diagnosisFilter = 1;
            else if (selectedDiagnosisId == R.id.radio_diagnosis_5) diagnosisFilter = 5;
            else if (selectedDiagnosisId == R.id.radio_diagnosis_6) diagnosisFilter = 6;
            else diagnosisFilter = 0;

            // Применяем фильтры к адаптеру
            adapter.applyFilters(searchBar.getText().toString(), stateFilter, diagnosisFilter);
        });

        builder.setNegativeButton("Сбросить", (dialog, which) -> {
            stateFilter = 0;
            diagnosisFilter = 0;
            // Обновляем диалог (можно пересоздать) – просто применяем фильтры
            adapter.applyFilters(searchBar.getText().toString(), 0, 0);
        });

        builder.setNeutralButton("Отмена", null);
        builder.show();
    }

    private List<Questionnaire> getAllQuestionnaires(List<QuestionnaireItem> selected) {
        List<Questionnaire> questionnaires = new ArrayList<>();
        for (QuestionnaireItem item : selected) {
            Questionnaire questionnaire = databaseModule.getQuestionnaireById(item.getIdQuestionnaire());
            questionnaires.add(questionnaire);
        }
        return questionnaires;
    }

    private List<Patient> getAllPatient(List<QuestionnaireItem> selected) {
        List<Patient> patients = new ArrayList<>();
        for (QuestionnaireItem item : selected) {
            Patient patient = databaseModule.getPatientById(item.getIdPatient());
            patients.add(patient);
        }
        return patients;
    }

    private void loadQuestionnaires() {
        List<QuestionnaireItem> tempData = databaseModule.getAllQuestionnaireItems(User.getInstance().getId());
        questionnaireList.clear();
        questionnaireList.addAll(tempData);
        // Обновляем адаптер и применяем текущие фильтры
        adapter.updateData(questionnaireList);
        // После обновления данных применяем фильтры с текущими значениями
        adapter.applyFilters(searchBar.getText().toString(), stateFilter, diagnosisFilter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // можно освободить ресурсы
    }
}