package com.pancreatitis.ui.questionnaires;

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
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pancreatitis.MainActivity;
import com.pancreatitis.models.*;
import com.pancreatitis.adapters.QuestionnaireItemAdapter;
import com.pancreatitis.R;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.questionnairemanager.QuestionnaireManagerModule;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AllQuestionnaireFragment extends Fragment {

    private RecyclerView recyclerView;
    private QuestionnaireItemAdapter adapter;
    private View loading_overlay;
    private Button btn_anket;
    private Button submit_anket;
    private EditText searchBar;
    private List<QuestionnaireItem> questionnaireList = new ArrayList<>();
    private DatabaseModule databaseModule = DatabaseModule.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_questionnaires, container, false);

        initRecyclerView(view);

        try {
            loadQuestionnaires();
        } catch (Exception e){
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }

        return view;
    }

    private void initRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_questionnaires);
        btn_anket = view.findViewById(R.id.create_anket);
        submit_anket = view.findViewById(R.id.submit_ankets);
        searchBar = view.findViewById(R.id.search_bar);
        loading_overlay = view.findViewById(R.id.loading_overlay);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new QuestionnaireItemAdapter(questionnaireList, new QuestionnaireItemAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(QuestionnaireItem questionnaire) {
            }
        });

        recyclerView.setAdapter(adapter);

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btn_anket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavController navController = Navigation.findNavController(v);

                Bundle args = new Bundle();

                navController.navigate(R.id.action_questionnaires_to_anket, args);
            }
        });

        submit_anket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<QuestionnaireItem> selected = adapter.getSelectedQuestionnaires();
                if (selected.isEmpty()) {
                    Toast.makeText(getContext(), "Выберите хотя бы одну анкету", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (User.getInstance().getKey() == null) {
                    Toast.makeText(getContext(), "Авторизуйтесь для отправки анкет", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Показываем загрузку и блокируем UI
                loading_overlay.setVisibility(View.VISIBLE);
                ((MainActivity) requireActivity()).lockNavigation();

                // Выполняем в фоне
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        QuestionnaireManagerModule qm = QuestionnaireManagerModule.getInstance();
                        List<Questionnaire> questionnaires = getAllQuestionnaires(selected);
                        List<Patient> patients = getAllPatient(selected);

                        final String result = qm.submitQuestionnaire(questionnaires, patients);

                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                loading_overlay.setVisibility(View.GONE);
                                ((MainActivity) requireActivity()).unlockNavigation();
                                Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });
    }

    private List<Questionnaire> getAllQuestionnaires(List<QuestionnaireItem> selectedQuestionnaires){
        List<Questionnaire> questionnaires = new ArrayList<>();
        for (QuestionnaireItem item : selectedQuestionnaires) {
            Questionnaire questionnaire = databaseModule.getQuestionnaireById((int)item.getIdQuestionnaire());
            questionnaires.add(questionnaire);
        }
        return questionnaires;
    }
    private List<Patient> getAllPatient(List<QuestionnaireItem> selectedQuestionnaires){
        List<Patient> patients = new ArrayList<>();
        for (QuestionnaireItem item : selectedQuestionnaires) {
            Patient patient = databaseModule.getPatientById(item.getIdPatient());
            patients.add(patient);
        }
        return patients;
    }

    private void loadQuestionnaires() {
        DatabaseModule databaseModule = DatabaseModule.getInstance();
        List<QuestionnaireItem> tempData = databaseModule.getAllQuestionnaireItems(1);
        questionnaireList.clear();
        questionnaireList.addAll(tempData);
        adapter.updateData(questionnaireList);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}