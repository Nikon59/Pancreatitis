package com.pancreatitis.ui.profile;

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

import com.pancreatitis.R;
import com.pancreatitis.databinding.FragmentProfileBinding;
import com.pancreatitis.models.Doctor;
import com.pancreatitis.models.User;
import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.database.DatabaseModule;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private Button btn_logout;
    private TextView title_fio;
    private EditText fioValue;
    private EditText emailValue;
    private EditText phoneValue;
    private EditText loginValue;
    private EditText passwordValue;
    private OnBackPressedCallback onBackPressedCallback;
    private CloudStorageModule cloudStorageModule;
    private DatabaseModule databaseModule;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Настройка обработки кнопки "Назад"
        try {
            initViews(view);
            loadrofile();
            setupProfile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        setupBackButtonHandling();

        return view;
    }

    private void initViews(View view){
        btn_logout = view.findViewById(R.id.btn_logout);
        title_fio = view.findViewById(R.id.title_fio);
        fioValue = view.findViewById(R.id.fioValue);
        emailValue = view.findViewById(R.id.emailValue);
        phoneValue = view.findViewById(R.id.phoneValue);
        loginValue = view.findViewById(R.id.loginValue);
        passwordValue = view.findViewById(R.id.passwordValue);
        cloudStorageModule = CloudStorageModule.getInstance();
        databaseModule = DatabaseModule.getInstance();
    }

    private Pair<String, String> receiveArguments() {
        Bundle args = getArguments();
        String login = args.getString("login");
        String password = args.getString("password");
        return new Pair<>(login, password);
    }

    private void loadrofile() throws Exception {
        Pair<String, String> pair = receiveArguments();
        String login = pair.first, password = pair.second;

        try {
            User user_info = cloudStorageModule.downloadUserInfo(login);
            Doctor doctor = databaseModule.getDoctorByFio(user_info.getFullName());
            if (doctor == null) {
                doctor = new Doctor(user_info.getFullName());
                databaseModule.insertDoctor(doctor);
            }
            doctor = databaseModule.getDoctorByFio(user_info.getFullName());

            User user = User.getInstance();
            user.setEmail(user_info.getEmail());
            user.setPhone(user_info.getPhone());
            user.setDoctor(doctor);
            user.setLogin(login);
            user.setPassword(password);
            user.setKey(user_info.getKey());
        } catch (Exception e) {
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupProfile(){
        User user = User.getInstance();
        fioValue.setText(user.getFullName());
        title_fio.setText(user.getFullName());
        emailValue.setText(user.getEmail());
        phoneValue.setText(user.getPhone());
        loginValue.setText(user.getLogin());
        passwordValue.setText(user.getPassword());
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
            Navigation.findNavController(v).navigate(R.id.action_profile_to_auth);
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