package com.pancreatitis.core.ml;

import com.pancreatitis.core.model.ml.ModelManifest;
import com.pancreatitis.core.model.ml.ModelMetadata;
import com.pancreatitis.core.model.ml.PredictionResult;

import java.util.List;
import java.util.Map;

public interface InferenceEngine extends AutoCloseable {

    PredictionResult predict(Map<String, Float> rawFeaturesByKey);

    PredictionResult predict(float[] rawFeaturesInOrder);

    void loadModel(String modelId);

    ModelMetadata getActiveModelMetadata();

    ModelManifest getManifest();

    List<String> getAvailableModelIds();

    boolean isModelLoaded();

    @Override
    void close();
}
