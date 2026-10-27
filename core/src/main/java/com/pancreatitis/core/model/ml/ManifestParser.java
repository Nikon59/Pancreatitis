package com.pancreatitis.core.model.ml;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class ManifestParser {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private ManifestParser() {
    }

    public static ModelManifest parse(InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream must not be null");
        ModelManifest manifest = MAPPER.readValue(inputStream, ModelManifest.class);
        validate(manifest);
        return manifest;
    }

    public static ModelManifest parse(Path path) throws IOException {
        Objects.requireNonNull(path, "path must not be null");
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Manifest file does not exist: " + path.toAbsolutePath());
        }
        try (InputStream is = Files.newInputStream(path)) {
            return parse(is);
        }
    }

    public static ModelManifest parse(byte[] jsonBytes) throws IOException {
        Objects.requireNonNull(jsonBytes, "jsonBytes must not be null");
        ModelManifest manifest = MAPPER.readValue(jsonBytes, ModelManifest.class);
        validate(manifest);
        return manifest;
    }

    public static ModelManifest parse(String jsonString) throws IOException {
        Objects.requireNonNull(jsonString, "jsonString must not be null");
        return parse(jsonString.getBytes(StandardCharsets.UTF_8));
    }

    public static String toJson(ModelManifest manifest) throws IOException {
        Objects.requireNonNull(manifest, "manifest must not be null");
        return MAPPER.writeValueAsString(manifest);
    }

    public static void writeToFile(ModelManifest manifest, Path destination) throws IOException {
        Objects.requireNonNull(manifest, "manifest must not be null");
        Objects.requireNonNull(destination, "destination must not be null");
        Path parent = destination.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        MAPPER.writeValue(destination.toFile(), manifest);
    }

    private static void validate(ModelManifest manifest) {
        if (manifest.getManifestVersion() == null || manifest.getManifestVersion().isBlank()) {
            throw new IllegalArgumentException("Invalid manifest: manifest_version is required");
        }
        if (manifest.getTargetClasses() == null || manifest.getTargetClasses().isEmpty()) {
            throw new IllegalArgumentException("Invalid manifest: target_classes must not be empty");
        }
        if (manifest.getFeatures() == null || manifest.getFeatures().isEmpty()) {
            throw new IllegalArgumentException("Invalid manifest: features list must not be empty");
        }
        if (manifest.getModels() == null || manifest.getModels().isEmpty()) {
            throw new IllegalArgumentException("Invalid manifest: models list must not be empty");
        }
    }
}
