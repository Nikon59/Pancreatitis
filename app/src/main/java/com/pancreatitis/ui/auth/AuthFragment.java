package com.pancreatitis.ui.auth;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import com.pancreatitis.MainActivity;
import com.pancreatitis.R;
import com.pancreatitis.databinding.FragmentAuthBinding;
import com.pancreatitis.models.Doctor;
import com.pancreatitis.models.RegistrationForm;
import com.pancreatitis.models.User;
import com.pancreatitis.modules.authorization.AuthorizationModule;
import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.localstorage.LocalStorageModule;
import com.pancreatitis.modules.safety.SafetyModule;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.SecretKey;

public class AuthFragment extends Fragment {
    private FragmentAuthBinding binding;
    private EditText loginEditText;
    private EditText passwordEditText;
    private View loading_overlay;
    private AuthorizationModule authorizationModule;
    private LocalStorageModule localStorageModule;
    private DatabaseModule databaseModule;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_auth, container, false);

        // Инициализация элементов
        loginEditText = view.findViewById(R.id.loginEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        loading_overlay = view.findViewById(R.id.loading_overlay);
        authorizationModule = AuthorizationModule.getInstance();
        authorizationModule.setContext(getContext());
        localStorageModule = LocalStorageModule.getInstance();
        databaseModule = DatabaseModule.getInstance();

        // Кнопка регистрации (всегда доступна)
        TextView goToNestedButton = view.findViewById(R.id.btn_register);
        goToNestedButton.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_auth_to_register);
        });

        // Кнопка регистрации (всегда доступна)
        TextView goToButton = view.findViewById(R.id.btn_activate);
        goToButton.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_auth_to_activate);
        });

        // Кнопка входа (только при успешной авторизации)
        Button btn_login = view.findViewById(R.id.btn_login);
        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Показываем загрузку и блокируем UI
                loading_overlay.setVisibility(View.VISIBLE);
                ((MainActivity) requireActivity()).lockNavigation();

                // Выполняем в фоне
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        String message;
                        try {
                            String login = loginEditText.getText().toString().trim();
                            String password = passwordEditText.getText().toString().trim();

                            if (login.isEmpty() || password.isEmpty()) {
                                message = "Введите логин и пароль";
                            } else {
                                SecretKey key = authorizationModule.authenticateUser(login, password);
                                if (key != null) {

                                    Doctor doctor = databaseModule.getDoctorByLogin(login);
                                    if (doctor == null) {
                                        doctor = new Doctor();
                                        doctor.setLogin(login);
                                        databaseModule.insertDoctor(doctor);
                                        doctor = databaseModule.getDoctorByLogin(login);
                                    }

                                    User user = User.getInstance(); // Создать user с ключом
                                    user.setLogin(login);
                                    user.setKey(key);
                                    user.setCreatedAt(doctor.getCreatedAt());
                                    user.setId(doctor.getId());

                                    message = "OK"; // специальный маркер успеха
                                } else {
                                    message = "Неверный логин или пароль";
                                }
                            }
                        } catch (final Exception e) {
                            message = "Ошибка: " + e.getMessage();
                        }

                        // Возвращаемся в UI-поток
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        String finalMessage = message;
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                // Скрываем загрузку
                                loading_overlay.setVisibility(View.GONE);
                                ((MainActivity) requireActivity()).unlockNavigation();

                                if ("OK".equals(finalMessage)) {
                                    // Навигация только в UI-потоке
                                    NavController navController = Navigation.findNavController(requireView());
                                    navController.navigate(R.id.action_auth_to_profile);
                                } else {
                                    showMessage(finalMessage);
                                }
                            }
                        });
                    }
                });
            }
        });

        return view;
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