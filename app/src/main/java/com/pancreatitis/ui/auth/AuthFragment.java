package com.pancreatitis.ui.auth;

import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.pancreatitis.R;
import com.pancreatitis.databinding.FragmentProfileBinding;
import com.pancreatitis.models.Doctor;
import com.pancreatitis.models.RegistrationForm;
import com.pancreatitis.models.User;
import com.pancreatitis.modules.authorization.AuthorizationModule;
import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.safety.SafetyModule;

import java.util.Objects;

import javax.crypto.SecretKey;

public class AuthFragment extends Fragment {
    private FragmentProfileBinding binding;
    private EditText loginEditText;
    private EditText passwordEditText;
    private AuthorizationModule authorizationModule;
    private CloudStorageModule cloudStorageModule;
    private DatabaseModule databaseModule;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_auth, container, false);

        // Инициализация элементов
        loginEditText = view.findViewById(R.id.loginEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        authorizationModule = AuthorizationModule.getInstance();
        cloudStorageModule = CloudStorageModule.getInstance();
        databaseModule = DatabaseModule.getInstance();

        // Кнопка регистрации (всегда доступна)
        TextView goToNestedButton = view.findViewById(R.id.btn_register);
        goToNestedButton.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_auth_to_register);
        });

        // Кнопка входа (только при успешной авторизации)
        Button btn_login = view.findViewById(R.id.btn_login);
        btn_login.setOnClickListener(v -> {
            attemptLogin();
        });

        return view;
    }

    private void attemptLogin() {
        try {
            String login = loginEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            // Проверка на пустые поля
            if (login.isEmpty() || password.isEmpty()) {
                showMessage("Введите логин и пароль");
                return;
            }

            // Авторизация
            SecretKey key = authorizationModule.authenticateUser(login, password);

            if (key != null) {
                NavController navController = Navigation.findNavController(requireView());

                User user_info = cloudStorageModule.downloadUserInfo(login);
                User user = User.getInstance();

                Doctor doctor = databaseModule.getDoctorByFio(user_info.getFullName());
                if (doctor == null) {
                    doctor = new Doctor(user_info.getFullName());
                    databaseModule.insertDoctor(doctor);
                    doctor = databaseModule.getDoctorByFio(user_info.getFullName());
                }
                
                user.setKey(key);
                user.setEmail(user_info.getEmail());
                user.setPhone(user_info.getPhone());
                user.setDoctor(doctor);
                user.setLogin(login);
                user.setPassword(password);

                navController.navigate(R.id.action_auth_to_profile);
            } else {
                // Неверные данные
                showMessage("Неверный логин или пароль");
            }
        } catch (Exception e) {
            showMessage("Ошибка: " + e.getMessage());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void showMessage(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}