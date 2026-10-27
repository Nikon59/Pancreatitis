package com.pancreatitis.core.model.ml;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ManifestParserTest {

    @Test
    @DisplayName("Parse valid model manifest from string")
    void testParseManifestString() throws Exception {
        String json = """
        {
          "manifest_version": "1.1.0",
          "target_classes": {
            "0": "Отёчный панкреатит",
            "1": "Панкрионекроз"
          },
          "features": [
            {
              "index": 0,
              "key": "Sex",
              "title": "Sex",
              "type": "float32",
              "min": 0.0,
              "max": 100.0,
              "default": 0.0,
              "scaling": {
                "mean": 0.5,
                "std": 0.25
              }
            }
          ],
          "models": [
            {
              "id": "catboost",
              "version": "1.0.0",
              "name": "CatBoost Classifier",
              "format": "onnx",
              "file_name": "catboost.onnx",
              "sha256": "b19616e5c6f942cc073e359974a0c1e2f2310b9f38b5e1ce0d5ff45dbd919dbe",
              "size_bytes": 620380,
              "metrics": {
                "roc_auc": 0.9203
              },
              "tensor_schema": {
                "input_name": "features",
                "shape": [1, 42],
                "dtype": "float32"
              }
            }
          ]
        }
        """;

        ModelManifest manifest = ManifestParser.parse(json);

        assertNotNull(manifest);
        assertEquals("1.1.0", manifest.getManifestVersion());
        assertEquals(2, manifest.getTargetClasses().size());
        assertEquals("Отёчный панкреатит", manifest.getTargetClassName(0));
        assertEquals("Панкрионекроз", manifest.getTargetClassName(1));

        assertEquals(1, manifest.getFeaturesCount());
        FeatureSchema feature = manifest.getFeatures().get(0);
        assertEquals("Sex", feature.getKey());
        assertEquals(0.5f, feature.getScaling().getMean());
        assertEquals(0.25f, feature.getScaling().getStd());
        assertEquals(2.0f, feature.normalize(1.0f));

        assertEquals(1, manifest.getModelsCount());
        Optional<ModelMetadata> catboost = manifest.findModelById("catboost");
        assertTrue(catboost.isPresent());
        assertEquals("features", catboost.get().getTensorSchema().getInputName());
        assertEquals(620380L, catboost.get().getSizeBytes());
    }

    @Test
    @DisplayName("Parse actual repository model_manifest.json")
    void testParseActualRepositoryManifest() throws Exception {
        Path manifestPath = Paths.get("../../ml-medclass-experiments/results/model_manifest.json");
        if (!manifestPath.toFile().exists()) {
            manifestPath = Paths.get("../ml-medclass-experiments/results/model_manifest.json");
        }
        if (!manifestPath.toFile().exists()) {
            manifestPath = Paths.get("app/src/main/assets/models/model_manifest.json");
        }

        if (manifestPath.toFile().exists()) {
            ModelManifest manifest = ManifestParser.parse(manifestPath);
            assertNotNull(manifest);
            assertEquals("1.1.0", manifest.getManifestVersion());
            assertEquals(42, manifest.getFeaturesCount());
            assertEquals(5, manifest.getModelsCount());
            assertTrue(manifest.findModelById("catboost").isPresent());
            assertTrue(manifest.findModelById("xgboost").isPresent());
            assertTrue(manifest.findModelById("lightgbm").isPresent());
            assertTrue(manifest.findModelById("random_forest").isPresent());
            assertTrue(manifest.findModelById("logreg").isPresent());
        }
    }
}
