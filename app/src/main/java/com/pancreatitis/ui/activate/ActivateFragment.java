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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.pancreatitis.MainActivity;
import com.pancreatitis.R;
import com.pancreatitis.databinding.FragmentProfileBinding;
import com.pancreatitis.models.Doctor;
import com.pancreatitis.models.User;
import com.pancreatitis.modules.authorization.AuthorizationModule;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.localstorage.LocalStorageModule;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.SecretKey;

public class ActivateFragment extends Fragment {
    private FragmentProfileBinding binding;
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

        // Кнопка входа (только при успешной авторизации)
        Button btn_login = view.findViewById(R.id.btn_login);


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
