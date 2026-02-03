package com.pancreatitis.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.pancreatitis.R;
import com.pancreatitis.models.CharacteristicItem;
import com.pancreatitis.models.CharacteristicItemNonNumeric;
import com.pancreatitis.models.CharacteristicItemNumeric;
import com.pancreatitis.models.DiagnosisItem;

import java.util.ArrayList;
import java.util.List;

public class CharacteristicItemAdapter extends RecyclerView.Adapter<CharacteristicItemAdapter.CharacteristicItemNonNumericViewHolder> {
    private List<CharacteristicItemNonNumeric> characteristicItems;

    public CharacteristicItemAdapter(List<CharacteristicItemNonNumeric> characteristicItems) {
        this.characteristicItems = characteristicItems;
    }

    @NonNull
    @Override
    public CharacteristicItemNonNumericViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_characteristic_non_numeric, parent, false);
        return new CharacteristicItemNonNumericViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CharacteristicItemNonNumericViewHolder holder, int position) {
        CharacteristicItemNonNumeric characteristicItem = characteristicItems.get(position);
        holder.bind(characteristicItem);
    }

    @Override
    public int getItemCount() {
        return characteristicItems.size();
    }

    public void updateData(List<CharacteristicItemNonNumeric> newCharacteristics) {
        characteristicItems.clear();
        characteristicItems.addAll(newCharacteristics);
        notifyDataSetChanged();
    }

    public List<CharacteristicItemNonNumeric> getCharacteristics() {
        return characteristicItems;
    }

    class CharacteristicItemNonNumericViewHolder extends RecyclerView.ViewHolder {
        private Spinner spinnerValue;
        private Context context;
        private ArrayAdapter<String> spinnerAdapter;
        private TextView createdAt;

        public CharacteristicItemNonNumericViewHolder(@NonNull View itemView) {
            super(itemView);
            spinnerValue = itemView.findViewById(R.id.value_characteristic);
            createdAt = itemView.findViewById(R.id.created_at);
            context = itemView.getContext();

            spinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, new ArrayList<>());
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerValue.setAdapter(spinnerAdapter);
        }

        public void bind(CharacteristicItemNonNumeric characteristic) {
            createdAt.setText("2026");
            List<String> options = characteristic.getOptions();

            spinnerAdapter.clear();
            spinnerAdapter.addAll(options);
            spinnerAdapter.notifyDataSetChanged();

            setSpinnerSelection(characteristic);
            setupSpinnerListener(characteristic);
        }

        private void setupSpinnerListener(CharacteristicItemNonNumeric characteristic) {
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

        private void setSpinnerSelection(CharacteristicItemNonNumeric characteristic) {
            spinnerValue.setSelection(characteristic.getValue());
        }
    }
}