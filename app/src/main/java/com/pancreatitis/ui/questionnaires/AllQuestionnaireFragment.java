package com.pancreatitis.ui.questionnaires;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pancreatitis.DatabaseHelper;
import com.pancreatitis.adapters.QuestionnaireAdapter;
import com.pancreatitis.models.QuestionnaireItem;
import com.pancreatitis.R;

import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

public class AllQuestionnaireFragment extends Fragment {

    private RecyclerView recyclerView;
    private QuestionnaireAdapter adapter;
    private Button btn_anket;
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
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Создаем общий слушатель как в адаптере
        View.OnClickListener commonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Навигация на фрагмент анкеты без данных (для создания новой)
                NavController navController = Navigation.findNavController(v);
                navController.navigate(R.id.action_questionnaires_to_anket);
            }
        };

        btn_anket.setOnClickListener(commonClickListener);

        adapter = new QuestionnaireAdapter(questionnaireList, new QuestionnaireAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(QuestionnaireItem questionnaire) {
                // Вызываем navigateToAnketFragment при клике на анкету
            }
        });

        recyclerView.setAdapter(adapter);
    }

    private void loadQuestionnaires() {
        // Временная заглушка на 20 элементов
        // List<QuestionnaireItem> tempData = createTempData();
        List<QuestionnaireItem> tempData = loadFromDatabase();
        questionnaireList.clear();
        questionnaireList.addAll(tempData);
        adapter.notifyDataSetChanged();
    }

    private List<QuestionnaireItem> createTempData() {
        List<QuestionnaireItem> tempList = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            QuestionnaireItem questionnaire = new QuestionnaireItem(
                    i,
                    "Patient " + i,
                    "Diagnosis " + i
            );
            tempList.add(questionnaire);
        }
        return tempList;
    }

    private List<QuestionnaireItem> loadFromDatabase() {
        List<QuestionnaireItem> questionnaireItems = new ArrayList<>();
        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        String query = "select ankets.id,patients.fio,ankets.diagnosis from ankets join patients on ankets.id_patient = patients.id";
        Cursor cursor = database.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String fio = cursor.getString(1);
                String diagnosis = cursor.getString(2);
                questionnaireItems.add(new QuestionnaireItem(id, fio, diagnosis));
            } while (cursor.moveToNext());
        }
        cursor.close();
        database.close();
        return questionnaireItems;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}