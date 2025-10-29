package com.pancreatitis.ui.questionnaires;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pancreatitis.DatabaseHelper;
import com.pancreatitis.adapters.QuestionnaireAdapter;
import com.pancreatitis.models.QuestionnaireItem;
import com.pancreatitis.R;

import java.util.*;

public class AllQuestionnaireFragment extends Fragment {

    private RecyclerView recyclerView;
    private QuestionnaireAdapter adapter;
    private Button btn_anket;
    private EditText searchBar;
    private List<QuestionnaireItem> questionnaireList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_questionnaires, container, false);

        initRecyclerView(view);
        loadQuestionnaires();

        return view;
    }

    private void initRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_questionnaires);
        btn_anket = view.findViewById(R.id.create_anket);
        searchBar = view.findViewById(R.id.search_bar);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Инициализируем адаптер ПЕРЕД установкой слушателей
        adapter = new QuestionnaireAdapter(questionnaireList, new QuestionnaireAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(QuestionnaireItem questionnaire) {
                // Вызываем navigateToAnketFragment при клике на анкету
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
                navController.navigate(R.id.action_questionnaires_to_anket);
            }
        });
    }

    private void loadQuestionnaires() {
        List<QuestionnaireItem> tempData = loadFromDatabase();
        questionnaireList.clear();
        questionnaireList.addAll(tempData);
        adapter.updateData(questionnaireList); // Используем метод updateData адаптера
    }

    private List<QuestionnaireItem> loadFromDatabase() {
        List<QuestionnaireItem> questionnaireItems = new ArrayList<>();
        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        String query = "select ankets.id,patients.id,patients.fio,ankets.diagnosis from ankets join patients on ankets.id_patient = patients.id order by ankets.id";
        Cursor cursor = database.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                int patientId = cursor.getInt(1);
                String fio = cursor.getString(2);
                String diagnosis = cursor.getString(3);
                questionnaireItems.add(new QuestionnaireItem(id, patientId, fio, codeToDiagnosis(diagnosis)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        database.close();
        return questionnaireItems;
    }

    private String codeToDiagnosis(String code){
        Map<String, String> diagnosisMap = new HashMap<>();
        diagnosisMap.put("1", "Отечный панкреатит");
        diagnosisMap.put("5", "Панкреонекроз среднетяжелое течение");
        diagnosisMap.put("6", "Панкреонекроз тяжелое течение");
        return diagnosisMap.get(code);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}