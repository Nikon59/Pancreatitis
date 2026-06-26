package com.pancreatitis;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.pancreatitis.models.Patient;
import com.pancreatitis.models.Questionnaire;
import com.pancreatitis.models.User;
import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.localstorage.LocalStorageModule;
import com.pancreatitis.modules.prediction.PredictionModule;
import com.pancreatitis.modules.questionnairemanager.QuestionnaireManagerModule;
import com.pancreatitis.modules.safety.SafetyModule;
import com.pancreatitis.ui.profile.ProfileFragment;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.crypto.SecretKey;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private NavController navController;
    private ImageView backButton;
    private TextView toolbarTitle;

    // Для периодической проверки
    private Handler periodicHandler;
    private Runnable periodicTask;
    private SharedPreferences prefs;
    private static final long CHECK_INTERVAL_MS = 2 * 60 * 1000; // 2 минут
    private static final long UPLOAD_PERIOD_MS = 24 * 60 * 60 * 1000; // 24 часа
    private static final String PREF_LAST_ARCHIVE_UPLOAD = "last_archive_upload";
    private static final String PREF_REQUEST_SENT = "data_request_sent";
    private static final String PREF_PROCESSED_DOWNLOAD_FILES = "processed_download_files";

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

    @Override
    protected void onResume() {
        super.onResume();
        startPeriodicUploadCheck();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPeriodicUploadCheck();
    }

    private void startPeriodicUploadCheck() {
        resetLastUploadTime();
        if (periodicHandler == null) {
            periodicHandler = new Handler(Looper.getMainLooper());
        }
        if (periodicTask != null) {
            periodicHandler.removeCallbacks(periodicTask);
        }
        periodicTask = () -> {
            // Запускаем проверку в фоновом потоке
            new Thread(() -> {
                checkAndUploadArchive();   // существующая отправка
                checkAndDownloadData(); // проверка входящих
            }).start();
            // Планируем следующий запуск
            periodicHandler.postDelayed(periodicTask, CHECK_INTERVAL_MS);
        };
        periodicHandler.post(periodicTask);
    }

    private void stopPeriodicUploadCheck() {
        if (periodicHandler != null && periodicTask != null) {
            periodicHandler.removeCallbacks(periodicTask);
        }
    }

    private String getRequestSentKey() {
        User user = User.getInstance();
        return PREF_REQUEST_SENT + "_" + user.getLogin();
    }

    private String getPrefProcessedDownloadFiles() {
        User user = User.getInstance();
        return PREF_PROCESSED_DOWNLOAD_FILES + "_" + user.getLogin();
    }

    public void resetLastUploadTime() {
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    private void checkAndUploadArchive() {
        User user = User.getInstance();
        if (user == null || user.getLogin() == null || user.getLogin().isEmpty()) {
            Log.d("MainActivity", "User not authorized, skip archive upload");
            return;
        }

        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String lastDateStr = prefs.getString(getLastArchiveUploadKey(), null);
        String todayStr = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());

        if (!todayStr.equals(lastDateStr)) {
            String result = QuestionnaireManagerModule.getInstance().uploadAllQuestionnairesAsArchive();
            if (result != null) {
                prefs.edit().putString(getLastArchiveUploadKey(), todayStr).apply();
                runOnUiThread(() -> Toast.makeText(this, result, Toast.LENGTH_SHORT).show());
            } else {
                Log.e("MainActivity", "Archive upload failed");
            }
        } else {
            Log.d("MainActivity", "Today's upload already done, skipping");
        }
    }

    public void forceCheckAndUploadArchive() {
        new Thread(this::checkAndUploadArchive).start();
    }

    public void forceCheckAndDownloadData() {
        new Thread(this::checkAndDownloadData).start();
    }

    public void checkAndDownloadData() {
        User user = User.getInstance();
        if (user == null || user.getLogin() == null || user.getLogin().isEmpty()) {
            Log.d("MainActivity", "User not authorized, skip download check");
            return;
        }

        // Проверяем, был ли отправлен запрос на получение данных для этого пользователя
        boolean requestSent = prefs.getBoolean(getRequestSentKey(), false);
        if (!requestSent) {
            Log.d("MainActivity", "No data request sent for this user, skipping download check");
            return;
        }

        try {
            CloudStorageModule cloud = CloudStorageModule.getInstance();
            cloud.setContext(this);
            List<String> files = cloud.listDownloadFilesForUser(user.getLogin());

            Set<String> processedFiles = prefs.getStringSet(getPrefProcessedDownloadFiles(), new HashSet<>());
            boolean anyImported = false;
            SecretKey key = user.getKey();
            boolean anyFile = false;

            for (String filePath : files) {
                anyFile = true;
                String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
                if (processedFiles.contains(fileName)) continue;

                byte[] encryptedData = cloud.downloadFile(filePath);
                int count = QuestionnaireManagerModule.getInstance().importQuestionnairesFromArchive(encryptedData, key);
                if (count > 0) {
                    anyImported = true;
                    processedFiles.add(fileName);
                    cloud.deleteFile(filePath);
                }
            }

            if (anyFile) {
                if (anyImported) {
                    // Сбрасываем флаг запроса, так как данные получены
                    prefs.edit()
                            .putBoolean(getRequestSentKey(), false)
                            .putStringSet(getPrefProcessedDownloadFiles(), processedFiles)
                            .apply();

                    runOnUiThread(() -> {
                        Toast.makeText(getBaseContext(), "Импортированы новые анкеты", Toast.LENGTH_LONG).show();
                        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                        if (fragment instanceof ProfileFragment) {
                            ((ProfileFragment) fragment).refreshRequestButton();
                        }
                    });
                } else {
                    // Данных нет – обновляем только список обработанных файлов (он не меняется)
                    prefs.edit()
                            .putBoolean(getRequestSentKey(), false)
                            .putStringSet(getPrefProcessedDownloadFiles(), processedFiles)
                            .apply();

                    runOnUiThread(() -> Toast.makeText(getBaseContext(), "Локальные данные актуальны", Toast.LENGTH_LONG).show());
                }
            }
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(getBaseContext(), "Ошибка при импортировании новых анкет", Toast.LENGTH_LONG).show());
            Log.e("MainActivity", "Download check error", e);
        }
    }

    private void copyFromAsstesAlgorithmFile() {
        new Thread(() -> {
            try {
                LocalStorageModule localStorage = LocalStorageModule.getInstance();
                localStorage.setContext(getBaseContext());
                if (!localStorage.isTrainingData()) {
                    localStorage.copyAlgorithmFromAssets("algorithm_2026_04_24_10_45_18.txt");
                }
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

                if (currentDestId == R.id.navigation_anket) {
                    QuestionnaireManagerModule questionnaireManagerModule = QuestionnaireManagerModule.getInstance();
                    questionnaireManagerModule.setPatient(new Patient());
                    questionnaireManagerModule.setQuestionnaire(new Questionnaire());
                    questionnaireManagerModule.setCharacterizationAnketPatients(new ArrayList<>());
                    return;
                }

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

    private String getLastArchiveUploadKey() {
        User user = User.getInstance();
        return PREF_LAST_ARCHIVE_UPLOAD + "_" + user.getLogin();
    }

    private void initViews() {
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
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
                    NavigationUI.setupWithNavController(bottomNavigation, navController);
                }

                navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                    updateToolbar(destination.getId());
                });
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Navigation setup error", e);
        }
    }

    private void setupToolbar() {
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
        } else if (destinationId == R.id.navigation_register) {
            return "Регистрация";
        } else if (destinationId == R.id.navigation_activate) {
            return "Активация";
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