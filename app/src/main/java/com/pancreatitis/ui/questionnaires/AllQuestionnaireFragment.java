package com.pancreatitis.ui.questionnaires;

import android.os.Bundle;
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

import com.pancreatitis.models.*;
import com.pancreatitis.adapters.QuestionnaireItemAdapter;
import com.pancreatitis.R;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.questionnairemanager.QuestionnaireManagerModule;

import java.util.*;

public class AllQuestionnaireFragment extends Fragment {

    private RecyclerView recyclerView;
    private QuestionnaireItemAdapter adapter;
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
            Toast.makeText(getContext(), e.toString(), Toast.LENGTH_LONG).show();
        }

        return view;
    }

    private void initRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_questionnaires);
        btn_anket = view.findViewById(R.id.create_anket);
        submit_anket = view.findViewById(R.id.submit_ankets);
        searchBar = view.findViewById(R.id.search_bar);
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
            public void onClick(View v) {
                // Получаем выбранные анкеты из адаптера
                List<QuestionnaireItem> selectedQuestionnaires = adapter.getSelectedQuestionnaires();

                if (selectedQuestionnaires.isEmpty()) {
                    Toast.makeText(getContext(), "Выберите хотя бы одну анкету", Toast.LENGTH_SHORT).show();
                    return;
                }

                QuestionnaireManagerModule questionnaireManagerModule = QuestionnaireManagerModule.getInstance();
                List<Questionnaire> questionnaires = getAllQuestionnaires(selectedQuestionnaires);
                List<Patient> patients = getAllPatient(selectedQuestionnaires);
                String result = questionnaireManagerModule.submitQuestionnaire(questionnaires, patients);
                Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show();
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
        List<QuestionnaireItem> tempData = databaseModule.getAllQuestionnaireItems(2);
        questionnaireList.clear();
        questionnaireList.addAll(tempData);
        adapter.updateData(questionnaireList);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}