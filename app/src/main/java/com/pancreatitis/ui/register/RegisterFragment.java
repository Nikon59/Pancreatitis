package com.pancreatitis.ui.register;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.pancreatitis.R;
import com.pancreatitis.databinding.FragmentProfileBinding;
import com.pancreatitis.models.RegistrationForm;
import com.pancreatitis.modules.registration.RegistrationModule;

import java.util.regex.Pattern;

public class RegisterFragment extends Fragment {
    private FragmentProfileBinding binding;
    private EditText fioEditText;
    private EditText emailEditText;
    private EditText phoneEditText;
    private EditText loginEditText;
    private EditText passwordEditText;
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
        registrationModule = RegistrationModule.getInstance();
    }

    private void setupSubmitButton(View view) {
        Button submitButton = view.findViewById(R.id.btn_submit);
        submitButton.setOnClickListener(v -> {
            processRegistration();
        });
    }

    private void processRegistration() {
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
                showMessage("Заполните все поля");
                return;
            }

            // Валидация данных
            if (!isValidEmail(email)) {
                showMessage("Введите корректный email");
                return;
            }

            if (!isValidPhone(phone)) {
                showMessage("Введите корректный номер телефона");
                return;
            }

            if (password.length() < 6) {
                showMessage("Пароль должен содержать не менее 6 символов");
                return;
            }

            // Создаем объект формы регистрации
            RegistrationForm registrationForm = new RegistrationForm(
                    login, password, fio, email, phone
            );

            Boolean result = registrationModule.submitRegistrationRequest(registrationForm);
            showMessage(result.toString());

        } catch (Exception e) {
            showMessage("Ошибка: " + getErrorMessage(e));
        }
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