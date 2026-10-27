package com.pancreatitis.modules.prediction;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PredictionResult {
    private final Map<Integer, Float> probabilities;
    private final com.pancreatitis.core.model.ml.PredictionResult coreResult;

    public PredictionResult(Map<Integer, Float> probabilities) {
        this.probabilities = new HashMap<>(probabilities);
        this.coreResult = null;
    }

    public PredictionResult(com.pancreatitis.core.model.ml.PredictionResult coreResult) {
        this.coreResult = coreResult;
        this.probabilities = (coreResult != null && coreResult.getClassProbabilities() != null)
                ? new HashMap<>(coreResult.getClassProbabilities())
                : Collections.emptyMap();
    }

    public Map<Integer, Float> getProbabilities() {
        return new HashMap<>(probabilities);
    }

    public com.pancreatitis.core.model.ml.PredictionResult getCoreResult() {
        return coreResult;
    }

    public int getPredictedClass() {
        if (coreResult != null) {
            return coreResult.getPredictedClassIndex();
        }
        int predictedClass = -1;
        float maxProb = -1f;
        for (Map.Entry<Integer, Float> entry : probabilities.entrySet()) {
            if (entry.getValue() > maxProb) {
                maxProb = entry.getValue();
                predictedClass = entry.getKey();
            }
        }
        return predictedClass;
    }

    public String getPredictedClassName() {
        return coreResult != null ? coreResult.getPredictedClassName() : "";
    }

    public String getModelId() {
        return coreResult != null ? coreResult.getModelId() : "onnx";
    }

    public int getFeaturesUsedCount() {
        return coreResult != null ? coreResult.getFeaturesUsedCount() : 0;
    }

    public int getTotalFeaturesCount() {
        return coreResult != null ? coreResult.getTotalFeaturesCount() : 0;
    }

    @Override
    public String toString() {
        return "PredictionResult{" +
                "predictedClass=" + getPredictedClass() +
                ", className='" + getPredictedClassName() + '\'' +
                ", probabilities=" + probabilities +
                '}';
    }
}