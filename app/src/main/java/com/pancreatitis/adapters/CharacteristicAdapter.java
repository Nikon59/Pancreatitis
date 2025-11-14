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
import com.pancreatitis.models.Characteristic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CharacteristicAdapter extends RecyclerView.Adapter<CharacteristicAdapter.CharacteristicViewHolder> {

    private List<Characteristic> characteristics;
    private Map<Long, List<String>> characteristicOptionsMap; // Карта опций для каждой характеристики

    public CharacteristicAdapter(List<Characteristic> characteristics) {
        this.characteristics = characteristics;
        this.characteristicOptionsMap = new HashMap<>();
    }

    public CharacteristicAdapter(List<Characteristic> characteristics, Map<Long, List<String>> optionsMap) {
        this.characteristics = characteristics;
        this.characteristicOptionsMap = optionsMap != null ? optionsMap : new HashMap<>();
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
            Characteristic characteristic = characteristics.get(position);
            holder.bind(characteristic);
        } catch (Exception e) {
            Toast.makeText(holder.context, e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public int getItemCount() {
        return characteristics.size();
    }

    public void updateData(List<Characteristic> newCharacteristics) {
        characteristics.clear();
        characteristics.addAll(newCharacteristics);
        notifyDataSetChanged();
    }

    public List<Characteristic> getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristicOptionsMap(Map<Long, List<String>> optionsMap) {
        this.characteristicOptionsMap = optionsMap;
        notifyDataSetChanged();
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

        public void bind(Characteristic characteristic) {
            try {
                textTitle.setText(characteristic.getTitle());

                // Получаем опции для этой характеристики
                List<String> options = characteristicOptionsMap.get(characteristic.getId());
                if (options == null) {
                    options = new ArrayList<>();
                    options.add("");
                }

                // Обновляем адаптер Spinner
                spinnerAdapter.clear();
                spinnerAdapter.addAll(options);
                spinnerAdapter.notifyDataSetChanged();

                // Устанавливаем выбранное значение
                setSpinnerSelection(characteristic);

                //setupSpinnerListener(characteristic);
                setupHelpButton(characteristic);

            } catch (Exception e) {
                Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
            }
        }

        private void setSpinnerSelection(Characteristic characteristic) {
            try {
                spinnerValue.setSelection(Integer.parseInt(characteristic.getValue()));
            } catch (Exception e) {
                Toast.makeText(context, characteristic.getValue(), Toast.LENGTH_SHORT).show();
                spinnerValue.setSelection(0);
            }
        }

//        private void setupSpinnerListener(Characteristic characteristic) {
//            spinnerValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//                @Override
//                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                    try {
//                        String selectedValue = (String) parent.getItemAtPosition(position);
//                        // Обновляем значение в характеристике
//                        //characteristic.setValue(selectedValue);
//                    } catch (Exception e) {
//                        Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
//                    }
//                }
//
//                @Override
//                public void onNothingSelected(AdapterView<?> parent) {
//                    characteristic.setValue("");
//                }
//            });
//        }

        private void setupHelpButton(Characteristic characteristic) {
            String hints = characteristic.getHints();
            boolean hasHints = hints != null && !hints.trim().isEmpty();

            btnHelp.setVisibility(hasHints ? View.VISIBLE : View.GONE);

            if (hasHints) {
                String dialogMessage = buildDialogMessage(characteristic);
                btnHelp.setOnClickListener(v -> showHelpDialog(dialogMessage));
            } else {
                btnHelp.setOnClickListener(null);
            }
        }

        private String buildDialogMessage(Characteristic characteristic) {
            StringBuilder message = new StringBuilder();

            String hints = characteristic.getHints();
            if (hints != null && !hints.trim().isEmpty()) {
                message.append(hints);
            }

            // Показываем доступные опции
            List<String> options = characteristicOptionsMap.get(characteristic.getId());
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