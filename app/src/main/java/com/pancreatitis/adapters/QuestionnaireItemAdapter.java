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
import com.pancreatitis.models.CharacterizationAnketPatient;
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
    private List<QuestionnaireItem> filtered_questionnaireList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(QuestionnaireItem questionnaire);
    }

    public void filter(String query) {
        filtered_questionnaireList.clear();
        if (query == null || query.trim().isEmpty()) {
            filtered_questionnaireList.addAll(questionnaireList);
        } else {
            String q = query.toLowerCase();
            for (QuestionnaireItem s : questionnaireList) {
                if (s != null && s.getFioPatient().toLowerCase().contains(q)) {
                    filtered_questionnaireList.add(s);
                }
            }
        }
        notifyDataSetChanged();
    }

    public QuestionnaireItemAdapter(List<QuestionnaireItem> questionnaireList, OnItemClickListener listener) {
        this.questionnaireList = new ArrayList<>(questionnaireList); // Создаем копию
        this.filtered_questionnaireList = new ArrayList<>(questionnaireList); // Инициализируем полный список
        this.listener = listener;
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

    public void updateData(List<QuestionnaireItem> newQuestionnaires) {
        this.questionnaireList.clear();
        this.questionnaireList.addAll(newQuestionnaires);
        this.filtered_questionnaireList.clear();
        this.filtered_questionnaireList.addAll(newQuestionnaires);
        notifyDataSetChanged();
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
        QuestionnaireItem questionnaire = filtered_questionnaireList.get(position);
        holder.bind(questionnaire, listener);
    }

    @Override
    public int getItemCount() {
        return filtered_questionnaireList.size();
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
            if (User.getInstance().getKey() == null){
                isChecked.setVisibility(View.GONE);
            }

            isChecked.setChecked(questionnaire.isSelected());
            isChecked.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    questionnaire.setSelected(!questionnaire.isSelected());
                }
            });

            textPatientName.setText(questionnaire.getFioPatient());
            textDiagnosis.setText(
                   questionnaire.getDiagnosis().length() > 20
                            ? questionnaire.getDiagnosis().substring(0, 20) + "..."
                            : questionnaire.getDiagnosis()
            );
            textStatus.setText(Objects.equals(questionnaire.getDiagnosis(), "-") ? "В процессе" : "Завершено");
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onItemClick(questionnaire);
                    }
                    navigateToAnketFragment(v, questionnaire);
                }
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