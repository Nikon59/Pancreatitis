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

public class CharacteristicItemAdapter extends RecyclerView.Adapter<CharacteristicItemAdapter.CharacteristicItemNonNumericViewHolder> {
    private List<CharacteristicItem> characteristicItems;
    private Context context;

    public CharacteristicItemAdapter(List<CharacteristicItem> characteristicItems, Context context) {
        this.characteristicItems = characteristicItems;
        this.context = context;
    }

    @NonNull
    @Override
    public CharacteristicItemNonNumericViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View titleView = inflater.inflate(R.layout.item_characteristic_non_numeric, parent, false);
        return new CharacteristicItemNonNumericViewHolder(titleView);
    }

    @Override
    public void onBindViewHolder(@NonNull CharacteristicItemNonNumericViewHolder holder, int position) {
        CharacteristicItem characteristicItem = characteristicItems.get(position);
        holder.bind((CharacteristicItemNonNumeric) characteristicItem);
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

//    class CharacteristicItemNumericViewHolder extends RecyclerView.ViewHolder {
//        private EditText valueCharacteristic;
//        private TextView createdAt;
//
//        public CharacteristicItemNumericViewHolder(@NonNull View itemView) {
//            super(itemView);
//            valueCharacteristic = itemView.findViewById(R.id.value_characteristic);
//            createdAt = itemView.findViewById(R.id.created_at);
//        }
//
//        public void bind(CharacteristicItemNumeric characteristic) {
//            valueCharacteristic.setText(Float.toString(characteristic.getValue()));
//        }
//    }

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

//            // Создаем адаптер
//            ArrayAdapter<String> adapter = new ArrayAdapter<>(
//                    itemView.getContext(),
//                    android.R.layout.simple_spinner_item,
//                    values
//            );
//
//            // Настраиваем вид выпадающего списка
//            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//
//            // Устанавливаем адаптер
//            valueCharacteristic.setAdapter(adapter);
        }
    }
}