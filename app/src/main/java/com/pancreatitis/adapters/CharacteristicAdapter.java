// CharacteristicAdapter.java
package com.pancreatitis.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.pancreatitis.R;
import com.pancreatitis.models.Characteristic;
import java.util.List;

public class CharacteristicAdapter extends RecyclerView.Adapter<CharacteristicAdapter.CharacteristicViewHolder> {

    private List<Characteristic> characteristics;

    public CharacteristicAdapter(List<Characteristic> characteristics) {
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

    // Метод для получения всех данных
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

            // Очищаем предыдущие TextWatcher'ы
            editValue.setTag(null);

            // Добавляем TextWatcher для отслеживания изменений
            editValue.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String newValue = s.toString();
                    characteristic.setValue(newValue);

                    // Валидация значения, если кнопка помощи активна
                    if (btnHelp.getVisibility() == View.VISIBLE) {
                        validateInput(characteristic, newValue);
                    }
                }
            });

            // Обработка кнопки помощи
            String hints = characteristic.getHints();

            if ((hints == null || hints.trim().isEmpty())) {
                btnHelp.setVisibility(View.GONE);
            } else {
                btnHelp.setVisibility(View.VISIBLE);
                String dialogMessage = buildDialogMessage(characteristic);
                btnHelp.setOnClickListener(v -> showHelpDialog(dialogMessage));

                // Проверяем начальное значение при наличии валидации
                if (characteristic.getValue() != null &&
                        !characteristic.getValue().isEmpty()) {
                    validateInput(characteristic, characteristic.getValue());
                }
            }
        }

        private void validateInput(Characteristic characteristic, String value) {
            if (value == null || value.trim().isEmpty()) {
                return;
            }

            try {
                int numericValue = Integer.parseInt(value);
                int minValue = characteristic.getMin_value();
                int maxValue = characteristic.getMax_value();

                boolean isValid = true;
                String errorMessage = null;

                if (numericValue < minValue) {
                    isValid = false;
                    errorMessage = "Значение не может быть меньше " + minValue;
                } else if (numericValue > maxValue) {
                    isValid = false;
                    errorMessage = "Значение не может быть больше " + maxValue;
                }

                if (!isValid) {
                    showValidationError(errorMessage);
                    // Можно также сбросить значение на предыдущее валидное или граничное
                    // characteristic.setValue(...);
                } else {
                    editValue.setError(null);
                }

            } catch (NumberFormatException e) {
                // Если значение не числовое, показываем ошибку
                showValidationError("Введите числовое значение");
            }
        }

        private void showValidationError(String message) {
            // Альтернативный вариант: выделение поля красным
            editValue.setError(message);

            // Можно также использовать диалог для важных ошибок:
            /*
            new AlertDialog.Builder(context)
                .setTitle("Ошибка ввода")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
            */
        }

        private String buildDialogMessage(Characteristic characteristic) {
            StringBuilder message = new StringBuilder();

            String hints = characteristic.getHints();
            if (hints != null && !hints.trim().isEmpty()) {
                message.append(hints);
            }

            // Добавляем информацию о допустимом диапазоне, если есть валидация
            message.append("\nДопустимый диапазон: ");

            int minValue = characteristic.getMin_value();
            int maxValue = characteristic.getMax_value();

            message.append("от ").append(minValue).append(" до ").append(maxValue);

            return message.toString();
        }

        private void showHelpDialog(String hints) {
            Context context = itemView.getContext();

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Подсказка")
                    .setMessage(hints)
                    .setPositiveButton("Понятно", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .setCancelable(true);

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    // Простой TextWatcher для избежания дублирования кода
    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void afterTextChanged(android.text.Editable s) {}
    }
}