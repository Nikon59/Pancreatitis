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

import androidx.fragment.app.Fragment;

import com.pancreatitis.MainActivity;
import com.pancreatitis.R;
import com.pancreatitis.models.RegistrationForm;
import com.pancreatitis.modules.registration.RegistrationModule;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class RegisterFragment extends Fragment {

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

        initViews(view);
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
        submitButton.setOnClickListener(v -> processRegistration());
    }

    private void processRegistration() {
        // Блокируем UI и показываем загрузку
        loading_overlay.setVisibility(View.VISIBLE);
        ((MainActivity) requireActivity()).lockNavigation();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String message;
            boolean isSuccess = false;

            try {
                String fio = fioEditText.getText().toString().trim();
                String email = emailEditText.getText().toString().trim();
                String phone = phoneEditText.getText().toString().trim();
                String login = loginEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                // Проверка заполненности
                if (fio.isEmpty() || email.isEmpty() || phone.isEmpty() ||
                        login.isEmpty() || password.isEmpty()) {
                    message = "Заполните все поля";
                }
                // Валидация email
                else if (!isValidEmail(email)) {
                    message = "Введите корректный email";
                }
                // Валидация телефона
                else if (!isValidPhone(phone)) {
                    message = "Введите корректный номер телефона\n(допустимы цифры, +, пробелы, скобки, дефис)";
                }
                // Валидация логина
                else if (!isValidLogin(login)) {
                    message = "Логин должен содержать только латинские буквы, цифры и подчеркивание, длина 3–20 символов";
                }
                // Проверка длины пароля
                else if (password.length() < 6) {
                    message = "Пароль должен содержать не менее 6 символов";
                } else {
                    RegistrationForm form = new RegistrationForm(login, password, fio, email, phone);
                    boolean result = registrationModule.submitRegistrationRequest(form);

                    if (result) {
                        message = "OK";
                        isSuccess = true;
                    } else {
                        message = "Ошибка регистрации";
                    }
                }
            } catch (Exception e) {
                message = "Ошибка: " + e.getMessage();
            }

            // Возврат в UI-поток
            String finalMessage = message;
            boolean finalIsSuccess = isSuccess;
            new Handler(Looper.getMainLooper()).post(() -> {
                loading_overlay.setVisibility(View.GONE);
                ((MainActivity) requireActivity()).unlockNavigation();

                if (finalIsSuccess) {
                    Toast.makeText(getContext(), "Регистрация успешна", Toast.LENGTH_LONG).show();
                    // Можно перенаправить на экран авторизации (раскомментировать при необходимости)
                    // Navigation.findNavController(requireView()).navigate(R.id.action_register_to_auth);
                } else {
                    Toast.makeText(getContext(), finalMessage, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    // ----- Валидация email -----
    private boolean isValidEmail(String email) {
        if (email == null) return false;
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return Pattern.matches(emailRegex, email);
    }

    // ----- Валидация телефона -----
    private boolean isValidPhone(String phone) {
        if (phone == null) return false;
        // Разрешены только цифры, +, пробелы, дефис, круглые скобки
        String allowedSymbolsPattern = "^[\\+0-9\\s\\-\\(\\)]+$";
        if (!Pattern.matches(allowedSymbolsPattern, phone)) return false;

        // Удаляем все нецифровые символы
        String digitsOnly = phone.replaceAll("[^0-9]", "");
        // Длина от 10 до 15 цифр (с учётом кода страны)
        return digitsOnly.length() >= 10 && digitsOnly.length() <= 15;
    }

    // ----- Валидация логина -----
    private boolean isValidLogin(String login) {
        if (login == null) return false;
        // Латинские буквы, цифры, подчёркивание, длина от 3 до 20
        String loginPattern = "^[a-zA-Z0-9_]{3,20}$";
        return Pattern.matches(loginPattern, login);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // освобождаем ресурсы (если есть)
    }
}