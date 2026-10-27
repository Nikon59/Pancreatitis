package com.pancreatitis.ui.debug;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.pancreatitis.R;
import com.pancreatitis.core.ml.ChecksumVerifier;
import com.pancreatitis.core.ml.OnnxInferenceEngine;
import com.pancreatitis.core.model.ml.FeatureSchema;
import com.pancreatitis.core.model.ml.ModelManifest;
import com.pancreatitis.core.model.ml.ModelMetadata;
import com.pancreatitis.core.model.ml.PredictionResult;
import com.pancreatitis.infrastructure.auth.AuthTestBridge;
import com.pancreatitis.infrastructure.clinical.ClinicalTerminology;
import com.pancreatitis.infrastructure.debug.DebugPreferences;
import com.pancreatitis.infrastructure.ml.ModelAssetManager;
import com.pancreatitis.models.User;
import com.pancreatitis.modules.prediction.PredictionModule;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DebugDiagnosticsActivity extends AppCompatActivity {

    private MaterialSwitch switchMockAuth;
    private MaterialSwitch switchForceOffline;
    private MaterialButton btnResetSession;

    private RadioGroup rgActiveModel;
    private RadioButton rbCatboost;
    private RadioButton rbXgboost;
    private RadioButton rbLightgbm;
    private RadioButton rbRf;
    private RadioButton rbLogreg;

    private MaterialSwitch switchSha256;
    private MaterialSwitch switchNormalization;
    private MaterialSwitch switchClamp;
    private MaterialSwitch switchDefaultFallback;

    private MaterialButton btnRunBenchmark;
    private LinearProgressIndicator progressBenchmark;
    private TableLayout tableBenchmarkResults;

    private MaterialButton btnSimulateError;
    private MaterialButton btnRestoreAssets;

    private DebugPreferences debugPrefs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_diagnostics);

        debugPrefs = DebugPreferences.getInstance(this);

        initViews();
        setupToolbar();
        setupBlock1AuthAndNetwork();
        setupBlock2ModelSelector();
        setupBlock3PipelineToggles();
        setupBlock4Benchmark();
        setupBlock5TensorInspectorAndSimulator();
    }

    private void initViews() {
        switchMockAuth = findViewById(R.id.switch_mock_auth);
        switchForceOffline = findViewById(R.id.switch_force_offline);
        btnResetSession = findViewById(R.id.btn_reset_session);

        rgActiveModel = findViewById(R.id.rg_active_model);
        rbCatboost = findViewById(R.id.rb_model_catboost);
        rbXgboost = findViewById(R.id.rb_model_xgboost);
        rbLightgbm = findViewById(R.id.rb_model_lightgbm);
        rbRf = findViewById(R.id.rb_model_rf);
        rbLogreg = findViewById(R.id.rb_model_logreg);

        switchSha256 = findViewById(R.id.switch_sha256);
        switchNormalization = findViewById(R.id.switch_normalization);
        switchClamp = findViewById(R.id.switch_clamp);
        switchDefaultFallback = findViewById(R.id.switch_default_fallback);

        btnRunBenchmark = findViewById(R.id.btn_run_benchmark);
        progressBenchmark = findViewById(R.id.progress_benchmark);
        tableBenchmarkResults = findViewById(R.id.table_benchmark_results);

        btnSimulateError = findViewById(R.id.btn_simulate_error);
        btnRestoreAssets = findViewById(R.id.btn_restore_assets);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.debug_toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    // ========== БЛОК 1: УПРАВЛЕНИЕ АВТОРИЗАЦИЕЙ И СЕТЬЮ ==========
    private void setupBlock1AuthAndNetwork() {
        boolean isMockAuth = AuthTestBridge.isTestAuthActive(this);
        switchMockAuth.setChecked(isMockAuth);
        switchMockAuth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AuthTestBridge.setTestAuthActive(this, isChecked);
            showSnackbar(isChecked ? "Тестовая авторизация включена" : "Тестовая авторизация отключена");
        });

        switchForceOffline.setChecked(debugPrefs.isForceOffline());
        switchForceOffline.setOnCheckedChangeListener((buttonView, isChecked) -> {
            debugPrefs.setForceOffline(isChecked);
            showSnackbar(isChecked ? "Принудительный офлайн активирован" : "Принудительный офлайн отключен");
        });

        btnResetSession.setOnClickListener(v -> {
            User user = User.getInstance();
            user.setLogin(null);
            user.setFio(null);
            user.setId(0);
            user.setKey(null);
            user.setCreatedAt(null);
            User.resetInstance();
            showSnackbar(getString(R.string.debug_session_reset_success));
        });
    }

    // ========== БЛОК 2: ГЛОБАЛЬНЫЙ ВЫБОР АКТИВНОЙ МОДЕЛИ ==========
    private void setupBlock2ModelSelector() {
        String activeModelId = debugPrefs.getActiveModelId("catboost");
        selectRadioButtonForModelId(activeModelId);

        rgActiveModel.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedId = "catboost";
            if (checkedId == R.id.rb_model_catboost) {
                selectedId = "catboost";
            } else if (checkedId == R.id.rb_model_xgboost) {
                selectedId = "xgboost";
            } else if (checkedId == R.id.rb_model_lightgbm) {
                selectedId = "lightgbm";
            } else if (checkedId == R.id.rb_model_rf) {
                selectedId = "random_forest";
            } else if (checkedId == R.id.rb_model_logreg) {
                selectedId = "logreg";
            }

            debugPrefs.setActiveModelId(selectedId);
            try {
                PredictionModule.getInstance().selectModel(selectedId);
                showSnackbar("Активная модель изменена на: " + selectedId);
            } catch (Exception e) {
                showSnackbar("Ошибка переключения модели: " + e.getMessage());
            }
        });
    }

    private void selectRadioButtonForModelId(String modelId) {
        if ("xgboost".equalsIgnoreCase(modelId)) {
            rbXgboost.setChecked(true);
        } else if ("lightgbm".equalsIgnoreCase(modelId)) {
            rbLightgbm.setChecked(true);
        } else if ("random_forest".equalsIgnoreCase(modelId)) {
            rbRf.setChecked(true);
        } else if ("logreg".equalsIgnoreCase(modelId)) {
            rbLogreg.setChecked(true);
        } else {
            rbCatboost.setChecked(true);
        }
    }

    // ========== БЛОК 3: ИНЖЕНЕРНЫЕ ПЕРЕКЛЮЧАТЕЛИ ПАЙПЛАЙНА ==========
    private void setupBlock3PipelineToggles() {
        switchSha256.setChecked(debugPrefs.isCheckSha256Enabled());
        switchSha256.setOnCheckedChangeListener((buttonView, isChecked) -> {
            debugPrefs.setCheckSha256Enabled(isChecked);
            showSnackbar(isChecked ? "Сверка SHA-256 активна" : "Сверка SHA-256 ОТКЛЮЧЕНА");
        });

        switchNormalization.setChecked(debugPrefs.isZScoreNormalizationEnabled());
        switchNormalization.setOnCheckedChangeListener((buttonView, isChecked) -> {
            debugPrefs.setZScoreNormalizationEnabled(isChecked);
            showSnackbar(isChecked ? "Z-score нормализация активна" : "Z-score нормализация ОТКЛЮЧЕНА");
        });

        switchClamp.setChecked(debugPrefs.isPhysiologicalClampEnabled());
        switchClamp.setOnCheckedChangeListener((buttonView, isChecked) -> {
            debugPrefs.setPhysiologicalClampEnabled(isChecked);
            showSnackbar(isChecked ? "Ограничение признаков [min, max] активно" : "Ограничение [min, max] ОТКЛЮЧЕНО");
        });

        switchDefaultFallback.setChecked(debugPrefs.isDefaultFallbackEnabled());
        switchDefaultFallback.setOnCheckedChangeListener((buttonView, isChecked) -> {
            debugPrefs.setDefaultFallbackEnabled(isChecked);
            showSnackbar(isChecked ? "Подстановка default значений активна" : "Подстановка default значений ОТКЛЮЧЕНА");
        });
    }

    // ========== БЛОК 4: СВОДНЫЙ БЕНЧМАРК И СТРЕСС-ТЕСТ ==========
    private void setupBlock4Benchmark() {
        btnRunBenchmark.setOnClickListener(v -> runBenchmarkStressTest());
    }

    private void runBenchmarkStressTest() {
        btnRunBenchmark.setEnabled(false);
        progressBenchmark.setVisibility(View.VISIBLE);
        tableBenchmarkResults.removeAllViews();
        addBenchmarkTableHeader();

        executor.execute(() -> {
            PredictionModule pm = PredictionModule.getInstance();
            pm.setContext(getApplicationContext());
            ModelManifest manifest = pm.getManifest();
            String originalActiveId = pm.getActiveModelId();

            Map<String, String> modelMap = new LinkedHashMap<>();
            modelMap.put("catboost", "CatBoost");
            modelMap.put("xgboost", "XGBoost");
            modelMap.put("lightgbm", "LightGBM");
            modelMap.put("random_forest", "Random Forest");
            modelMap.put("logreg", "Logistic Regression");

            Map<String, Float> baselineFeatures = buildSyntheticBaselineVector(manifest);

            File modelsDir = new File(getFilesDir(), "models");

            for (Map.Entry<String, String> entry : modelMap.entrySet()) {
                String modelId = entry.getKey();
                String displayName = entry.getValue();

                String weightsStatus;
                String shaStatus;
                String latencyStr;
                String probsStr;

                ModelMetadata meta = (manifest != null) ? manifest.getModel(modelId) : null;
                File modelFile = (meta != null) ? new File(modelsDir, meta.getFileName()) : null;

                if (modelFile != null && modelFile.exists()) {
                    weightsStatus = "Загружен";
                    boolean shaValid = false;
                    try {
                        shaValid = ChecksumVerifier.verifyFileSha256(modelFile.toPath(), meta.getSha256());
                    } catch (Exception e) {
                        shaValid = false;
                    }
                    shaStatus = shaValid ? "Совпадает" : "Нарушена";
                } else {
                    weightsStatus = "Отсутствует";
                    shaStatus = "Файл не найден";
                }

                try {
                    OnnxInferenceEngine engine = pm.getInferenceEngine();
                    if (engine != null) {
                        engine.loadModel(modelId);

                        long startNs = System.nanoTime();
                        PredictionResult result = engine.predict(baselineFeatures);
                        long endNs = System.nanoTime();

                        float latencyMs = (endNs - startNs) / 1_000_000.0f;
                        latencyStr = String.format(ClinicalTerminology.RU_LOCALE, "%.2f мс", latencyMs);

                        float p0 = result.getProbabilityForClass(0) * 100.0f;
                        float p1 = result.getProbabilityForClass(1) * 100.0f;
                        probsStr = String.format(ClinicalTerminology.RU_LOCALE, "%.1f%% / %.1f%%", p0, p1);
                    } else {
                        latencyStr = "N/A";
                        probsStr = "N/A";
                    }
                } catch (Exception e) {
                    latencyStr = "Ошибка";
                    probsStr = "Сбой: " + e.getClass().getSimpleName();
                }

                final String finalWeights = weightsStatus;
                final String finalSha = shaStatus;
                final String finalLatency = latencyStr;
                final String finalProbs = probsStr;

                mainHandler.post(() -> addBenchmarkTableRow(displayName, finalWeights, finalSha, finalLatency, finalProbs));
            }

            // Восстанавливаем оригинальную активную модель
            if (originalActiveId != null) {
                try {
                    pm.selectModel(originalActiveId);
                } catch (Exception ignored) {
                }
            }

            mainHandler.post(() -> {
                progressBenchmark.setVisibility(View.GONE);
                btnRunBenchmark.setEnabled(true);
                showSnackbar("Сравнительный тест всех 5 моделей успешно завершён");
            });
        });
    }

    private void addBenchmarkTableHeader() {
        TableRow row = new TableRow(this);
        row.setPadding(8, 8, 8, 8);
        row.setBackgroundColor(getColor(R.color.md_theme_light_surfaceVariant));

        row.addView(createCellTextView("Модель", true));
        row.addView(createCellTextView("Веса", true));
        row.addView(createCellTextView("SHA-256", true));
        row.addView(createCellTextView("Латентность", true));
        row.addView(createCellTextView("Вероятности (0 / 1)", true));

        tableBenchmarkResults.addView(row);
    }

    private void addBenchmarkTableRow(String model, String weights, String sha, String latency, String probs) {
        TableRow row = new TableRow(this);
        row.setPadding(8, 8, 8, 8);

        row.addView(createCellTextView(model, false));
        row.addView(createCellTextView(weights, false));
        row.addView(createCellTextView(sha, false));
        row.addView(createCellTextView(latency, false));
        row.addView(createCellTextView(probs, false));

        tableBenchmarkResults.addView(row);
    }

    private TextView createCellTextView(String text, boolean isHeader) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(12, 8, 12, 8);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        if (isHeader) {
            tv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall);
            tv.setTextColor(getColor(R.color.text_primary));
        } else {
            tv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
            tv.setTextColor(getColor(R.color.text_secondary));
        }
        return tv;
    }

    private Map<String, Float> buildSyntheticBaselineVector(ModelManifest manifest) {
        Map<String, Float> map = new HashMap<>();
        if (manifest != null && manifest.getFeatures() != null) {
            for (FeatureSchema schema : manifest.getFeatures()) {
                map.put(schema.getKey(), schema.getDefaultValue());
            }
        }
        // Задаем клинически значимый профиль острого панкреатита
        map.put("sex", 1.0f);
        map.put("age", 54.0f);
        map.put("time disease", 14.0f);
        map.put("t", 37.9f);
        map.put("ps", 98.0f);
        map.put("chdd", 22.0f);
        map.put("er", 4.3f);
        map.put("hb", 136.0f);
        map.put("l", 14.8f);
        map.put("lii", 3.9f);
        map.put("coe", 26.0f);
        map.put("glu", 7.4f);
        map.put("ls/cen gid", 120.0f);
        return map;
    }

    // ========== БЛОК 5: ИНСПЕКТОР ТЕНЗОРОВ И СИМУЛЯТОР ОШИБОК ==========
    private void setupBlock5TensorInspectorAndSimulator() {
        btnSimulateError.setOnClickListener(v -> {
            debugPrefs.setSimulateInferenceError(true);
            showSnackbar(getString(R.string.debug_inference_failed_simulated));
        });

        btnRestoreAssets.setOnClickListener(v -> {
            btnRestoreAssets.setEnabled(false);
            executor.execute(() -> {
                try {
                    ModelAssetManager.forceReExtract(getApplicationContext());
                    PredictionModule pm = PredictionModule.getInstance();
                    pm.selectModel(debugPrefs.getActiveModelId("catboost"));

                    mainHandler.post(() -> {
                        btnRestoreAssets.setEnabled(true);
                        showSnackbar(getString(R.string.debug_models_restored_success));
                    });
                } catch (Exception e) {
                    mainHandler.post(() -> {
                        btnRestoreAssets.setEnabled(true);
                        showSnackbar("Ошибка восстановления: " + e.getMessage());
                    });
                }
            });
        });
    }

    private void showSnackbar(String message) {
        View root = findViewById(android.R.id.content);
        if (root != null) {
            Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
