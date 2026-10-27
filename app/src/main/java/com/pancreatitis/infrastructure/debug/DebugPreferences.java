package com.pancreatitis.infrastructure.debug;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public final class DebugPreferences {

    private static final String PREF_NAME = "pancreatitis_debug_diagnostics_prefs";

    private static final String KEY_MOCK_AUTH = "debug_mock_auth";
    private static final String KEY_FORCE_OFFLINE = "debug_force_offline";
    private static final String KEY_ACTIVE_MODEL_ID = "debug_active_model_id";
    private static final String KEY_CHECK_SHA256 = "debug_check_sha256";
    private static final String KEY_ZSCORE_NORMALIZATION = "debug_zscore_normalization";
    private static final String KEY_PHYSIOLOGICAL_CLAMP = "debug_physiological_clamp";
    private static final String KEY_DEFAULT_FALLBACK = "debug_default_fallback";
    private static final String KEY_SIMULATE_INFERENCE_ERROR = "debug_simulate_inference_error";

    private static volatile DebugPreferences instance;
    private final SharedPreferences prefs;

    private DebugPreferences(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static DebugPreferences getInstance(Context context) {
        if (instance == null) {
            synchronized (DebugPreferences.class) {
                if (instance == null) {
                    instance = new DebugPreferences(context);
                }
            }
        }
        return instance;
    }

    public boolean isMockAuthEnabled(boolean defaultValue) {
        return prefs.getBoolean(KEY_MOCK_AUTH, defaultValue);
    }

    public void setMockAuthEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_MOCK_AUTH, enabled).apply();
    }

    public boolean isForceOffline() {
        return prefs.getBoolean(KEY_FORCE_OFFLINE, false);
    }

    public void setForceOffline(boolean forceOffline) {
        prefs.edit().putBoolean(KEY_FORCE_OFFLINE, forceOffline).apply();
    }

    public String getActiveModelId(String defaultModelId) {
        return prefs.getString(KEY_ACTIVE_MODEL_ID, defaultModelId);
    }

    public void setActiveModelId(String modelId) {
        prefs.edit().putString(KEY_ACTIVE_MODEL_ID, modelId).apply();
    }

    public boolean isCheckSha256Enabled() {
        return prefs.getBoolean(KEY_CHECK_SHA256, true);
    }

    public void setCheckSha256Enabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_CHECK_SHA256, enabled).apply();
    }

    public boolean isZScoreNormalizationEnabled() {
        return prefs.getBoolean(KEY_ZSCORE_NORMALIZATION, true);
    }

    public void setZScoreNormalizationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ZSCORE_NORMALIZATION, enabled).apply();
    }

    public boolean isPhysiologicalClampEnabled() {
        return prefs.getBoolean(KEY_PHYSIOLOGICAL_CLAMP, true);
    }

    public void setPhysiologicalClampEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_PHYSIOLOGICAL_CLAMP, enabled).apply();
    }

    public boolean isDefaultFallbackEnabled() {
        return prefs.getBoolean(KEY_DEFAULT_FALLBACK, true);
    }

    public void setDefaultFallbackEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_DEFAULT_FALLBACK, enabled).apply();
    }

    public boolean isSimulateInferenceError() {
        return prefs.getBoolean(KEY_SIMULATE_INFERENCE_ERROR, false);
    }

    public void setSimulateInferenceError(boolean simulate) {
        prefs.edit().putBoolean(KEY_SIMULATE_INFERENCE_ERROR, simulate).apply();
    }

    public boolean isAnyDebugOverrideActive() {
        if (isMockAuthEnabled(true)) return true;
        if (isForceOffline()) return true;
        if (!isCheckSha256Enabled()) return true;
        if (!isZScoreNormalizationEnabled()) return true;
        if (!isPhysiologicalClampEnabled()) return true;
        if (!isDefaultFallbackEnabled()) return true;
        if (isSimulateInferenceError()) return true;
        return false;
    }

    public List<String> getActiveOverridesList() {
        List<String> list = new ArrayList<>();
        if (isMockAuthEnabled(true)) {
            list.add("Тестовая авторизация");
        }
        if (isForceOffline()) {
            list.add("Принудительный офлайн");
        }
        if (!isCheckSha256Enabled()) {
            list.add("Отключена проверка SHA-256");
        }
        if (!isZScoreNormalizationEnabled()) {
            list.add("Отключена нормализация");
        }
        if (!isPhysiologicalClampEnabled()) {
            list.add("Отключен диапазон [min, max]");
        }
        if (!isDefaultFallbackEnabled()) {
            list.add("Отключена подстановка default");
        }
        if (isSimulateInferenceError()) {
            list.add("Симуляция ошибки инференса");
        }
        return list;
    }

    public void resetAllToReleaseDefaults() {
        prefs.edit()
                .putBoolean(KEY_MOCK_AUTH, false)
                .putBoolean(KEY_FORCE_OFFLINE, false)
                .putBoolean(KEY_CHECK_SHA256, true)
                .putBoolean(KEY_ZSCORE_NORMALIZATION, true)
                .putBoolean(KEY_PHYSIOLOGICAL_CLAMP, true)
                .putBoolean(KEY_DEFAULT_FALLBACK, true)
                .putBoolean(KEY_SIMULATE_INFERENCE_ERROR, false)
                .apply();
    }
}
