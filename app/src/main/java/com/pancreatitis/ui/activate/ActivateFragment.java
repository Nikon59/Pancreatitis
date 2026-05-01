package com.pancreatitis.ui.activate;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.pancreatitis.R;
import com.pancreatitis.modules.bip39.Bip39Decoder;
import com.pancreatitis.modules.localstorage.LocalStorageModule;

import java.util.ArrayList;
import java.util.List;

public class ActivateFragment extends Fragment {

    // Количество слов мнемонической фразы (12 или 24)
    private static final int WORD_COUNT = 24;
    // Количество столбцов в сетке
    private static final int COLUMN_COUNT = 3;

    private View loadingOverlay;
    private List<EditText> wordEditTexts;
    private EditText loginEditTextGlobal;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activate, container, false);

        FrameLayout rootLayout = view.findViewById(R.id.root_layout);
        loadingOverlay = view.findViewById(R.id.loading_overlay);

        // Основной контейнер для контента
        LinearLayout contentLayout = new LinearLayout(getContext());
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(16, 16, 16, 16);

        // Заголовок
        TextView title = new TextView(getContext());
        title.setText("Введите мнемоническую фразу (" + WORD_COUNT + " слов)");
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.BLACK);
        contentLayout.addView(title);

        // Поле ввода логина
        EditText loginEditText = new EditText(getContext());
        loginEditText.setHint("Логин");
        loginEditText.setSingleLine(true);
        LinearLayout.LayoutParams loginParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        loginParams.setMargins(0, 16, 0, 16);
        loginEditText.setLayoutParams(loginParams);
        contentLayout.addView(loginEditText);
        loginEditTextGlobal = loginEditText;

        // Прокручиваемая сетка для слов
        ScrollView scrollView = new ScrollView(getContext());
        GridLayout gridLayout = new GridLayout(getContext());
        gridLayout.setColumnCount(COLUMN_COUNT);
        wordEditTexts = new ArrayList<>();

        for (int i = 0; i < WORD_COUNT; i++) {
            EditText editText = new EditText(getContext());
            editText.setHint("Слово " + (i + 1));
            editText.setSingleLine(true);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(i % COLUMN_COUNT, 1f);
            params.rowSpec = GridLayout.spec(i / COLUMN_COUNT);
            params.setMargins(4, 4, 4, 4);
            editText.setLayoutParams(params);

            gridLayout.addView(editText);
            wordEditTexts.add(editText);
        }
        scrollView.addView(gridLayout);

        // Даём прокрутке занять всё доступное пространство
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        contentLayout.addView(scrollView, scrollParams);

        // Кнопка активации
        Button activateButton = new Button(getContext());
        activateButton.setText("Активация");
        activateButton.setOnClickListener(v -> onActivate());
        activateButton.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.colorActive));
        activateButton.setTextColor(getResources().getColor(android.R.color.white, null));
        contentLayout.addView(activateButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Добавляем контент в корень FrameLayout перед loading overlay
        rootLayout.addView(contentLayout, 0);

        return view;
    }

    /**
     * Обработчик нажатия кнопки "Активация".
     * Собирает слова из полей и передаёт в заглушку.
     */
    private void onActivate() {
        String login = loginEditTextGlobal.getText().toString().trim();
        if (login.isEmpty()) {
            Toast.makeText(getContext(), "Введите логин", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> words = new ArrayList<>();
        for (EditText et : wordEditTexts) {
            String word = et.getText().toString().trim().toLowerCase();
            if (word.isEmpty()) {
                Toast.makeText(getContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }
            words.add(word);
        }

        // Показываем индикатор загрузки
        loadingOverlay.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                // Восстанавливаем ключ из мнемонической фразы
                String mnemonic = String.join(" ", words);
                byte[] encryptedKey = Bip39Decoder.fromMnemonic(mnemonic);

                // Сохраняем ключ в папку пользователя
                LocalStorageModule localStorage = LocalStorageModule.getInstance();
                localStorage.createUserFolder(login);
                localStorage.saveLocalUserKey(login, encryptedKey);

                requireActivity().runOnUiThread(() -> {
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Ключ успешно сохранён", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Неправильный набор слов", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}