package com.pancreatitis.core.ml;

import com.pancreatitis.core.model.ml.FeatureSchema;
import com.pancreatitis.core.model.ml.ModelManifest;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class FeatureNormalizer {

    private final ModelManifest manifest;

    private volatile boolean normalizeEnabled = true;
    private volatile boolean clampEnabled = true;
    private volatile boolean defaultFallbackEnabled = true;

    public FeatureNormalizer(ModelManifest manifest) {
        this.manifest = Objects.requireNonNull(manifest, "manifest must not be null");
    }

    public void setNormalizeEnabled(boolean normalizeEnabled) {
        this.normalizeEnabled = normalizeEnabled;
    }

    public boolean isNormalizeEnabled() {
        return normalizeEnabled;
    }

    public void setClampEnabled(boolean clampEnabled) {
        this.clampEnabled = clampEnabled;
    }

    public boolean isClampEnabled() {
        return clampEnabled;
    }

    public void setDefaultFallbackEnabled(boolean defaultFallbackEnabled) {
        this.defaultFallbackEnabled = defaultFallbackEnabled;
    }

    public boolean isDefaultFallbackEnabled() {
        return defaultFallbackEnabled;
    }

    public NormalizedInput normalize(Map<String, Float> rawFeaturesByKey) {
        List<FeatureSchema> schemaList = manifest.getFeatures();
        int size = schemaList.size();
        float[] normalized = new float[size];
        int usedCount = 0;

        for (int i = 0; i < size; i++) {
            FeatureSchema schema = schemaList.get(i);
            Float userValue = (rawFeaturesByKey != null) ? rawFeaturesByKey.get(schema.getKey()) : null;

            float rawValue;
            if (userValue != null && !Float.isNaN(userValue) && !Float.isInfinite(userValue)) {
                rawValue = clampEnabled ? schema.clamp(userValue) : userValue;
                usedCount++;
            } else if (defaultFallbackEnabled) {
                rawValue = clampEnabled ? schema.clamp(schema.getDefaultValue()) : schema.getDefaultValue();
            } else {
                rawValue = 0.0f;
            }

            normalized[i] = normalizeEnabled ? schema.getScaling().normalize(rawValue) : rawValue;
        }

        return new NormalizedInput(normalized, usedCount, size);
    }

    public NormalizedInput normalize(float[] rawFeaturesInOrder) {
        List<FeatureSchema> schemaList = manifest.getFeatures();
        int size = schemaList.size();
        float[] normalized = new float[size];
        int usedCount = 0;

        for (int i = 0; i < size; i++) {
            FeatureSchema schema = schemaList.get(i);
            float rawValue;

            if (rawFeaturesInOrder != null && i < rawFeaturesInOrder.length) {
                float val = rawFeaturesInOrder[i];
                if (!Float.isNaN(val) && !Float.isInfinite(val)) {
                    rawValue = clampEnabled ? schema.clamp(val) : val;
                    usedCount++;
                } else if (defaultFallbackEnabled) {
                    rawValue = clampEnabled ? schema.clamp(schema.getDefaultValue()) : schema.getDefaultValue();
                } else {
                    rawValue = 0.0f;
                }
            } else if (defaultFallbackEnabled) {
                rawValue = clampEnabled ? schema.clamp(schema.getDefaultValue()) : schema.getDefaultValue();
            } else {
                rawValue = 0.0f;
            }

            normalized[i] = normalizeEnabled ? schema.getScaling().normalize(rawValue) : rawValue;
        }

        return new NormalizedInput(normalized, usedCount, size);
    }

    public static final class NormalizedInput {
        private final float[] features;
        private final int usedFeaturesCount;
        private final int totalFeaturesCount;

        public NormalizedInput(float[] features, int usedFeaturesCount, int totalFeaturesCount) {
            this.features = features;
            this.usedFeaturesCount = usedFeaturesCount;
            this.totalFeaturesCount = totalFeaturesCount;
        }

        public float[] getFeatures() {
            return features;
        }

        public int getUsedFeaturesCount() {
            return usedFeaturesCount;
        }

        public int getTotalFeaturesCount() {
            return totalFeaturesCount;
        }
    }
}
