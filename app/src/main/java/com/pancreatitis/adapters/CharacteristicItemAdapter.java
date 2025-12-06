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

    private List<CharacteristicItem> characteristicItems;

    public CharacteristicItemAdapter(List<CharacteristicItem> characteristicItems) {
        this.characteristicItems = characteristicItems;
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
            CharacteristicItem characteristicItem = characteristicItems.get(position);
            holder.bind(characteristicItem);
        } catch (Exception e) {
            Toast.makeText(holder.context, e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public int getItemCount() {
        return characteristicItems.size();
    }

    public void updateData(List<CharacteristicItem> newCharacteristics) {
        characteristicItems.clear();
        characteristicItems.addAll(newCharacteristics);
        notifyDataSetChanged();
    }

    public List<CharacteristicItem> getCharacteristics() {
        return characteristicItems;
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

            spinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, new ArrayList<>());
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerValue.setAdapter(spinnerAdapter);
        }

        public void bind(CharacteristicItem characteristic) {
            try {
                textTitle.setText(characteristic.getName());

                List<String> options = characteristic.getOptions();

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

        private void setupSpinnerListener(CharacteristicItem characteristic) {
            spinnerValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    characteristic.setValue(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    characteristic.setValue(0);
                }
            });
        }

        private void setSpinnerSelection(CharacteristicItem characteristic) {
            spinnerValue.setSelection(characteristic.getValue());
        }

        private void setupHelpButton(CharacteristicItem characteristic) {
            String hints = characteristic.getHint();
            boolean hasHints = hints != null && !hints.trim().isEmpty();

            btnHelp.setVisibility(hasHints ? View.VISIBLE : View.GONE);
            String dialogMessage = buildDialogMessage(characteristic);
            btnHelp.setOnClickListener(v -> showHelpDialog(dialogMessage));
        }

        private String buildDialogMessage(CharacteristicItem characteristic) {
            StringBuilder message = new StringBuilder();

            String hints = characteristic.getHint();
            if (hints != null && !hints.trim().isEmpty()) {
                message.append(hints);
            }

            // Показываем доступные опции
            List<String> options = characteristic.getOptions();
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