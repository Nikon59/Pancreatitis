package com.pancreatitis.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pancreatitis.R;
import com.pancreatitis.models.Characteristic;
import com.pancreatitis.models.CharacteristicItem;
import com.pancreatitis.models.DiagnosisItem;

import java.util.ArrayList;
import java.util.List;

public class DiagnosisItemAdapter extends RecyclerView.Adapter<DiagnosisItemAdapter.DiagnosisViewHolder> {

    private List<DiagnosisItem> diagnosisItems;

    public DiagnosisItemAdapter(List<DiagnosisItem> diagnosisItems) {
        this.diagnosisItems = diagnosisItems;
    }

    @NonNull
    @Override
    public DiagnosisViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_diagnosis, parent, false);
        return new DiagnosisViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiagnosisViewHolder holder, int position) {
        DiagnosisItem diagnosisItem = diagnosisItems.get(position);
        holder.bind(diagnosisItem);
    }

    @Override
    public int getItemCount() {
        return diagnosisItems.size();
    }

    public List<DiagnosisItem> getDiagnosisItems(){
        return diagnosisItems;
    }
    static class DiagnosisViewHolder extends RecyclerView.ViewHolder {
        private TextView textTitle;
        private Spinner spinnerValue;
        private Context context;
        private ArrayAdapter<String> spinnerAdapter;

        public DiagnosisViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.name_characteristic);
            spinnerValue = itemView.findViewById(R.id.spinner_diagnosis);
            context = itemView.getContext();

            spinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, new ArrayList<>());
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerValue.setAdapter(spinnerAdapter);
        }

        public void bind(DiagnosisItem diagnosisItem) {
            try {
                textTitle.setText(diagnosisItem.getName());

                List<String> options = diagnosisItem.getOptions();

                spinnerAdapter.clear();
                spinnerAdapter.addAll(options);
                spinnerAdapter.notifyDataSetChanged();

                setSpinnerSelection(diagnosisItem);
                setupSpinnerListener(diagnosisItem);

            } catch (Exception e) {
                Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
            }
        }

        private void setupSpinnerListener(DiagnosisItem diagnosisItem) {
            spinnerValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    diagnosisItem.setValue(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    diagnosisItem.setValue(0);
                }
            });
        }

        private void setSpinnerSelection(DiagnosisItem diagnosisItem) {
            spinnerValue.setSelection(diagnosisItem.getValue());
        }
    }
}