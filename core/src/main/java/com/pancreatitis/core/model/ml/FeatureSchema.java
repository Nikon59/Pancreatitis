package com.pancreatitis.core.model.ml;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

public final class FeatureSchema implements Serializable {

    private final int index;
    private final String key;
    private final String title;
    private final String type;
    private final float min;
    private final float max;
    private final float defaultValue;
    private final ScalingParams scaling;

    @JsonCreator
    public FeatureSchema(
            @JsonProperty(value = "index", required = true) int index,
            @JsonProperty(value = "key", required = true) String key,
            @JsonProperty(value = "title", required = true) String title,
            @JsonProperty("type") String type,
            @JsonProperty("min") float min,
            @JsonProperty("max") float max,
            @JsonProperty("default") float defaultValue,
            @JsonProperty("scaling") ScalingParams scaling
    ) {
        this.index = index;
        this.key = key;
        this.title = title;
        this.type = (type != null) ? type : "float32";
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
        this.scaling = (scaling != null) ? scaling : new ScalingParams(0.0f, 1.0f);
    }

    public int getIndex() {
        return index;
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public float getDefaultValue() {
        return defaultValue;
    }

    public ScalingParams getScaling() {
        return scaling;
    }

    public float clamp(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return defaultValue;
        }
        if (min < max) {
            return Math.max(min, Math.min(max, value));
        }
        return value;
    }

    public float normalize(float rawValue) {
        float clamped = clamp(rawValue);
        return scaling.normalize(clamped);
    }

    public boolean isValidValue(float value) {
        return value >= min && value <= max;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeatureSchema that = (FeatureSchema) o;
        return index == that.index && Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, key);
    }

    @Override
    public String toString() {
        return "FeatureSchema{" +
                "index=" + index +
                ", key='" + key + '\'' +
                ", title='" + title + '\'' +
                ", min=" + min +
                ", max=" + max +
                '}';
    }
}
