package com.pancreatitis;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.localstorage.LocalStorageModule;
import com.pancreatitis.modules.prediction.PredictionModule;

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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private NavController navController;
    private ImageView backButton;
    private TextView toolbarTitle;
    private Toolbar toolbar; // Добавляем ссылку на весь тулбар

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DatabaseModule databaseModule = DatabaseModule.getInstance();
        databaseModule.setContext(getApplicationContext());
        databaseModule.initWork();

        checkAndDownloadAlgorithmFile();

        // Убираем ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initViews();
        setupNavigation();
        setupToolbar();
        setupBackPressedCallback();
    }

    private void checkAndDownloadAlgorithmFile() {
        new Thread(() -> {
            try {
                downloadAlgorithmFileToAssets();
            } catch (Exception e) {
                Log.e("MainActivity", "Error checking algorithm file", e);
            }
        }).start();
    }

    private void downloadAlgorithmFileToAssets() {
        try {
            CloudStorageModule cloudStorageModule = CloudStorageModule.getInstance();
            LocalStorageModule localStorageModule = LocalStorageModule.getInstance();
            localStorageModule.setContext(getBaseContext());

            // Скачиваем файл из облака
            byte[] fileData = cloudStorageModule.downloadTrainingData();

            // Сохраняем файл во внутреннюю память приложения
            localStorageModule.downloadAlgorithmFile(fileData);

            Log.d("MainActivity", "algorithm.txt downloaded and saved successfully");
        } catch (Exception e) {
            Log.e("MainActivity", "Failed to download algorithm.txt", e);
        }
    }

    private void setupBackPressedCallback() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (navController.getCurrentDestination() != null &&
                        navController.getCurrentDestination().getId() != R.id.navigation_home &&
                        navController.getCurrentDestination().getId() != R.id.navigation_auth &&
                        navController.getCurrentDestination().getId() != R.id.navigation_helps) {
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
        toolbar = findViewById(R.id.toolbar);
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
        // Определяем, на каких фрагментах скрывать тулбар
        Set<Integer> hiddenToolbarFragments = new HashSet<>(Arrays.asList(
                R.id.navigation_auth,
                R.id.navigation_profile
        ));

        // Скрываем или показываем весь тулбар
        if (toolbar != null) {
            boolean showToolbar = !hiddenToolbarFragments.contains(destinationId);
            toolbar.setVisibility(showToolbar ? View.VISIBLE : View.GONE);
        }

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
            return "Помощь";}
        else if (destinationId == R.id.navigation_auth) {
            return "Аут";
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

    // Дополнительные методы для управления тулбаром из фрагментов (опционально)
    public void setToolbarVisibility(boolean visible) {
        if (toolbar != null) {
            toolbar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    public void showToolbar() {
        setToolbarVisibility(true);
    }

    public void hideToolbar() {
        setToolbarVisibility(false);
    }
}