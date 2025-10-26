package com.pancreatitis.ui.questionnaires;

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
import com.pancreatitis.adapters.QuestionnaireAdapter;
import com.pancreatitis.models.QuestionnaireItem;
import com.pancreatitis.R;

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
        loadQuestionnaires(); // Загружаем данные

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
        List<QuestionnaireItem> tempData = createTempData();
        questionnaireList.clear();
        questionnaireList.addAll(tempData);
        adapter.notifyDataSetChanged();

        // TODO: Замените этот метод на реальную загрузку из БД
        // loadFromDatabase();
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

    // Метод для загрузки из БД (заглушка)
    private void loadFromDatabase() {
        // TODO: Реализуйте загрузку из вашей БД
        // Пример:
        // DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        // List<Questionnaire> dbQuestionnaires = dbHelper.getAllQuestionnaires();
        // questionnaireList.clear();
        // questionnaireList.addAll(dbQuestionnaires);
        // adapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}