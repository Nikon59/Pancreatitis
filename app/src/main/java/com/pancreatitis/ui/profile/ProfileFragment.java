package com.pancreatitis.ui.profile;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.pancreatitis.MainActivity;
import com.pancreatitis.R;
import com.pancreatitis.databinding.FragmentProfileBinding;
import com.pancreatitis.models.Doctor;
import com.pancreatitis.models.User;
import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.localstorage.LocalStorageModule;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private Button btn_logout;
    private Button btn_update_training_data;
    private EditText loginValue;
    private View loadingOverlay;
    private OnBackPressedCallback onBackPressedCallback;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Настройка обработки кнопки "Назад"
        try {
            initViews(view);
            setupProfile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        setupBackButtonHandling();

        return view;
    }

    private void initViews(View view){
        btn_logout = view.findViewById(R.id.btn_logout);
        btn_update_training_data = view.findViewById(R.id.btn_update_training_data);
        loginValue = view.findViewById(R.id.loginValue);
        loadingOverlay = view.findViewById(R.id.loading_overlay);
    }

    private void setupProfile(){
        User user = User.getInstance(); // Создать user с ключом
        loginValue.setText(user.getLogin());
    }

    private void setupBackButtonHandling() {
        // Создаем callback для блокировки кнопки "Назад"
        onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Ничего не делаем - блокируем возврат
                // Можно показать сообщение
                // Toast.makeText(getContext(), "Выйти можно через кнопку 'Выйти'", Toast.LENGTH_SHORT).show();
            }
        };

        // Регистрируем callback
        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), onBackPressedCallback);

        // Кнопка регистрации (всегда доступна)
        btn_logout.setOnClickListener(v -> {
            User.resetInstance();
            Navigation.findNavController(v).navigate(R.id.action_profile_to_auth);
        });

        btn_update_training_data.setOnClickListener(v -> {
            // --- Показываем оверлей и блокируем UI ---
            loadingOverlay.setVisibility(View.VISIBLE);
            ((MainActivity) requireActivity()).lockNavigation();

            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    CloudStorageModule cloud = CloudStorageModule.getInstance();
                    LocalStorageModule local = LocalStorageModule.getInstance();
                    local.setContext(getContext());
                    cloud.setContext(getContext());

                    LocalDateTime cloudTimestamp = cloud.getTrainingDataTimestamp();
                    LocalDateTime localTimestamp = local.getAlgorithmFileDateTime();

                    if (cloudTimestamp == null || localTimestamp == null) {
                        requireActivity().runOnUiThread(() -> {
                            // --- Скрываем оверлей и разблокируем ---
                            loadingOverlay.setVisibility(View.GONE);
                            ((MainActivity) requireActivity()).unlockNavigation();

                            new AlertDialog.Builder(requireContext())
                                    .setTitle("Ошибка")
                                    .setMessage("Не удалось получить временные метки.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        });
                        return;
                    }

                    if (cloudTimestamp.equals(localTimestamp)) {
                        requireActivity().runOnUiThread(() -> {
                            // --- Скрываем оверлей и разблокируем ---
                            loadingOverlay.setVisibility(View.GONE);
                            ((MainActivity) requireActivity()).unlockNavigation();

                            new AlertDialog.Builder(requireContext())
                                    .setTitle("Проверка данных")
                                    .setMessage("Версия актуальна.\n" + localTimestamp)
                                    .setPositiveButton("OK", null)
                                    .show();
                        });
                    } else {
                        requireActivity().runOnUiThread(() -> {
                            loadingOverlay.setVisibility(View.GONE);
                            ((MainActivity) requireActivity()).unlockNavigation();
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("Доступна новая версия")
                                    .setMessage("Облако: " + cloudTimestamp + "\nЛокально: " + localTimestamp + "\n\nЗагрузить обновление?")
                                    .setPositiveButton("Загрузить", (dialog, which) -> {
                                        loadingOverlay.setVisibility(View.VISIBLE);
                                        ((MainActivity) requireActivity()).lockNavigation();
                                        Executors.newSingleThreadExecutor().execute(() -> {
                                            try {
                                                byte[] algoData = cloud.downloadTrainingData();
                                                byte[] signature = cloud.downloadTrainingDataSignature();
                                                boolean isValid = cloud.verifyTrainingDataSignature(algoData, signature);
                                                if (!isValid) {
                                                    requireActivity().runOnUiThread(() -> {
                                                        loadingOverlay.setVisibility(View.GONE);
                                                        ((MainActivity) requireActivity()).unlockNavigation();

                                                        new AlertDialog.Builder(requireContext())
                                                                .setTitle("Ошибка")
                                                                .setMessage("Неверная цифровая подпись. Обновление отменено.")
                                                                .setPositiveButton("OK", null)
                                                                .show();
                                                    });
                                                    return;
                                                }
                                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
                                                String fileName = String.format("algorithm_%s.txt", cloudTimestamp.format(formatter));
                                                local.downloadAlgorithmFile(algoData, fileName);
                                                requireActivity().runOnUiThread(() -> {
                                                    loadingOverlay.setVisibility(View.GONE);
                                                    ((MainActivity) requireActivity()).unlockNavigation();

                                                    new AlertDialog.Builder(requireContext())
                                                            .setTitle("Успех")
                                                            .setMessage("Данные обучения успешно обновлены.")
                                                            .setPositiveButton("OK", null)
                                                            .show();
                                                });
                                            } catch (Exception e) {
                                                requireActivity().runOnUiThread(() -> {
                                                    loadingOverlay.setVisibility(View.GONE);
                                                    ((MainActivity) requireActivity()).unlockNavigation();

                                                    new AlertDialog.Builder(requireContext())
                                                            .setTitle("Ошибка загрузки")
                                                            .setMessage(e.getMessage())
                                                            .setPositiveButton("OK", null)
                                                            .show();
                                                });
                                            }
                                        });
                                    })
                                    .setNegativeButton("Отмена", (dialog, which) -> {
                                        // Пользователь отказался от загрузки – убираем оверлей
                                        loadingOverlay.setVisibility(View.GONE);
                                        ((MainActivity) requireActivity()).unlockNavigation();
                                    })
                                    .show();
                        });
                    }
                } catch (Exception e) {
                    requireActivity().runOnUiThread(() -> {
                        loadingOverlay.setVisibility(View.GONE);
                        ((MainActivity) requireActivity()).unlockNavigation();

                        new AlertDialog.Builder(requireContext())
                                .setTitle("Ошибка")
                                .setMessage(e.getMessage())
                                .setPositiveButton("OK", null)
                                .show();
                    });
                }
            });
        });
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;

        if (onBackPressedCallback != null) {
            onBackPressedCallback.remove();
        }
    }
}