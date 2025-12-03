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

import com.pancreatitis.Modules;
import com.pancreatitis.models.*;
import com.pancreatitis.modules.*;
import com.pancreatitis.adapters.QuestionnaireAdapter;
import com.pancreatitis.R;
import com.pancreatitis.modules.database.DatabaseModule;

import java.util.*;

public class AllQuestionnaireFragment extends Fragment {

    private RecyclerView recyclerView;
    private QuestionnaireAdapter adapter;
    private Button btn_anket;
    private EditText searchBar;
    private List<Questionnaire> questionnaireList = new ArrayList<>();

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

        adapter = new QuestionnaireAdapter(questionnaireList, new QuestionnaireAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Questionnaire questionnaire) {
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
        Modules modules = (Modules) requireActivity().getApplication();
        DatabaseModule databaseModule = modules.getDatabaseModule();
        List<Questionnaire> tempData = databaseModule.getAllQuestionnaires();
        questionnaireList.clear();
        questionnaireList.addAll(tempData);
        adapter.updateData(questionnaireList); // Используем метод updateData адаптера
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}