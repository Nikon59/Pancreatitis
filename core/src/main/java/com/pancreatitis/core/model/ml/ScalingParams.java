package com.pancreatitis.core.model.ml;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

public final class ScalingParams implements Serializable {

    private final float mean;
    private final float std;

    @JsonCreator
    public ScalingParams(
            @JsonProperty("mean") float mean,
            @JsonProperty("std") float std
    ) {
        this.mean = mean;
        this.std = (std != 0.0f) ? std : 1.0f;
    }

    public float getMean() {
        return mean;
    }

    public float getStd() {
        return std;
    }

    public float normalize(float rawValue) {
        return (rawValue - mean) / std;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScalingParams that = (ScalingParams) o;
        return Float.compare(that.mean, mean) == 0 && Float.compare(that.std, std) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mean, std);
    }

    @Override
    public String toString() {
        return "ScalingParams{" +
                "mean=" + mean +
                ", std=" + std +
                '}';
    }
}
