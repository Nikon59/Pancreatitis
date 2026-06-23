package com.pancreatitis.ui.activate;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.pancreatitis.MainActivity;
import com.pancreatitis.R;
import com.pancreatitis.modules.bip39.Bip39Decoder;
import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.localstorage.LocalStorageModule;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ActivateFragment extends Fragment {

    private static final int TIMEOUT_SECONDS = 300;          // 5 минут
    private static final int CHECK_INTERVAL_SECONDS = 3;    // проверка каждые 3 секунды

    private EditText loginEditText;
    private Button activateButton;
    private FrameLayout loadingOverlay;
    private TextView timerTextView;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> checkFuture;
    private ScheduledFuture<?> timerUpdateFuture;
    private Handler uiHandler = new Handler(Looper.getMainLooper());
    private int remainingSeconds = TIMEOUT_SECONDS;
    private boolean isFinished = false; // флаг, чтобы не выполнять повторные действия

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activate, container, false);

        loginEditText = view.findViewById(R.id.loginEditText);
        activateButton = view.findViewById(R.id.btn_activate);
        loadingOverlay = view.findViewById(R.id.loading_overlay);
        timerTextView = loadingOverlay.findViewById(R.id.timerTextView);

        activateButton.setOnClickListener(v -> onActivate());

        return view;
    }

    private void onActivate() {
        String login = loginEditText.getText().toString().trim();
        if (login.isEmpty()) {
            Toast.makeText(getContext(), "Введите логин", Toast.LENGTH_SHORT).show();
            return;
        }

        // Блокируем UI и показываем оверлей
        loadingOverlay.setVisibility(View.VISIBLE);
        timerTextView.setVisibility(View.VISIBLE);
        ((MainActivity) requireActivity()).lockNavigation();

        // Сбрасываем счётчик
        remainingSeconds = TIMEOUT_SECONDS;
        isFinished = false;
        updateTimerText();

        // Создаём планировщик, если ещё не создан
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
        }

        // Задача для обновления таймера на UI (каждую секунду)
        Runnable timerUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isFinished) return;
                remainingSeconds--;
                updateTimerText();
                if (remainingSeconds <= 0) {
                    // Время истекло – завершаем с ошибкой
                    finishWithError("Время ожидания истекло. Файл не появился.");
                } else {
                    // Планируем следующее обновление через 1 секунду
                    uiHandler.postDelayed(this, 1000);
                }
            }
        };

        // Задача для проверки наличия файла в облаке (периодическая)
        Runnable checkRunnable = new Runnable() {
            @Override
            public void run() {
                if (isFinished) return;
                try {
                    CloudStorageModule cloud = CloudStorageModule.getInstance();
                    String filePath = "/users/" + login + ".txt";

                    // Проверяем существование файла
                    if (cloud.fileExists(filePath)) {
                        // Файл найден – скачиваем и обрабатываем
                        byte[] fileData = cloud.downloadFile(filePath);
                        String content = new String(fileData, StandardCharsets.UTF_8);
                        String[] words = content.trim().split("\\s+");

                        if (words.length != 24) {
                            throw new Exception("Файл должен содержать ровно 24 слова");
                        }

                        // Восстанавливаем ключ из мнемонической фразы
                        String mnemonic = String.join(" ", words);
                        byte[] encryptedKey = Bip39Decoder.fromMnemonic(mnemonic);

                        // Сохраняем ключ локально
                        LocalStorageModule localStorage = LocalStorageModule.getInstance();
                        localStorage.createUserFolder(login);
                        localStorage.saveLocalUserKey(login, encryptedKey);

                        // Удаляем файл из облака
                        cloud.deleteFile(filePath);

                        // Успех – завершаем с положительным результатом
                        finishWithSuccess("Активация выполнена успешно!");
                        return;
                    }
                } catch (FileNotFoundException e) {
                    // Файл ещё не появился – ничего не делаем, продолжим проверку
                } catch (Exception e) {
                    // Ошибка при проверке или обработке – завершаем с ошибкой
                    finishWithError("Ошибка: " + e.getMessage());
                }
            }
        };

        // Запускаем периодическую проверку (первый запуск сразу)
        checkFuture = scheduler.scheduleAtFixedRate(checkRunnable, 0, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);

        // Запускаем обновление таймера (первый запуск через 1 секунду)
        uiHandler.postDelayed(timerUpdateRunnable, 1000);
    }

    private void updateTimerText() {
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        String text = String.format("Осталось: %02d:%02d", minutes, seconds);
        timerTextView.setText(text);
    }

    private void finishWithSuccess(String message) {
        if (isFinished) return;
        isFinished = true;
        cancelTasks();
        showToast(message);
        hideOverlayAndUnlock();
    }

    private void finishWithError(String message) {
        if (isFinished) return;
        isFinished = true;
        cancelTasks();
        showToast(message);
        hideOverlayAndUnlock();
    }

    private void cancelTasks() {
        if (checkFuture != null) {
            checkFuture.cancel(false);
            checkFuture = null;
        }
        if (timerUpdateFuture != null) {
            timerUpdateFuture.cancel(false);
            timerUpdateFuture = null;
        }
        uiHandler.removeCallbacksAndMessages(null);
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private void hideOverlayAndUnlock() {
        requireActivity().runOnUiThread(() -> {
            loadingOverlay.setVisibility(View.GONE);
            timerTextView.setVisibility(View.GONE);
            ((MainActivity) requireActivity()).unlockNavigation();
        });
    }

    private void showToast(String message) {
        requireActivity().runOnUiThread(() ->
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show()
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancelTasks();
        // Скрываем оверлей, если он виден
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.GONE);
        }
        if (timerTextView != null) {
            timerTextView.setVisibility(View.GONE);
        }
        ((MainActivity) requireActivity()).unlockNavigation();
    }
}