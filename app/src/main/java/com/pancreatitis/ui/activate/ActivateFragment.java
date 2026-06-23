package com.pancreatitis.ui.activate;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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

    private static final int TIMEOUT_SECONDS = 30;
    private static final int CHECK_INTERVAL_SECONDS = 3;

    // Статическое состояние, общее для всех экземпляров фрагмента
    private static boolean sIsRunning = false;
    private static int sRemainingSeconds = TIMEOUT_SECONDS;
    private static boolean sIsFinished = true;
    private static String sResultMessage = null;
    private static ScheduledExecutorService sScheduler = null;
    private static ScheduledFuture<?> sCheckFuture = null;

    private EditText loginEditText;
    private Button activateButton;
    private TextView timerTextView;
    private Handler uiHandler = new Handler(Looper.getMainLooper());
    private boolean isTimerUpdateRunning = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activate, container, false);

        loginEditText = view.findViewById(R.id.loginEditText);
        activateButton = view.findViewById(R.id.btn_activate);
        timerTextView = view.findViewById(R.id.timerTextView);

        activateButton.setOnClickListener(v -> onActivate());

        restoreUiState();
        return view;
    }

    private void onActivate() {
        if (getContext() == null) return;

        String login = loginEditText.getText().toString().trim();
        if (login.isEmpty()) {
            Toast.makeText(getContext(), "Введите логин", Toast.LENGTH_SHORT).show();
            return;
        }

        if (sIsRunning) {
            Toast.makeText(getContext(), "Процесс уже запущен", Toast.LENGTH_SHORT).show();
            return;
        }

        // Сброс состояния
        sIsRunning = true;
        sIsFinished = false;
        sRemainingSeconds = TIMEOUT_SECONDS;
        sResultMessage = null;

        showTimerMode();

        if (sScheduler == null || sScheduler.isShutdown()) {
            sScheduler = Executors.newSingleThreadScheduledExecutor();
        }

        if (sCheckFuture == null || sCheckFuture.isCancelled()) {
            Runnable checkRunnable = () -> {
                if (sIsFinished) return;
                CloudStorageModule cloud = CloudStorageModule.getInstance();
                String filePath = "/users/" + login + ".txt";
                try {
                    if (cloud.fileExists(filePath)) {
                        byte[] fileData = cloud.downloadFile(filePath);
                        String content = new String(fileData, StandardCharsets.UTF_8);
                        String[] words = content.trim().split("\\s+");

                        if (words.length != 24) {
                            throw new Exception("Файл должен содержать ровно 24 слова");
                        }

                        String mnemonic = String.join(" ", words);
                        byte[] encryptedKey = Bip39Decoder.fromMnemonic(mnemonic);

                        LocalStorageModule localStorage = LocalStorageModule.getInstance();
                        localStorage.createUserFolder(login);
                        localStorage.saveLocalUserKey(login, encryptedKey);

                        finishWithSuccess("Активация выполнена успешно!");
                    }
                } catch (FileNotFoundException e) {
                    // файл ещё не появился – игнорируем
                } catch (Exception e) {
                    finishWithError("Ошибка: " + e.getMessage());
                } finally {
                    cloud.deleteFile(filePath);
                }
            };
            sCheckFuture = sScheduler.scheduleAtFixedRate(checkRunnable, 0,
                    CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
        }

        startTimerUpdate();
    }

    private void startTimerUpdate() {
        if (isTimerUpdateRunning) return;
        if (!isAdded()) return; // фрагмент не прикреплён – не запускаем

        isTimerUpdateRunning = true;
        updateTimerText();

        uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (sIsFinished) {
                    isTimerUpdateRunning = false;
                    return;
                }
                sRemainingSeconds--;
                updateTimerText();

                if (sRemainingSeconds <= 0) {
                    finishWithError("Время ожидания истекло");
                    isTimerUpdateRunning = false;
                } else {
                    // планируем следующий шаг только если фрагмент всё ещё активен
                    if (isAdded()) {
                        uiHandler.postDelayed(this, 1000);
                    } else {
                        isTimerUpdateRunning = false;
                    }
                }
            }
        }, 1000);
    }

    private void updateTimerText() {
        // Проверяем, что фрагмент прикреплён и view существует
        if (!isAdded() || timerTextView == null) return;

        int minutes = sRemainingSeconds / 60;
        int seconds = sRemainingSeconds % 60;
        String text = String.format("Осталось: %02d:%02d", minutes, seconds);
        timerTextView.setText(text);
    }

    private void showTimerMode() {
        if (activateButton != null) activateButton.setVisibility(View.GONE);
        if (timerTextView != null) {
            timerTextView.setVisibility(View.VISIBLE);
            updateTimerText();
        }
    }

    private void showButtonMode() {
        if (activateButton != null) activateButton.setVisibility(View.VISIBLE);
        if (timerTextView != null) timerTextView.setVisibility(View.GONE);
    }

    private void finishWithSuccess(String message) {
        finishProcess(true, message);
    }

    private void finishWithError(String message) {
        finishProcess(false, message);
    }

    private synchronized void finishProcess(boolean success, String message) {
        if (sIsFinished) return;
        sIsFinished = true;
        sIsRunning = false;
        sResultMessage = message;

        cancelTasks();

        // Обновляем UI, только если фрагмент прикреплён и активность жива
        if (isAdded() && getActivity() != null) {
            requireActivity().runOnUiThread(() -> {
                showButtonMode();
                if (getContext() != null) {
                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                }
                sResultMessage = null;
            });
        }
        // Если фрагмент неактивен – сообщение будет показано при восстановлении
    }

    private void cancelTasks() {
        if (sCheckFuture != null) {
            sCheckFuture.cancel(false);
            sCheckFuture = null;
        }
        if (sScheduler != null) {
            sScheduler.shutdownNow();
            sScheduler = null;
        }
        uiHandler.removeCallbacksAndMessages(null);
        isTimerUpdateRunning = false;
    }

    private void restoreUiState() {
        if (sIsFinished && sResultMessage != null) {
            // Показываем сохранённое сообщение, если есть контекст
            if (getContext() != null) {
                Toast.makeText(getContext(), sResultMessage, Toast.LENGTH_LONG).show();
            }
            sResultMessage = null;
            showButtonMode();
            return;
        }

        if (sIsRunning && !sIsFinished) {
            showTimerMode();
            if (!isTimerUpdateRunning) {
                startTimerUpdate();
            }
        } else {
            showButtonMode();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Останавливаем только обновление таймера, фоновые задачи не трогаем
        uiHandler.removeCallbacksAndMessages(null);
        isTimerUpdateRunning = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Полная остановка при уничтожении активности
        cancelTasks();
    }
}