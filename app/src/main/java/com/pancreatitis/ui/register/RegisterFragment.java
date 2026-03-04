package com.pancreatitis.ui.register;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.pancreatitis.MainActivity;
import com.pancreatitis.R;
import com.pancreatitis.databinding.FragmentProfileBinding;
import com.pancreatitis.models.RegistrationForm;
import com.pancreatitis.modules.registration.RegistrationModule;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class RegisterFragment extends Fragment {
    private FragmentProfileBinding binding;
    private EditText fioEditText;
    private EditText emailEditText;
    private EditText phoneEditText;
    private EditText loginEditText;
    private EditText passwordEditText;
    private View loading_overlay;
    private RegistrationModule registrationModule;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        // Инициализация полей ввода
        initViews(view);

        // Настройка кнопки отправки
        setupSubmitButton(view);

        return view;
    }

    private void initViews(View view) {
        fioEditText = view.findViewById(R.id.fioValue);
        emailEditText = view.findViewById(R.id.emailValue);
        phoneEditText = view.findViewById(R.id.phoneValue);
        loginEditText = view.findViewById(R.id.loginValue);
        passwordEditText = view.findViewById(R.id.passwordValue);
        loading_overlay = view.findViewById(R.id.loading_overlay);
        registrationModule = RegistrationModule.getInstance();
    }

    private void setupSubmitButton(View view) {
        Button submitButton = view.findViewById(R.id.btn_submit);
        submitButton.setOnClickListener(v -> {
            processRegistration();
        });
    }

    private void processRegistration() {
        // Показываем загрузку и блокируем UI
        loading_overlay.setVisibility(View.VISIBLE);
        ((MainActivity) requireActivity()).lockNavigation();

        // Выполняем в фоне
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                String message;
                boolean isSuccess = false;

                try {
                    // Получаем данные из полей
                    String fio = fioEditText.getText().toString().trim();
                    String email = emailEditText.getText().toString().trim();
                    String phone = phoneEditText.getText().toString().trim();
                    String login = loginEditText.getText().toString().trim();
                    String password = passwordEditText.getText().toString().trim();

                    // Проверка заполненности полей
                    if (fio.isEmpty() || email.isEmpty() || phone.isEmpty() ||
                            login.isEmpty() || password.isEmpty()) {
                        message = "Заполните все поля";
                    }
//                    else if (!isValidEmail(email)) {
//                        message = "Введите корректный email";
//                    }
//                    else if (!isValidPhone(phone)) {
//                        message = "Введите корректный номер телефона";
//                    }
                    else if (password.length() < 6) {
                        message = "Пароль должен содержать не менее 6 символов";
                    } else {
                        // Создаем объект формы регистрации
                        RegistrationForm registrationForm = new RegistrationForm(
                                login, password, fio, email, phone
                        );

                        boolean result = registrationModule.submitRegistrationRequest(registrationForm);

                        if (result) {
                            message = "OK"; // специальный маркер успеха
                            isSuccess = true;
                        } else {
                            message = "Ошибка регистрации";
                        }
                    }
                } catch (Exception e) {
                    message = "Ошибка: " + e.getMessage();
                }

                // Возвращаемся в UI-поток
                Handler mainHandler = new Handler(Looper.getMainLooper());
                String finalMessage = message;
                boolean finalIsSuccess = isSuccess;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Скрываем загрузку
                        loading_overlay.setVisibility(View.GONE);
                        ((MainActivity) requireActivity()).unlockNavigation();

                        if (finalIsSuccess) {
                            showMessage("Регистрация успешна");
                            // Можно добавить навигацию на экран авторизации или профиля
                            // Navigation.findNavController(requireView()).navigate(R.id.action_register_to_auth);
                        } else {
                            showMessage(finalMessage);
                        }
                    }
                });
            }
        });
    }

    // Валидация email
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }

    // Валидация телефона (простая проверка)
    private boolean isValidPhone(String phone) {
        // Удаляем все нецифровые символы
        String digitsOnly = phone.replaceAll("[^0-9]", "");
        return digitsOnly.length() >= 10; // Минимум 10 цифр
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void showMessage(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    // Улучшенный метод получения сообщения об ошибке
    private String getErrorMessage(Exception e) {
        if (e == null) {
            return "Неизвестная ошибка";
        }

        String message = e.getMessage();
        if (message == null || message.isEmpty()) {
            message = e.getClass().getSimpleName();
        }

        return message;
    }
}