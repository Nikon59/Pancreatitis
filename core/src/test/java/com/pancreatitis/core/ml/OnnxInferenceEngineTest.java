package com.pancreatitis.core.ml;

import com.pancreatitis.core.model.ml.ManifestParser;
import com.pancreatitis.core.model.ml.ModelManifest;
import com.pancreatitis.core.model.ml.PredictionResult;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OnnxInferenceEngineTest {

    private Path resolveModelsDirectory() {
        Path[] candidates = new Path[]{
                Paths.get("../../ml-medclass-experiments/results"),
                Paths.get("../ml-medclass-experiments/results"),
                Paths.get("app/src/main/assets/models"),
                Paths.get("../app/src/main/assets/models")
        };

        for (Path cand : candidates) {
            if (Files.exists(cand.resolve("model_manifest.json")) && Files.exists(cand.resolve("catboost.onnx"))) {
                return cand.toAbsolutePath();
            }
        }
        return null;
    }

    @Test
    @DisplayName("Verify SHA-256 checksum check rejects tampered models")
    void testTamperedChecksumRejection() throws Exception {
        Path modelsDir = resolveModelsDirectory();
        if (modelsDir == null) {
            return;
        }

        ModelManifest manifest = ManifestParser.parse(modelsDir.resolve("model_manifest.json"));
        String json = ManifestParser.toJson(manifest);
        String tamperedManifestJson = json.replace(
                manifest.getModels().get(0).getSha256(),
                "0000000000000000000000000000000000000000000000000000000000000000"
        );

        ModelManifest tamperedManifest = ManifestParser.parse(tamperedManifestJson);

        try (OnnxInferenceEngine engine = new OnnxInferenceEngine(tamperedManifest, modelsDir)) {
            assertThrows(SecurityException.class, () -> engine.loadModel(tamperedManifest.getModels().get(0).getId()));
        }
    }

    @Test
    @DisplayName("Load all models and execute predictions with proper resource release")
    void testInferenceOnAllModels() throws Exception {
        Path modelsDir = resolveModelsDirectory();
        if (modelsDir == null) {
            return;
        }

        ModelManifest manifest = ManifestParser.parse(modelsDir.resolve("model_manifest.json"));

        try (OnnxInferenceEngine engine = new OnnxInferenceEngine(manifest, modelsDir)) {
            assertEquals(5, engine.getAvailableModelIds().size());

            Map<String, Float> testFeatures = new HashMap<>();
            testFeatures.put("Sex", 1.0f);
            testFeatures.put("Age", 55.0f);
            testFeatures.put("pain", 3.0f);
            testFeatures.put("t", 38.5f);
            testFeatures.put("amilase", 120.0f);

            for (String modelId : engine.getAvailableModelIds()) {
                engine.loadModel(modelId);
                assertTrue(engine.isModelLoaded(), "Model should be loaded: " + modelId);
                assertEquals(modelId, engine.getActiveModelMetadata().getId());

                PredictionResult result = engine.predict(testFeatures);
                assertNotNull(result, "Result must not be null for model: " + modelId);
                assertEquals(modelId, result.getModelId());
                assertTrue(result.getPredictedClassIndex() >= 0);
                assertNotNull(result.getPredictedClassName());

                assertTrue(result.getClassProbabilities().size() >= 2);
                double totalProb = result.getClassProbabilities().values().stream()
                        .mapToDouble(Float::doubleValue)
                        .sum();
                assertEquals(1.0, totalProb, 0.08, "Probabilities should sum to 1.0 for model: " + modelId);

                assertEquals(5, result.getFeaturesUsedCount());
                assertEquals(42, result.getTotalFeaturesCount());
            }

            engine.close();
            assertFalse(engine.isModelLoaded());
        }
    }

    @Test
    @DisplayName("Load model and execute prediction using byte array resolver")
    void testInferenceWithByteResolver() throws Exception {
        Path modelsDir = resolveModelsDirectory();
        if (modelsDir == null) {
            return;
        }

        ModelManifest manifest = ManifestParser.parse(modelsDir.resolve("model_manifest.json"));

        try (OnnxInferenceEngine engine = new OnnxInferenceEngine(manifest, null, meta -> {
            try {
                return Files.readAllBytes(modelsDir.resolve(meta.getFileName()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })) {
            engine.loadModel("catboost");
            assertTrue(engine.isModelLoaded());

            Map<String, Float> testFeatures = new HashMap<>();
            testFeatures.put("Sex", 0.0f);
            testFeatures.put("Age", 45.0f);

            PredictionResult result = engine.predict(testFeatures);
            assertNotNull(result);
            assertEquals("catboost", result.getModelId());
            assertNotNull(result.getPredictedClassName());
        }
    }
}
