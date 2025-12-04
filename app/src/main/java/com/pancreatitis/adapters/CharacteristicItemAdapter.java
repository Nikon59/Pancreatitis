package com.pancreatitis.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pancreatitis.R;
import com.pancreatitis.models.*;

import java.util.*;
import java.util.stream.Collectors;

public class CharacteristicItemAdapter extends RecyclerView.Adapter<CharacteristicItemAdapter.CharacteristicViewHolder> {

    private List<CharacterizationAnketPatient> characteristics;

    public CharacteristicItemAdapter(List<CharacterizationAnketPatient> characteristics) {
        this.characteristics = characteristics;
    }

    @NonNull
    @Override
    public CharacteristicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.characteristic, parent, false);
        return new CharacteristicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CharacteristicViewHolder holder, int position) {
        try {
            CharacterizationAnketPatient characteristic = characteristics.get(position);
            holder.bind(characteristic);
        } catch (Exception e) {
            Toast.makeText(holder.context, e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public int getItemCount() {
        return characteristics.size();
    }

    public void updateData(List<CharacterizationAnketPatient> newCharacteristics) {
        characteristics.clear();
        characteristics.addAll(newCharacteristics);
        notifyDataSetChanged();
    }

    public List<CharacterizationAnketPatient> getCharacteristics() {
        return characteristics;
    }

    class CharacteristicViewHolder extends RecyclerView.ViewHolder {
        private TextView textTitle;
        private Spinner spinnerValue;
        private ImageView btnHelp;
        private Context context;
        private ArrayAdapter<String> spinnerAdapter;

        public CharacteristicViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.name_characteristic);
            spinnerValue = itemView.findViewById(R.id.value_characteristic);
            btnHelp = itemView.findViewById(R.id.get_helps);
            context = itemView.getContext();

            // Настраиваем адаптер для Spinner
            spinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, new ArrayList<>());
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerValue.setAdapter(spinnerAdapter);
        }

        public void bind(CharacterizationAnketPatient characteristic) {
            try {
                textTitle.setText(characteristic.getCharacteristic().getOpis());

                // Получаем опции для этой характеристики
                List<String> options = characteristic.getCharacteristic().getCharacterizationValue()
                        .stream()
                        .map(CharacterizationValue::getValue)
                        .collect(Collectors.toList());

                // Обновляем адаптер Spinner
                spinnerAdapter.clear();
                spinnerAdapter.addAll(options);
                spinnerAdapter.notifyDataSetChanged();

                setSpinnerSelection(characteristic);
                setupSpinnerListener(characteristic);
                setupHelpButton(characteristic);

            } catch (Exception e) {
                Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
            }
        }

        private void setupSpinnerListener(CharacterizationAnketPatient characteristic) {
            spinnerValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    // Сохраняем индекс выбранного элемента в characteristic.value
                    characteristic.setValue(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    characteristic.setValue(0);
                }
            });
        }

        private void setSpinnerSelection(CharacterizationAnketPatient characteristic) {
            spinnerValue.setSelection(characteristic.getValue());
        }

        private void setupHelpButton(CharacterizationAnketPatient characteristic) {
            String hints = characteristic.getCharacteristic().getHints();
            boolean hasHints = hints != null && !hints.trim().isEmpty();

            btnHelp.setVisibility(hasHints ? View.VISIBLE : View.GONE);

            if (hasHints) {
                String dialogMessage = buildDialogMessage(characteristic);
                btnHelp.setOnClickListener(v -> showHelpDialog(dialogMessage));
            } else {
                btnHelp.setOnClickListener(null);
            }
        }

        private String buildDialogMessage(CharacterizationAnketPatient characteristic) {
            StringBuilder message = new StringBuilder();

            String hints = characteristic.getCharacteristic().getHints();
            if (hints != null && !hints.trim().isEmpty()) {
                message.append(hints);
            }

            // Показываем доступные опции
            List<String> options = characteristic.getCharacteristic().getCharacterizationValue()
                    .stream()
                    .map(CharacterizationValue::getValue)
                    .collect(Collectors.toList());
            if (options != null && !options.isEmpty()) {
                message.append("\n\nДоступные варианты:");
                for (String option : options) {
                    if (!option.isEmpty()) {
                        message.append("\n• ").append(option);
                    }
                }
            }

            return message.toString();
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