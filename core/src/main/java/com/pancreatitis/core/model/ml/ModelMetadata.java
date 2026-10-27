package com.pancreatitis.core.model.ml;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ModelMetadata implements Serializable {

    @JsonProperty("id")
    private final String id;

    @JsonProperty("version")
    private final String version;

    @JsonProperty("name")
    private final String name;

    @JsonProperty("format")
    private final String format;

    @JsonProperty("file_name")
    private final String fileName;

    @JsonProperty("sha256")
    private final String sha256;

    @JsonProperty("size_bytes")
    private final long sizeBytes;

    @JsonProperty("metrics")
    private final Map<String, Object> metrics;

    @JsonProperty("tensor_schema")
    private final TensorSchema tensorSchema;

    @JsonCreator
    public ModelMetadata(
            @JsonProperty(value = "id", required = true) String id,
            @JsonProperty("version") String version,
            @JsonProperty(value = "name", required = true) String name,
            @JsonProperty("format") String format,
            @JsonProperty(value = "file_name", required = true) String fileName,
            @JsonProperty(value = "sha256", required = true) String sha256,
            @JsonProperty("size_bytes") long sizeBytes,
            @JsonProperty("metrics") Map<String, Object> metrics,
            @JsonProperty("tensor_schema") TensorSchema tensorSchema
    ) {
        this.id = id;
        this.version = (version != null) ? version : "1.0.0";
        this.name = name;
        this.format = (format != null) ? format : "onnx";
        this.fileName = fileName;
        this.sha256 = sha256;
        this.sizeBytes = sizeBytes;
        this.metrics = (metrics != null) ? Collections.unmodifiableMap(metrics) : Collections.emptyMap();
        this.tensorSchema = (tensorSchema != null)
                ? tensorSchema
                : new TensorSchema("input", List.of(1, 42), "float32");
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public String getFormat() {
        return format;
    }

    public String getFileName() {
        return fileName;
    }

    public String getSha256() {
        return sha256;
    }

    public String getSha256Checksum() {
        return sha256;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    public TensorSchema getTensorSchema() {
        return tensorSchema;
    }

    public Double getMetricValue(String metricKey) {
        Object val = metrics.get(metricKey);
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModelMetadata that = (ModelMetadata) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(version, that.version) &&
                Objects.equals(sha256, that.sha256);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version, sha256);
    }

    @Override
    public String toString() {
        return "ModelMetadata{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", fileName='" + fileName + '\'' +
                ", sha256='" + sha256 + '\'' +
                '}';
    }
}
