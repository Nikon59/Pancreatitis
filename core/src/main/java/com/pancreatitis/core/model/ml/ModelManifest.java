package com.pancreatitis.core.model.ml;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ModelManifest implements Serializable {

    @JsonProperty("manifest_version")
    private final String manifestVersion;

    @JsonProperty("target_classes")
    private final Map<Integer, String> targetClasses;

    @JsonProperty("features")
    private final List<FeatureSchema> features;

    @JsonProperty("models")
    private final List<ModelMetadata> models;

    @JsonIgnore
    private final Map<String, FeatureSchema> featuresByKey;

    @JsonIgnore
    private final Map<String, ModelMetadata> modelsById;

    @JsonCreator
    public ModelManifest(
            @JsonProperty(value = "manifest_version", required = true) String manifestVersion,
            @JsonProperty(value = "target_classes", required = true) Map<Integer, String> targetClasses,
            @JsonProperty(value = "features", required = true) List<FeatureSchema> features,
            @JsonProperty(value = "models", required = true) List<ModelMetadata> models
    ) {
        this.manifestVersion = manifestVersion;
        this.targetClasses = (targetClasses != null)
                ? Collections.unmodifiableMap(new LinkedHashMap<>(targetClasses))
                : Collections.emptyMap();
        this.features = (features != null)
                ? Collections.unmodifiableList(features)
                : Collections.emptyList();
        this.models = (models != null)
                ? Collections.unmodifiableList(models)
                : Collections.emptyList();

        Map<String, FeatureSchema> byKey = new LinkedHashMap<>();
        for (FeatureSchema feature : this.features) {
            byKey.put(feature.getKey(), feature);
        }
        this.featuresByKey = Collections.unmodifiableMap(byKey);

        Map<String, ModelMetadata> byId = new LinkedHashMap<>();
        for (ModelMetadata model : this.models) {
            byId.put(model.getId(), model);
        }
        this.modelsById = Collections.unmodifiableMap(byId);
    }

    public String getManifestVersion() {
        return manifestVersion;
    }

    public Map<Integer, String> getTargetClasses() {
        return targetClasses;
    }

    public String getTargetClassName(int classIndex) {
        return targetClasses.getOrDefault(classIndex, "Неизвестный класс (" + classIndex + ")");
    }

    public List<FeatureSchema> getFeatures() {
        return features;
    }

    public List<ModelMetadata> getModels() {
        return models;
    }

    public Optional<ModelMetadata> findModelById(String modelId) {
        if (modelId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(modelsById.get(modelId));
    }

    public ModelMetadata getModel(String modelId) {
        return findModelById(modelId).orElse(null);
    }

    public Optional<FeatureSchema> findFeatureByKey(String key) {
        if (key == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(featuresByKey.get(key));
    }

    public Optional<FeatureSchema> findFeatureByIndex(int index) {
        if (index >= 0 && index < features.size()) {
            return Optional.of(features.get(index));
        }
        return Optional.empty();
    }

    public int getFeaturesCount() {
        return features.size();
    }

    public int getModelsCount() {
        return models.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModelManifest that = (ModelManifest) o;
        return Objects.equals(manifestVersion, that.manifestVersion) &&
                Objects.equals(targetClasses, that.targetClasses) &&
                Objects.equals(features, that.features) &&
                Objects.equals(models, that.models);
    }

    @Override
    public int hashCode() {
        return Objects.hash(manifestVersion, targetClasses, features, models);
    }

    @Override
    public String toString() {
        return "ModelManifest{" +
                "manifestVersion='" + manifestVersion + '\'' +
                ", targetClasses=" + targetClasses +
                ", featuresCount=" + features.size() +
                ", modelsCount=" + models.size() +
                '}';
    }
}
