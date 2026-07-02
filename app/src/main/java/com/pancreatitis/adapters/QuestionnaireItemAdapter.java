package com.pancreatitis.adapters;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.pancreatitis.R;
import com.pancreatitis.models.Patient;
import com.pancreatitis.models.Questionnaire;
import com.pancreatitis.models.QuestionnaireItem;
import com.pancreatitis.models.User;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.questionnairemanager.QuestionnaireManagerModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class QuestionnaireItemAdapter extends RecyclerView.Adapter<QuestionnaireItemAdapter.QuestionnaireViewHolder> {

    private List<QuestionnaireItem> questionnaireList;
    private List<QuestionnaireItem> filteredList;
    private OnItemClickListener listener;

    // Текущие фильтры
    private int currentStateFilter = 0;   // 0 – все, 1 – в процессе, 2 – завершено
    private int currentDiagnosisFilter = 0; // 0 – все, 1,5,6 – коды диагнозов

    public interface OnItemClickListener {
        void onItemClick(QuestionnaireItem questionnaire);
    }

    public QuestionnaireItemAdapter(List<QuestionnaireItem> questionnaireList, OnItemClickListener listener) {
        this.questionnaireList = new ArrayList<>(questionnaireList);
        this.filteredList = new ArrayList<>(questionnaireList);
        this.listener = listener;
    }

    // Применяет все фильтры (текст, состояние, диагноз)
    public void applyFilters(String query, int stateFilter, int diagnosisFilter) {
        this.currentStateFilter = stateFilter;
        this.currentDiagnosisFilter = diagnosisFilter;

        filteredList.clear();
        if (questionnaireList.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        for (QuestionnaireItem item : questionnaireList) {
            // Поиск по имени
            boolean nameMatches = (query == null || query.trim().isEmpty())
                    || item.getFioPatient().toLowerCase().contains(query.toLowerCase().trim());

            // Фильтр по состоянию
            boolean stateMatches;
            switch (stateFilter) {
                case 1: // в процессе
                    stateMatches = item.getDiagnosis().equals("-");
                    break;
                case 2: // завершено
                    stateMatches = !item.getDiagnosis().equals("-");
                    break;
                default: // все
                    stateMatches = true;
                    break;
            }

            // Фильтр по диагнозу (текстовое представление)
            boolean diagnosisMatches;
            switch (diagnosisFilter) {
                case 1:
                    diagnosisMatches = item.getDiagnosis().equals("Отечный панкреатит");
                    break;
                case 5:
                    diagnosisMatches = item.getDiagnosis().equals("Панкреонекроз среднетяжелое течение");
                    break;
                case 6:
                    diagnosisMatches = item.getDiagnosis().equals("Панкреонекроз тяжелое течение");
                    break;
                default:
                    diagnosisMatches = true;
                    break;
            }

            if (nameMatches && stateMatches && diagnosisMatches) {
                filteredList.add(item);
            }
        }
        notifyDataSetChanged();
    }

    // Обновление данных с сохранением текущих фильтров
    public void updateData(List<QuestionnaireItem> newQuestionnaires) {
        this.questionnaireList.clear();
        this.questionnaireList.addAll(newQuestionnaires);
        // Применяем текущие фильтры (сохраняем значения)
        applyFilters(
                null, // можно передать текущий текст поиска извне, но лучше передавать отдельно
                currentStateFilter,
                currentDiagnosisFilter
        );
        // Внимание: текст поиска не хранится в адаптере, его нужно передавать отдельно
        // Для этого мы добавим метод applyFilters с query
    }

    // Удобный метод для фильтрации по тексту (используется из TextWatcher)
    public void filter(String query) {
        applyFilters(query, currentStateFilter, currentDiagnosisFilter);
    }

    public List<QuestionnaireItem> getSelectedQuestionnaires() {
        List<QuestionnaireItem> selected = new ArrayList<>();
        for (QuestionnaireItem item : questionnaireList) {
            if (item.isSelected()) {
                selected.add(item);
            }
        }
        return selected;
    }

    @NonNull
    @Override
    public QuestionnaireViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_questionnaire, parent, false);
        return new QuestionnaireViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuestionnaireViewHolder holder, int position) {
        QuestionnaireItem questionnaire = filteredList.get(position);
        holder.bind(questionnaire, listener);
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    static class QuestionnaireViewHolder extends RecyclerView.ViewHolder {
        private TextView textPatientName;
        private TextView textDiagnosis;
        private TextView textStatus;
        private CheckBox isChecked;

        public QuestionnaireViewHolder(@NonNull View itemView) {
            super(itemView);
            textPatientName = itemView.findViewById(R.id.text_fio);
            textDiagnosis = itemView.findViewById(R.id.text_diagnosis);
            textStatus = itemView.findViewById(R.id.text_status);
            isChecked = itemView.findViewById(R.id.is_checked);
        }

        public void bind(final QuestionnaireItem questionnaire, final OnItemClickListener listener) {
            if (User.getInstance().getKey() == null) {
                isChecked.setVisibility(View.GONE);
            }

            isChecked.setChecked(questionnaire.isSelected());
            isChecked.setOnClickListener(v -> questionnaire.setSelected(!questionnaire.isSelected()));

            textPatientName.setText(questionnaire.getFioPatient());
            textDiagnosis.setText(
                    questionnaire.getDiagnosis().length() > 20
                            ? questionnaire.getDiagnosis().substring(0, 20) + "..."
                            : questionnaire.getDiagnosis()
            );
            textStatus.setText(Objects.equals(questionnaire.getDiagnosis(), "-") ? "В процессе" : "Завершено");

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(questionnaire);
                }
                navigateToAnketFragment(v, questionnaire);
            });
        }

        private void navigateToAnketFragment(View view, QuestionnaireItem questionnaireItem) {
            try {
                NavController navController = Navigation.findNavController(view);

                DatabaseModule databaseModule = DatabaseModule.getInstance();
                Patient patient = databaseModule.getPatientById(questionnaireItem.getIdPatient());
                Questionnaire questionnaire = databaseModule.getQuestionnaireById(questionnaireItem.getIdQuestionnaire());

                QuestionnaireManagerModule questionnaireManagerModule = QuestionnaireManagerModule.getInstance();
                questionnaireManagerModule.setQuestionnaire(questionnaire);
                questionnaireManagerModule.setPatient(patient);

                navController.navigate(R.id.action_questionnaires_to_anket);
            } catch (Exception e) {
                Toast.makeText(itemView.getContext(), e.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }
}