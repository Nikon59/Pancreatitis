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
import com.pancreatitis.models.QuestionnaireItem;
import java.util.List;
import java.util.Objects;

public class QuestionnaireAdapter extends RecyclerView.Adapter<QuestionnaireAdapter.QuestionnaireViewHolder> {

    private List<QuestionnaireItem> questionnaireList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(QuestionnaireItem questionnaire);
    }

    public QuestionnaireAdapter(List<QuestionnaireItem> questionnaireList, OnItemClickListener listener) {
        this.questionnaireList = questionnaireList;
        this.listener = listener;
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
        QuestionnaireItem questionnaire = questionnaireList.get(position);
        holder.bind(questionnaire, listener);
    }

    @Override
    public int getItemCount() {
        return questionnaireList.size();
    }

    public void updateData(List<QuestionnaireItem> newQuestionnaires) {
        questionnaireList.clear();
        questionnaireList.addAll(newQuestionnaires);
        notifyDataSetChanged();
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

        public void bind(final QuestionnaireItem questionnaire, final OnItemClickListener listener) {
            textPatientName.setText(questionnaire.getPatientName());
            textDiagnosis.setText(
                   questionnaire.getDiagnosis().length() > 30
                            ? questionnaire.getDiagnosis().substring(0, 30)
                            : questionnaire.getDiagnosis()
            );
            textStatus.setText(Objects.equals(questionnaire.getDiagnosis(), "-") ? "In Progress" : "Completed");

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

        private void navigateToAnketFragment(View view, QuestionnaireItem questionnaire) {
            NavController navController = Navigation.findNavController(view);

            // Создаем Bundle для передачи данных
            Bundle args = new Bundle();
            args.putInt("questionnaireId", questionnaire.getId());
            //args.putString("diagnosis", questionnaire.getDiagnosis());

            // Выполняем переход с аргументами
            navController.navigate(R.id.action_questionnaires_to_anket, args);
        }
    }
}