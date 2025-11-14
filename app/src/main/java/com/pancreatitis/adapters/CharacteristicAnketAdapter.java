package com.pancreatitis.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pancreatitis.R;
import com.pancreatitis.models.Characteristic;

import java.util.List;

public class CharacteristicAnketAdapter extends RecyclerView.Adapter<CharacteristicAnketAdapter.CharacteristicViewHolder> {

    private List<Characteristic> characteristics;

    public CharacteristicAnketAdapter(List<Characteristic> characteristics) {
        this.characteristics = characteristics;
    }

    @NonNull
    @Override
    public CharacteristicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.characteristic_anket, parent, false);
        return new CharacteristicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CharacteristicViewHolder holder, int position) {
        Characteristic characteristic = characteristics.get(position);
        holder.bind(characteristic);
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

    static class CharacteristicViewHolder extends RecyclerView.ViewHolder {
        private TextView textTitle;
        private EditText editValue;
        private ImageView btnHelp;
        private Context context;

        public CharacteristicViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.name_characteristic);
            editValue = itemView.findViewById(R.id.value_characteristic);
            btnHelp = itemView.findViewById(R.id.get_helps);
            context = itemView.getContext();
        }

        public void bind(Characteristic characteristic) {
            textTitle.setText(characteristic.getTitle());
            editValue.setText(characteristic.getValue());
            editValue.setInputType(characteristic.getInputType());

            setupTextWatcher(characteristic);
            setupHelpButton(characteristic);
        }

        private void setupTextWatcher(Characteristic characteristic) {
            // Clear previous TextWatcher
            editValue.setTag(null);

            TextWatcher textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    characteristic.setValue(s.toString());
                    if (btnHelp.getVisibility() == View.VISIBLE) {
                        validateInput(characteristic, s.toString());
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            };

            editValue.addTextChangedListener(textWatcher);
            editValue.setTag(textWatcher);
        }

        private void setupHelpButton(Characteristic characteristic) {
            String hints = characteristic.getHints();
            boolean hasHints = hints != null && !hints.trim().isEmpty();

            btnHelp.setVisibility(hasHints ? View.VISIBLE : View.GONE);

            if (hasHints) {
                String dialogMessage = buildDialogMessage(characteristic);
                btnHelp.setOnClickListener(v -> showHelpDialog(dialogMessage));

                // Validate initial value
                String currentValue = characteristic.getValue();
                if (currentValue != null && !currentValue.isEmpty()) {
                    validateInput(characteristic, currentValue);
                }
            } else {
                btnHelp.setOnClickListener(null);
            }
        }

        private void validateInput(Characteristic characteristic, String value) {
            if (value == null || value.trim().isEmpty()) {
                editValue.setError(null);
                return;
            }

            try {
                int numericValue = Integer.parseInt(value);
                int minValue = characteristic.getMin_value();
                int maxValue = characteristic.getMax_value();

                if (numericValue < minValue) {
                    editValue.setError("Value cannot be less than " + minValue);
                } else if (numericValue > maxValue) {
                    editValue.setError("Value cannot be greater than " + maxValue);
                } else {
                    editValue.setError(null);
                }
            } catch (NumberFormatException e) {
                editValue.setError("Please enter a valid number");
            }
        }

        private String buildDialogMessage(Characteristic characteristic) {
            StringBuilder message = new StringBuilder();

            String hints = characteristic.getHints();
            if (hints != null && !hints.trim().isEmpty()) {
                message.append(hints);
            }

            message.append("\n\nAllowed range: ")
                    .append(characteristic.getMin_value())
                    .append(" to ")
                    .append(characteristic.getMax_value());

            return message.toString();
        }

        private void showHelpDialog(String message) {
            new AlertDialog.Builder(context)
                    .setTitle("Help")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
        }
    }
}