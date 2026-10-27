package com.pancreatitis.modules.prediction;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.pancreatitis.core.ml.InferenceEngine;
import com.pancreatitis.core.ml.OnnxInferenceEngine;
import com.pancreatitis.core.model.ml.FeatureSchema;
import com.pancreatitis.core.model.ml.ManifestParser;
import com.pancreatitis.core.model.ml.ModelManifest;
import com.pancreatitis.core.model.ml.ModelMetadata;
import com.pancreatitis.infrastructure.ml.ModelAssetManager;
import com.pancreatitis.models.Characteristic;
import com.pancreatitis.models.CharasteristicDTO;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PredictionModule {

    private static final String TAG = "PredictionModule";
    private static PredictionModule instance;

    private Context context;
    private OnnxInferenceEngine inferenceEngine;
    private ModelManifest manifest;
    private String activeModelId = "catboost";
    private List<Characteristic> lastLoadedCharacteristics;

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface PredictionCallback {
        void onSuccess(PredictionResult result);
        void onError(Throwable error);
    }

    private PredictionModule() {
        this.lastLoadedCharacteristics = null;
    }

    public static synchronized PredictionModule getInstance() {
        if (instance == null) {
            instance = new PredictionModule();
        }
        return instance;
    }

    public Context getContext() {
        return context;
    }

    public synchronized void setContext(Context context) {
        this.context = (context != null) ? context.getApplicationContext() : null;
        if (this.context != null && this.inferenceEngine == null) {
            backgroundExecutor.execute(() -> {
                try {
                    ensureInitialized();
                } catch (Exception e) {
                    Log.e(TAG, "Background initialization of ML engine failed", e);
                }
            });
        }
    }

    public synchronized void ensureInitialized() throws Exception {
        if (context == null) {
            throw new IllegalStateException("Context not set. Call setContext() first.");
        }
        if (inferenceEngine != null && inferenceEngine.isModelLoaded()) {
            return;
        }

        File modelsDir = ModelAssetManager.ensureModelsExtracted(context);
        File manifestFile = new File(modelsDir, "model_manifest.json");

        if (!manifestFile.exists()) {
            try (InputStream in = context.getAssets().open("models/model_manifest.json")) {
                this.manifest = ManifestParser.parse(in);
            }
        } else {
            this.manifest = ManifestParser.parse(manifestFile.toPath());
        }

        Path modelsDirPath = modelsDir.toPath();
        this.inferenceEngine = new OnnxInferenceEngine(manifest, modelsDirPath);

        List<String> modelIds = inferenceEngine.getAvailableModelIds();
        if (modelIds.contains("catboost")) {
            activeModelId = "catboost";
        } else if (!modelIds.isEmpty()) {
            activeModelId = modelIds.get(0);
        }

        inferenceEngine.loadModel(activeModelId);
        Log.i(TAG, "ML engine initialized successfully with model: " + activeModelId);
    }

    public PredictionResult predict(List<CharasteristicDTO> characteristicDTOs) throws Exception {
        Future<PredictionResult> future = backgroundExecutor.submit(new Callable<PredictionResult>() {
            @Override
            public PredictionResult call() throws Exception {
                return executeInference(characteristicDTOs);
            }
        });
        return future.get();
    }

    public void predictAsync(List<CharasteristicDTO> characteristicDTOs, PredictionCallback callback) {
        backgroundExecutor.execute(() -> {
            try {
                PredictionResult result = executeInference(characteristicDTOs);
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onSuccess(result);
                    }
                });
            } catch (Throwable t) {
                Log.e(TAG, "Async inference error", t);
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(t);
                    }
                });
            }
        });
    }

    private synchronized PredictionResult executeInference(List<CharasteristicDTO> characteristicDTOs) throws Exception {
        ensureInitialized();

        if (context != null) {
            com.pancreatitis.infrastructure.debug.DebugPreferences prefs =
                    com.pancreatitis.infrastructure.debug.DebugPreferences.getInstance(context);
            if (prefs.isSimulateInferenceError()) {
                throw new ai.onnxruntime.OrtException(
                        ai.onnxruntime.OrtException.OrtErrorCode.ORT_FAIL,
                        "Симулированный сбой инференса (Debug Diagnostics Simulation)"
                );
            }
            inferenceEngine.setNormalizeEnabled(prefs.isZScoreNormalizationEnabled());
            inferenceEngine.setClampEnabled(prefs.isPhysiologicalClampEnabled());
            inferenceEngine.setDefaultFallbackEnabled(prefs.isDefaultFallbackEnabled());
            inferenceEngine.setChecksumVerificationEnabled(prefs.isCheckSha256Enabled());

            String debugModelId = prefs.getActiveModelId(null);
            if (debugModelId != null && !debugModelId.equalsIgnoreCase(activeModelId)) {
                try {
                    selectModel(debugModelId);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to apply debug active model: " + debugModelId, e);
                }
            }
        }

        this.lastLoadedCharacteristics = extractCharacteristicsFromDTO(characteristicDTOs);
        Map<String, Float> featureMap = buildFeatureMap(characteristicDTOs, manifest);

        com.pancreatitis.core.model.ml.PredictionResult coreResult = inferenceEngine.predict(featureMap);
        return new PredictionResult(coreResult);
    }

    private Map<String, Float> buildFeatureMap(List<CharasteristicDTO> dtoList, ModelManifest manifest) {
        Map<String, Float> featureMap = new HashMap<>();
        if (manifest == null || manifest.getFeatures() == null) {
            return featureMap;
        }

        Map<String, Float> dtoByName = new HashMap<>();
        Map<Integer, Float> dtoById = new HashMap<>();

        if (dtoList != null) {
            for (CharasteristicDTO dto : dtoList) {
                if (dto == null) continue;
                float val = dto.getValue();
                if (Float.isNaN(val) || Float.isInfinite(val)) {
                    continue;
                }
                if (dto.getName() != null) {
                    dtoByName.put(dto.getName().trim().toLowerCase(), val);
                }
                dtoById.put(dto.getId(), val);
            }
        }

        for (FeatureSchema schema : manifest.getFeatures()) {
            String featureKey = schema.getKey();
            float value = schema.getDefaultValue();

            Float matchedVal = null;
            if (dtoByName.containsKey(featureKey.toLowerCase())) {
                matchedVal = dtoByName.get(featureKey.toLowerCase());
            } else {
                int matchedId = schema.getIndex() + 3;
                if (dtoById.containsKey(matchedId)) {
                    matchedVal = dtoById.get(matchedId);
                }
            }

            if (matchedVal != null && !Float.isNaN(matchedVal) && !Float.isInfinite(matchedVal)) {
                value = schema.clamp(matchedVal);
            } else {
                value = schema.clamp(schema.getDefaultValue());
            }

            featureMap.put(featureKey, value);
        }

        return featureMap;
    }

    private List<Characteristic> extractCharacteristicsFromDTO(List<CharasteristicDTO> dtoList) {
        if (dtoList == null) {
            return Collections.emptyList();
        }
        List<Characteristic> characteristics = new ArrayList<>();
        for (CharasteristicDTO dto : dtoList) {
            Characteristic ch = new Characteristic();
            ch.setId(dto.getId());
            ch.setName(dto.getName());
            ch.setOpis(dto.getOpis());
            ch.setIdType(dto.getIdType());
            ch.setWeight(dto.getWeight());
            ch.setMinValue(dto.getMinValue());
            ch.setMaxValue(dto.getMaxValue());
            characteristics.add(ch);
        }
        return characteristics;
    }

    public synchronized void selectModel(String modelId) throws Exception {
        ensureInitialized();
        if (inferenceEngine != null) {
            inferenceEngine.loadModel(modelId);
            this.activeModelId = modelId;
            Log.i(TAG, "Switched active ML model to: " + modelId);
        }
    }

    public synchronized String getActiveModelId() {
        return activeModelId;
    }

    public synchronized ModelMetadata getActiveModelMetadata() {
        return (inferenceEngine != null) ? inferenceEngine.getActiveModelMetadata() : null;
    }

    public synchronized List<ModelMetadata> getAvailableModels() {
        return (manifest != null && manifest.getModels() != null)
                ? manifest.getModels()
                : Collections.emptyList();
    }

    public synchronized ModelManifest getManifest() {
        return manifest;
    }

    public synchronized boolean isReady() {
        return inferenceEngine != null && inferenceEngine.isModelLoaded();
    }

    public synchronized OnnxInferenceEngine getInferenceEngine() {
        return inferenceEngine;
    }

    public List<Characteristic> getLastLoadedCharacteristics() {
        return lastLoadedCharacteristics != null
                ? new ArrayList<>(lastLoadedCharacteristics)
                : Collections.emptyList();
    }
}