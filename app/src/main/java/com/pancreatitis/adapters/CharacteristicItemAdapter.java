package com.pancreatitis.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.List;

public class CharacteristicItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private int idQuestionnaire;
    private List<CharacteristicItem> characteristicItems;

    public CharacteristicItemAdapter(List<CharacteristicItem> characteristicItems, int idQuestionnaire) {
        this.characteristicItems = characteristicItems;
        this.idQuestionnaire = idQuestionnaire;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_characteristic_title, parent, false);
        return new CharacteristicItemNumericViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        CharacteristicItem characteristicItem = characteristicItems.get(position);

        switch (characteristicItem.getIdType()){
            case 1:
                CharacteristicItemNonNumericViewHolder characteristicItemNonNumericViewHolder1 = (CharacteristicItemNonNumericViewHolder) holder;
                characteristicItemNonNumericViewHolder1.bind((CharacteristicItemNonNumeric) characteristicItem);
                break;
            case 2:
                CharacteristicItemNonNumericViewHolder characteristicItemNonNumericViewHolder2 = (CharacteristicItemNonNumericViewHolder) holder;
                characteristicItemNonNumericViewHolder2.bind((CharacteristicItemNonNumeric) characteristicItem);
                break;
            case 3:
                CharacteristicItemNumericViewHolder characteristicItemNumericViewHolder = (CharacteristicItemNumericViewHolder) holder;
                characteristicItemNumericViewHolder.bind((CharacteristicItemNumeric) characteristicItem);
                break;
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

    class CharacteristicItemNumericViewHolder extends RecyclerView.ViewHolder {
        private EditText valueCharacteristic;
        private TextView createdAt;

        public CharacteristicItemNumericViewHolder(@NonNull View itemView) {
            super(itemView);
            valueCharacteristic = itemView.findViewById(R.id.value_characteristic);
            createdAt = itemView.findViewById(R.id.created_at);
        }

        public void bind(CharacteristicItemNumeric characteristic) {
            valueCharacteristic.setText(Float.toString(characteristic.getValue()));
        }
    }

    class CharacteristicItemNonNumericViewHolder extends RecyclerView.ViewHolder {
        private Spinner valueCharacteristic;
        private TextView createdAt;

        public CharacteristicItemNonNumericViewHolder(@NonNull View itemView) {
            super(itemView);
            valueCharacteristic = itemView.findViewById(R.id.value_characteristic);
            createdAt = itemView.findViewById(R.id.created_at);
        }

        public void bind(CharacteristicItemNonNumeric characteristic) {
            // Получаем список значений из модели
            List<String> values = characteristic.getOptions();

            // Создаем адаптер
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    itemView.getContext(),
                    android.R.layout.simple_spinner_item,
                    values
            );

            // Настраиваем вид выпадающего списка
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            // Устанавливаем адаптер
            valueCharacteristic.setAdapter(adapter);
        }
    }
}