package com.pancreatitis.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pancreatitis.R;
import com.pancreatitis.models.Characteristic;

import java.util.List;

public class DiagnosisAdapter extends RecyclerView.Adapter<DiagnosisAdapter.DiagnosisViewHolder> {

    private List<Characteristic> characteristics;
    private List<String> diagnosisOptions;

    public DiagnosisAdapter(List<Characteristic> characteristics, List<String> diagnosisOptions) {
        this.characteristics = characteristics;
        this.diagnosisOptions = diagnosisOptions;
    }

    @NonNull
    @Override
    public DiagnosisViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_diagnosis_spinner, parent, false);
        return new DiagnosisViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiagnosisViewHolder holder, int position) {
        Characteristic characteristic = characteristics.get(position);
        holder.bind(characteristic, diagnosisOptions);
    }

    @Override
    public int getItemCount() {
        return characteristics.size();
    }

    public List<Characteristic> getCharacteristics() {
        return characteristics;
    }

    static class DiagnosisViewHolder extends RecyclerView.ViewHolder {
        private TextView textTitle;
        private Spinner spinnerDiagnosis;
        private Context context;

        public DiagnosisViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.name_characteristic);
            spinnerDiagnosis = itemView.findViewById(R.id.spinner_diagnosis);
            context = itemView.getContext();
        }

        public void bind(Characteristic characteristic, List<String> diagnosisOptions) {
            textTitle.setText(characteristic.getTitle());

            // Настраиваем адаптер для Spinner
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    context,
                    android.R.layout.simple_spinner_item,
                    diagnosisOptions
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerDiagnosis.setAdapter(adapter);

            // Устанавливаем текущее значение
            String currentValue = characteristic.getValue();
            if (currentValue != null && !currentValue.isEmpty()) {
                for (int i = 0; i < diagnosisOptions.size(); i++) {
                    if (diagnosisOptions.get(i).equals(currentValue)) {
                        spinnerDiagnosis.setSelection(i);
                        break;
                    }
                }
            } else {
                spinnerDiagnosis.setSelection(0); // Выбираем пустую строку
            }

            // Обработка выбора
            spinnerDiagnosis.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    String selectedDiagnosis = diagnosisOptions.get(position);
                    characteristic.setValue(selectedDiagnosis);
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {
                    characteristic.setValue("");
                }
            });
        }
    }
}