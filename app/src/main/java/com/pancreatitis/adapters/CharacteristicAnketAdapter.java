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
import java.util.Objects;

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

    public void lockFieldByTitle(String fieldTitle) {
        for (int i = 0; i < characteristics.size(); i++) {
            Characteristic characteristic = characteristics.get(i);
            if (Objects.equals(characteristic.getTitle(), fieldTitle)) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    static class CharacteristicViewHolder extends RecyclerView.ViewHolder {
        private TextView textTitle;
        private EditText editValue;
        private ImageView btnHelp;

        public CharacteristicViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.name_characteristic);
            editValue = itemView.findViewById(R.id.value_characteristic);
            btnHelp = itemView.findViewById(R.id.get_helps);
        }

        public void bind(Characteristic characteristic) {
            textTitle.setText(characteristic.getTitle());
            editValue.setText(characteristic.getValue());
            editValue.setInputType(characteristic.getInputType());

            if (Objects.equals(characteristic.getTitle(), "Full Name") && characteristic.getValue() != null && !Objects.equals(characteristic.getValue(), "")) {
                editValue.setEnabled(false);
            }

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
        }
    }
}