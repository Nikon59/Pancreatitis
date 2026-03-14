package com.pancreatitis.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pancreatitis.R;
import com.pancreatitis.models.Characteristic;
import com.pancreatitis.models.CharacteristicItem;
import com.pancreatitis.models.Patient;
import com.pancreatitis.models.Questionnaire;
import com.pancreatitis.models.QuestionnaireInformationItem;
import com.pancreatitis.modules.questionnairemanager.QuestionnaireManagerModule;

import java.util.ArrayList;
import java.util.List;

public class QuestionnaireInformationItemAdapter extends RecyclerView.Adapter<QuestionnaireInformationItemAdapter.CharacteristicViewHolder> {

    private List<QuestionnaireInformationItem> questionnaireInformationItems;

    public QuestionnaireInformationItemAdapter(List<QuestionnaireInformationItem> questionnaireInformationItems) {
        this.questionnaireInformationItems = questionnaireInformationItems;
    }

    @NonNull
    @Override
    public CharacteristicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_questionnaire_information, parent, false);
        return new CharacteristicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CharacteristicViewHolder holder, int position) {
        try {
            QuestionnaireInformationItem questionnaireInformationItem = questionnaireInformationItems.get(position);
            holder.bind(questionnaireInformationItem);
        } catch (Exception e) {
            Toast.makeText(holder.context, e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public int getItemCount() {
        return questionnaireInformationItems.size();
    }

    public void updateData(List<QuestionnaireInformationItem> questionnaireInformationItem) {
        questionnaireInformationItems.clear();
        questionnaireInformationItems.addAll(questionnaireInformationItem);
        notifyDataSetChanged();
    }

    public List<QuestionnaireInformationItem> getCharacteristics() {
        return questionnaireInformationItems;
    }

    class CharacteristicViewHolder extends RecyclerView.ViewHolder {
        private TextView textTitle;
        private EditText editValue;
        private Context context;

        public CharacteristicViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.name_characteristic);
            editValue = itemView.findViewById(R.id.value_characteristic);
            context = itemView.getContext();
        }

        public void bind(QuestionnaireInformationItem questionnaireInformationItem) {
            textTitle.setText(questionnaireInformationItem.getName());
            editValue.setText(questionnaireInformationItem.getValue());

            String title = questionnaireInformationItem.getName();
            boolean isPatientName = "Имя пациента".equals(title);
            boolean isCreationDate = "Дата создания".equals(title);

            QuestionnaireManagerModule questionnaireManagerModule = QuestionnaireManagerModule.getInstance();
            Questionnaire questionnaire = questionnaireManagerModule.getQuestionnaire();

            if ((isPatientName && !questionnaireInformationItem.getValue().isEmpty()) && questionnaire.getId() != -1 || isCreationDate) {
                editValue.setEnabled(false);
            } else {
                editValue.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
                setupTextWatcher(questionnaireInformationItem);
            }
        }

        private void setupTextWatcher(QuestionnaireInformationItem questionnaireInformationItem) {
            editValue.setTag(null);

            TextWatcher textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String title = questionnaireInformationItem.getName();
                    boolean isPatientName = "Имя пациента".equals(title);
                    boolean isAdmittedFrom = "Откуда поступил".equals(title);

                    QuestionnaireManagerModule questionnaireManagerModule = QuestionnaireManagerModule.getInstance();
                    Questionnaire questionnaire = questionnaireManagerModule.getQuestionnaire();
                    Patient patient = questionnaireManagerModule.getPatient();

                    if (isPatientName) {
                        patient.setFio(s.toString());
                    }
                    if (isAdmittedFrom) {
                        questionnaire.setAdmittedFrom(s.toString());
                    }

                    questionnaireInformationItem.setValue(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            };

            editValue.addTextChangedListener(textWatcher);
            editValue.setTag(textWatcher);
        }
    }
}