package com.pancreatitis.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.pancreatitis.R;
import com.pancreatitis.models.Characteristic;
import com.pancreatitis.models.CharacteristicItem;
import com.pancreatitis.models.CharacteristicItemNonNumeric;
import com.pancreatitis.models.CharacteristicItemNumeric;
import com.pancreatitis.models.CharacterizationValue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class UnifiedCharacteristicAdapter extends RecyclerView.Adapter<UnifiedCharacteristicAdapter.CharacteristicViewHolder> {
    private List<CharacteristicItem> characteristicItems;
    private int idQuestionnaire;
    private boolean showTitle = true; // Флаг для отображения заголовка

    public UnifiedCharacteristicAdapter(List<CharacteristicItem> characteristicItems, int idQuestionnaire) {
        this.characteristicItems = characteristicItems;
        this.idQuestionnaire = idQuestionnaire;
    }

    public UnifiedCharacteristicAdapter(List<CharacteristicItem> characteristicItems, int idQuestionnaire, boolean showTitle) {
        this.characteristicItems = characteristicItems;
        this.idQuestionnaire = idQuestionnaire;
        this.showTitle = showTitle;
    }

    @Override
    public int getItemViewType(int position) {
        CharacteristicItem item = characteristicItems.get(position);
        return item.getIdType(); // 1, 2 или 3
    }

    @NonNull
    @Override
    public CharacteristicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        // Создаем базовый контейнер
        View containerView = inflater.inflate(R.layout.item_characteristic_title, parent, false);

        // В зависимости от типа добавляем соответствующую разметку
        View contentView;
        switch (viewType) {
            case 1:
            case 2:
                contentView = inflater.inflate(R.layout.item_characteristic_non_numeric, parent, false);
                break;
            case 3:
                contentView = inflater.inflate(R.layout.item_characteristic_numeric, parent, false);
                break;
            default:
                throw new IllegalArgumentException("Unknown view type: " + viewType);
        }

        // Добавляем контент в контейнер
        LinearLayout container = containerView.findViewById(R.id.characteristic_container);
        container.addView(contentView);

        return new CharacteristicViewHolder(containerView, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull CharacteristicViewHolder holder, int position) {
        try {
            CharacteristicItem characteristicItem = characteristicItems.get(position);
            holder.bind(characteristicItem);
        } catch (Exception e) {
            Toast.makeText(holder.context, e.getMessage(), Toast.LENGTH_LONG).show();
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

    public void addItem(CharacteristicItem characteristicItem) {
        characteristicItems.add(characteristicItem);
        notifyDataSetChanged();
    }

    public List<CharacteristicItem> getCharacteristics() {
        return characteristicItems;
    }

    public void setShowTitle(boolean showTitle) {
        if (this.showTitle != showTitle) {
            this.showTitle = showTitle;
            notifyDataSetChanged();
        }
    }

    public boolean isShowTitle() {
        return showTitle;
    }

    // Единый ViewHolder для всех типов
    class CharacteristicViewHolder extends RecyclerView.ViewHolder {
        private TextView textTitle;
        private ImageView btnHelp;
        private LinearLayout container;
        private Context context;
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // Поля для числовых характеристик
        private EditText valueCharacteristic;
        private TextView createdAtNumeric;

        // Поля для нечисловых характеристик
        private Spinner spinnerValue;
        private ArrayAdapter<String> spinnerAdapter;
        private TextView createdAtNonNumeric;
        private int viewType;
        private AdapterView.OnItemSelectedListener spinnerListener;

        public CharacteristicViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            this.viewType = viewType;
            this.context = itemView.getContext();

            // Инициализация заголовка (общая часть)
            container = itemView.findViewById(R.id.characteristic_container);
            textTitle = itemView.findViewById(R.id.name_characteristic);
            btnHelp = itemView.findViewById(R.id.get_helps);

            // Инициализация в зависимости от типа
            if (viewType == 1 || viewType == 2) {
                // Нечисловые характеристики
                spinnerValue = itemView.findViewById(R.id.value_characteristic);
                createdAtNonNumeric = itemView.findViewById(R.id.created_at);

                spinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, new ArrayList<>());
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerValue.setAdapter(spinnerAdapter);
            } else if (viewType == 3) {
                // Числовые характеристики
                valueCharacteristic = itemView.findViewById(R.id.value_characteristic);
                createdAtNumeric = itemView.findViewById(R.id.created_at);
            }
        }

        public void bind(CharacteristicItem characteristic) {
            // Общая часть: заголовок
            textTitle.setText(characteristic.getName());
            setupHelpButton(characteristic);

            // Настройка клика по заголовку
            container.setOnClickListener(v -> {
                if (showTitle) {
                    NavController navController = Navigation.findNavController(v);
                    Bundle args = new Bundle();
                    args.putInt("idQuestionnaire", idQuestionnaire);
                    args.putInt("idCharacteristic", characteristic.getIdCharacteristic());
                    navController.navigate(R.id.action_anket_to_characteristic, args);
                }
            });

            // Отображение/скрытие заголовка
            textTitle.setVisibility(showTitle ? View.VISIBLE : View.GONE);
            btnHelp.setVisibility(showTitle ? View.VISIBLE : View.GONE);

            // Специфичная часть: данные характеристики
            if ((viewType == 1 || viewType == 2) && characteristic instanceof CharacteristicItemNonNumeric) {
                bindNonNumeric((CharacteristicItemNonNumeric) characteristic);
            } else if (viewType == 3 && characteristic instanceof CharacteristicItemNumeric) {
                bindNumeric((CharacteristicItemNumeric) characteristic);
            }
        }

        private void bindNumeric(CharacteristicItemNumeric characteristic) {
            if (createdAtNumeric != null) {
                createdAtNumeric.setText(characteristic.getCreatedAt());
            }
            if (valueCharacteristic != null) {
                valueCharacteristic.setText(Float.toString(characteristic.getValue()));
            }
        }

        private void bindNonNumeric(CharacteristicItemNonNumeric characteristic) {
            if (createdAtNonNumeric != null) {
                createdAtNonNumeric.setText(characteristic.getCreatedAt());
            }

            if (spinnerAdapter != null && spinnerValue != null) {
                List<String> options = characteristic.getOptions();
                spinnerAdapter.clear();
                spinnerAdapter.addAll(options);
                spinnerAdapter.notifyDataSetChanged();

                // Убираем слушатель перед программной установкой
                spinnerValue.setOnItemSelectedListener(null);
                spinnerValue.setSelection(characteristic.getValue());

                // Восстанавливаем слушатель в следующем цикле UI, чтобы не поймать программное событие
                spinnerValue.post(() -> setupSpinnerListener(characteristic));

                if (showTitle) {
                    spinnerValue.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            if (event.getAction() == MotionEvent.ACTION_UP) {
                                container.performClick();
                            }
                            return true; // блокируем стандартное поведение Spinner (не раскрывать)
                        }
                    });
                }
            }
        }

        private void setupSpinnerListener(CharacteristicItemNonNumeric characteristic) {
            if (spinnerValue == null) return;

            // Сохраняем слушатель, чтобы иметь возможность снять/восстановить при необходимости
            spinnerListener = new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    // Это вызов от пользователя — обновляем value и createdAt
                    characteristic.setValue(position);
                    characteristic.setCreatedAt(LocalDateTime.now().format(formatter));
                    if (createdAtNonNumeric != null) {
                        createdAtNonNumeric.setText(characteristic.getCreatedAt());
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    characteristic.setValue(0);
                }
            };

            spinnerValue.setOnItemSelectedListener(spinnerListener);
        }

        private void setSpinnerSelection(CharacteristicItemNonNumeric characteristic) {
            if (spinnerValue != null) {
                spinnerValue.setSelection(characteristic.getValue());
            }
        }

        private void setupHelpButton(CharacteristicItem characteristic) {
            String hints = characteristic.getHint();
            boolean hasHints = hints != null && !hints.trim().isEmpty();

            if (btnHelp != null) {
                btnHelp.setVisibility(hasHints && showTitle ? View.VISIBLE : View.GONE);
                String dialogMessage = buildDialogMessage(characteristic);
                btnHelp.setOnClickListener(v -> showHelpDialog(dialogMessage));
            }
        }

        private String buildDialogMessage(CharacteristicItem characteristic) {
            StringBuilder message = new StringBuilder();
            String hints = characteristic.getHint();
            if (hints != null && !hints.trim().isEmpty()) {
                message.append(hints);
            }

            // Показываем доступные опции
            if (characteristic.getIdType() == 1 || characteristic.getIdType() == 2) {
                List<String> options = ((CharacteristicItemNonNumeric) characteristic).getOptions();
                if (options != null && !options.isEmpty()) {
                    message.append("\n\nДоступные варианты:\n• Нет данных");
                    for (String option : options) {
                        if (!option.isEmpty()) {
                            message.append("\n• ").append(option);
                        }
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