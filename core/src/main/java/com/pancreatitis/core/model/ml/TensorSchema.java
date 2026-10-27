package com.pancreatitis.core.model.ml;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class TensorSchema implements Serializable {

    @JsonProperty("input_name")
    private final String inputName;

    @JsonProperty("shape")
    private final List<Integer> shape;

    @JsonProperty("dtype")
    private final String dtype;

    @JsonCreator
    public TensorSchema(
            @JsonProperty(value = "input_name", required = true) String inputName,
            @JsonProperty("shape") List<Integer> shape,
            @JsonProperty("dtype") String dtype
    ) {
        this.inputName = inputName;
        this.shape = (shape != null) ? Collections.unmodifiableList(shape) : List.of(1, 42);
        this.dtype = (dtype != null) ? dtype : "float32";
    }

    public String getInputName() {
        return inputName;
    }

    public List<Integer> getShape() {
        return shape;
    }

    public String getDtype() {
        return dtype;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TensorSchema that = (TensorSchema) o;
        return Objects.equals(inputName, that.inputName) &&
                Objects.equals(shape, that.shape) &&
                Objects.equals(dtype, that.dtype);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inputName, shape, dtype);
    }

    @Override
    public String toString() {
        return "TensorSchema{" +
                "inputName='" + inputName + '\'' +
                ", shape=" + shape +
                ", dtype='" + dtype + '\'' +
                '}';
    }
}
