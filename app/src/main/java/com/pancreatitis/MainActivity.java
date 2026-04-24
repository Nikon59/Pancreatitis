package com.pancreatitis;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.pancreatitis.models.Patient;
import com.pancreatitis.models.Questionnaire;
import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.localstorage.LocalStorageModule;
import com.pancreatitis.modules.prediction.PredictionModule;
import com.pancreatitis.modules.questionnairemanager.QuestionnaireManagerModule;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private NavController navController;
    private ImageView backButton;
    private TextView toolbarTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DatabaseModule databaseModule = DatabaseModule.getInstance();
        databaseModule.setContext(getApplicationContext());
        databaseModule.initWork();

        copyFromAsstesAlgorithmFile();

        // Убираем ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initViews();
        setupNavigation();
        setupToolbar();
        setupBackPressedCallback();
    }

    private void copyFromAsstesAlgorithmFile() {
        new Thread(() -> {
            try {
                LocalStorageModule localStorage = LocalStorageModule.getInstance();
                localStorage.setContext(getBaseContext());
                localStorage.copyAlgorithmFromAssets("algorithm_2026_04_24_10_45_18.txt");
            } catch (Exception e) {
                Log.e("MainActivity", "Error checking algorithm file", e);
            }
        }).start();
    }

    private void setupBackPressedCallback() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                int currentDestId = navController.getCurrentDestination().getId();

                // Проверяем, нужно ли обработать нажатие особым образом
                if (currentDestId == R.id.navigation_anket) {
                    QuestionnaireManagerModule questionnaireManagerModule = QuestionnaireManagerModule.getInstance();
                    questionnaireManagerModule.setPatient(new Patient());
                    questionnaireManagerModule.setQuestionnaire(new Questionnaire());
                    questionnaireManagerModule.setCharacterizationAnketPatients(new ArrayList<>());
                    return;  // не продолжаем стандартную обработку
                }

                // Для других экранов — стандартная логика
                Set<Integer> mainScreens = new HashSet<>(Arrays.asList(
                        R.id.navigation_home,
                        R.id.navigation_auth,
                        R.id.navigation_helps
                ));

                if (!mainScreens.contains(currentDestId)) {
                    navController.navigateUp();
                } else {
                    finish();
                }
            }
        });
    }

    private void initViews() {
        backButton = findViewById(R.id.btn_back);
        toolbarTitle = findViewById(R.id.toolbar_title);
    }

    private void setupNavigation() {
        try {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);

            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();

                BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
                if (bottomNavigation != null) {
                    // Простая настройка без сложной логики
                    NavigationUI.setupWithNavController(bottomNavigation, navController);
                }

                // Упрощенный слушатель для тулбара
                navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                    updateToolbar(destination.getId());
                });
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Navigation setup error", e);
        }
    }

    private void setupToolbar() {
        // Проверяем что элементы не null перед использованием
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                if (navController != null) {
                    navController.navigateUp();
                }
            });
        }
    }

    private void updateToolbar(int destinationId) {
        if (backButton != null) {
            Set<Integer> hiddenBackButtonFragments = new HashSet<>(Arrays.asList(
                    R.id.navigation_home,
                    R.id.navigation_auth,
                    R.id.navigation_profile,
                    R.id.navigation_helps
            ));

            boolean showBackButton = !hiddenBackButtonFragments.contains(destinationId);
            backButton.setVisibility(showBackButton ? View.VISIBLE : View.GONE);
        }

        if (toolbarTitle != null) {
            String title = getFragmentTitle(destinationId);
            toolbarTitle.setText(title);
        }
    }

    private String getFragmentTitle(int destinationId) {
        if (destinationId == R.id.navigation_home) {
            return "Добро пожаловать";
        } else if (destinationId == R.id.navigation_helps) {
            return "Помощь";
        } else if (destinationId == R.id.navigation_profile) {
            return "Личный кабинет";
        } else if (destinationId == R.id.navigation_questionnaires) {
            return "Менеджер анкет";
        } else if (destinationId == R.id.navigation_anket) {
            return "Анкета";
        } else {
            return "";
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public void lockNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        Toolbar toolbar = findViewById(R.id.toolbar);
        bottomNavigationView.setVisibility(View.GONE);
        toolbar.setVisibility(View.GONE);
    }

    public void unlockNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        Toolbar toolbar = findViewById(R.id.toolbar);
        bottomNavigationView.setVisibility(View.VISIBLE);
        toolbar.setVisibility(View.VISIBLE);
    }
}