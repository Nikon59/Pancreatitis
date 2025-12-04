package com.pancreatitis.adapters;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import com.pancreatitis.R;
import com.pancreatitis.models.Questionnaire;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class QuestionnaireItemAdapter extends RecyclerView.Adapter<QuestionnaireItemAdapter.QuestionnaireViewHolder> {

    private List<Questionnaire> questionnaireList;
    private List<Questionnaire> filtered_questionnaireList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Questionnaire questionnaire);
    }

    public void filter(String query) {
        filtered_questionnaireList.clear();
        if (query == null || query.trim().isEmpty()) {
            filtered_questionnaireList.addAll(questionnaireList);
        } else {
            String q = query.toLowerCase();
            for (Questionnaire s : questionnaireList) {
                if (s != null && s.getPatient().getFio().toLowerCase().contains(q)) {
                    filtered_questionnaireList.add(s);
                }
            }
        }
        notifyDataSetChanged();
    }

    public QuestionnaireItemAdapter(List<Questionnaire> questionnaireList, OnItemClickListener listener) {
        this.questionnaireList = new ArrayList<>(questionnaireList); // Создаем копию
        this.filtered_questionnaireList = new ArrayList<>(questionnaireList); // Инициализируем полный список
        this.listener = listener;
    }

    public void updateData(List<Questionnaire> newQuestionnaires) {
        this.questionnaireList.clear();
        this.questionnaireList.addAll(newQuestionnaires);
        this.filtered_questionnaireList.clear();
        this.filtered_questionnaireList.addAll(newQuestionnaires); // Обновляем оба списка
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
        Questionnaire questionnaire = filtered_questionnaireList.get(position);
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

        public QuestionnaireViewHolder(@NonNull View itemView) {
            super(itemView);
            textPatientName = itemView.findViewById(R.id.text_fio);
            textDiagnosis = itemView.findViewById(R.id.text_diagnosis);
            textStatus = itemView.findViewById(R.id.text_status);
        }

        public void bind(final Questionnaire questionnaire, final OnItemClickListener listener) {
            textPatientName.setText(questionnaire.getPatient().getFio());
            textDiagnosis.setText(
                   questionnaire.getDiagnosis().length() > 30
                            ? questionnaire.getDiagnosis().substring(0, 30)
                            : questionnaire.getDiagnosis()
            );
            textStatus.setText(Objects.equals(questionnaire.getDiagnosis(), "-") ? "In Progress" : "Completed");itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onItemClick(questionnaire);
                    }
                    navigateToAnketFragment(v, questionnaire);
                }
            });
        }

        private void navigateToAnketFragment(View view, Questionnaire questionnaire) {
            NavController navController = Navigation.findNavController(view);

            Bundle args = new Bundle();
            args.putInt("questionnaireId", (int)questionnaire.getId());
            //args.putString("diagnosis", questionnaire.getDiagnosis());

            navController.navigate(R.id.action_questionnaires_to_anket, args);
        }
    }
}