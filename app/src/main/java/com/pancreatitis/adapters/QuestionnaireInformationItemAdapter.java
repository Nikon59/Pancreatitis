package com.pancreatitis.adapters;

import android.app.AlertDialog;
import android.content.Context;
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
import com.pancreatitis.models.CharacteristicItem;
import com.pancreatitis.models.QuestionnaireInformationItem;

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
        private EditText editText;
        private Context context;

        public CharacteristicViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.name_characteristic);
            editText = itemView.findViewById(R.id.value_characteristic);
            context = itemView.getContext();
        }

        public void bind(QuestionnaireInformationItem questionnaireInformationItem) {
            try {
                textTitle.setText(questionnaireInformationItem.getName());

            } catch (Exception e) {
                Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
            }
        }

        private void showHelpDialog(String message) {
            new AlertDialog.Builder(context)
                    .setTitle("Справка")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
        }
    }
}