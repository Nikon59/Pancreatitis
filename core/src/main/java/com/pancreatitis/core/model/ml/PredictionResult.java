package com.pancreatitis.core.model.ml;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public final class PredictionResult implements Serializable {

    private final String modelId;
    private final String modelVersion;
    private final int predictedClassIndex;
    private final String predictedClassName;
    private final Map<Integer, Float> classProbabilities;
    private final Map<Integer, String> classNames;
    private final int featuresUsedCount;
    private final int totalFeaturesCount;
    private final Instant calculatedAt;

    public PredictionResult(
            String modelId,
            String modelVersion,
            int predictedClassIndex,
            String predictedClassName,
            Map<Integer, Float> classProbabilities,
            Map<Integer, String> classNames,
            int featuresUsedCount,
            int totalFeaturesCount,
            Instant calculatedAt
    ) {
        this.modelId = modelId;
        this.modelVersion = modelVersion;
        this.predictedClassIndex = predictedClassIndex;
        this.predictedClassName = predictedClassName;
        this.classProbabilities = (classProbabilities != null)
                ? Collections.unmodifiableMap(classProbabilities)
                : Collections.emptyMap();
        this.classNames = (classNames != null)
                ? Collections.unmodifiableMap(classNames)
                : Collections.emptyMap();
        this.featuresUsedCount = featuresUsedCount;
        this.totalFeaturesCount = totalFeaturesCount;
        this.calculatedAt = (calculatedAt != null) ? calculatedAt : Instant.now();
    }

    public String getModelId() {
        return modelId;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public int getPredictedClassIndex() {
        return predictedClassIndex;
    }

    public String getPredictedClassName() {
        return predictedClassName;
    }

    public Map<Integer, Float> getClassProbabilities() {
        return classProbabilities;
    }

    public float getProbabilityForClass(int classIndex) {
        return classProbabilities.getOrDefault(classIndex, 0.0f);
    }

    public Map<Integer, String> getClassNames() {
        return classNames;
    }

    public int getFeaturesUsedCount() {
        return featuresUsedCount;
    }

    public int getTotalFeaturesCount() {
        return totalFeaturesCount;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PredictionResult that = (PredictionResult) o;
        return predictedClassIndex == that.predictedClassIndex &&
                Objects.equals(modelId, that.modelId) &&
                Objects.equals(classProbabilities, that.classProbabilities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelId, predictedClassIndex, classProbabilities);
    }

    @Override
    public String toString() {
        return "PredictionResult{" +
                "modelId='" + modelId + '\'' +
                ", predictedClass=" + predictedClassIndex + " (" + predictedClassName + ")" +
                ", probabilities=" + classProbabilities +
                '}';
    }
}
