package com.pancreatitis.core.ml;

import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;

import com.pancreatitis.core.model.ml.ModelManifest;
import com.pancreatitis.core.model.ml.ModelMetadata;
import com.pancreatitis.core.model.ml.PredictionResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class OnnxInferenceEngine implements InferenceEngine {

    private final ModelManifest manifest;
    private final Function<ModelMetadata, Path> modelPathResolver;
    private final Function<ModelMetadata, byte[]> modelBytesResolver;
    private final FeatureNormalizer normalizer;
    private final OrtEnvironment ortEnv;

    private OrtSession activeSession;
    private ModelMetadata activeMetadata;
    private String resolvedInputName;
    private int resolvedInputDim;
    private volatile boolean checksumVerificationEnabled = true;

    public OnnxInferenceEngine(ModelManifest manifest, Path modelsDirectory) {
        this(manifest, meta -> modelsDirectory.resolve(meta.getFileName()), null);
    }

    public OnnxInferenceEngine(ModelManifest manifest, Function<ModelMetadata, Path> modelPathResolver) {
        this(manifest, modelPathResolver, null);
    }

    public OnnxInferenceEngine(ModelManifest manifest,
                               Function<ModelMetadata, Path> modelPathResolver,
                               Function<ModelMetadata, byte[]> modelBytesResolver) {
        this.manifest = Objects.requireNonNull(manifest, "manifest must not be null");
        this.modelPathResolver = modelPathResolver;
        this.modelBytesResolver = modelBytesResolver;
        if (modelPathResolver == null && modelBytesResolver == null) {
            throw new IllegalArgumentException("Either modelPathResolver or modelBytesResolver must be provided");
        }
        this.normalizer = new FeatureNormalizer(manifest);
        this.ortEnv = OrtEnvironment.getEnvironment();
    }

    @Override
    public synchronized void loadModel(String modelId) {
        Objects.requireNonNull(modelId, "modelId must not be null");

        ModelMetadata metadata = manifest.findModelById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found in manifest: " + modelId));

        try (OrtSession.SessionOptions options = new OrtSession.SessionOptions()) {
            closeActiveSession();

            if (modelBytesResolver != null) {
                byte[] modelBytes = modelBytesResolver.apply(metadata);
                if (modelBytes == null || modelBytes.length == 0) {
                    throw new IllegalStateException("Model bytes empty for model: " + modelId);
                }
                if (checksumVerificationEnabled) {
                    boolean checksumValid = ChecksumVerifier.verifyBytesSha256(modelBytes, metadata.getSha256());
                    if (!checksumValid) {
                        throw new SecurityException("SHA-256 checksum mismatch for model: " + modelId +
                                ", expected: " + metadata.getSha256());
                    }
                }
                this.activeSession = ortEnv.createSession(modelBytes, options);
            } else {
                Path modelFile = modelPathResolver.apply(metadata);
                if (modelFile == null || !Files.exists(modelFile)) {
                    throw new IllegalStateException("Model file not found on disk: " +
                            (modelFile != null ? modelFile.toAbsolutePath() : "null"));
                }
                if (checksumVerificationEnabled) {
                    boolean checksumValid = ChecksumVerifier.verifyFileSha256(modelFile, metadata.getSha256());
                    if (!checksumValid) {
                        throw new SecurityException("SHA-256 checksum mismatch for model: " + modelId +
                                ", file: " + modelFile.getFileName() +
                                ", expected: " + metadata.getSha256());
                    }
                }
                this.activeSession = ortEnv.createSession(modelFile.toString(), options);
            }

            this.activeMetadata = metadata;
            inspectSessionInput(metadata);

        } catch (IOException e) {
            throw new IllegalStateException("Failed to read model for: " + modelId, e);
        } catch (OrtException e) {
            throw new IllegalStateException("Failed to initialize ONNX session for model: " + modelId, e);
        }
    }

    private void inspectSessionInput(ModelMetadata metadata) {
        try {
            Map<String, NodeInfo> inputInfoMap = activeSession.getInputInfo();
            if (!inputInfoMap.isEmpty()) {
                Map.Entry<String, NodeInfo> firstEntry = inputInfoMap.entrySet().iterator().next();
                this.resolvedInputName = firstEntry.getKey();

                NodeInfo nodeInfo = firstEntry.getValue();
                if (nodeInfo.getInfo() instanceof TensorInfo) {
                    TensorInfo tensorInfo = (TensorInfo) nodeInfo.getInfo();
                    long[] shape = tensorInfo.getShape();
                    if (shape != null && shape.length > 1 && shape[1] > 0) {
                        this.resolvedInputDim = (int) shape[1];
                        return;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // Fallback to metadata schema
        this.resolvedInputName = metadata.getTensorSchema().getInputName();
        List<Integer> shape = metadata.getTensorSchema().getShape();
        this.resolvedInputDim = (shape != null && shape.size() > 1 && shape.get(1) > 0)
                ? shape.get(1)
                : manifest.getFeaturesCount();
    }

    @Override
    public PredictionResult predict(Map<String, Float> rawFeaturesByKey) {
        FeatureNormalizer.NormalizedInput normalizedInput = normalizer.normalize(rawFeaturesByKey);
        return runInference(normalizedInput);
    }

    @Override
    public PredictionResult predict(float[] rawFeaturesInOrder) {
        FeatureNormalizer.NormalizedInput normalizedInput = normalizer.normalize(rawFeaturesInOrder);
        return runInference(normalizedInput);
    }

    private synchronized PredictionResult runInference(FeatureNormalizer.NormalizedInput input) {
        if (activeSession == null || activeMetadata == null) {
            throw new IllegalStateException("No active model loaded. Call loadModel() before predicting.");
        }

        float[] features = input.getFeatures();
        int targetDim = (resolvedInputDim > 0) ? resolvedInputDim : features.length;
        float[][] batchInput = new float[1][targetDim];

        System.arraycopy(features, 0, batchInput[0], 0, Math.min(features.length, targetDim));

        String inputName = (resolvedInputName != null && !resolvedInputName.isBlank())
                ? resolvedInputName
                : activeMetadata.getTensorSchema().getInputName();

        OnnxTensor inputTensor = null;

        try {
            inputTensor = OnnxTensor.createTensor(ortEnv, batchInput);
            Map<String, OnnxTensor> inputs = Collections.singletonMap(inputName, inputTensor);

            try (OrtSession.Result result = activeSession.run(inputs)) {
                float[] probabilities = extractProbabilities(result);

                Map<Integer, Float> classProbabilities = new LinkedHashMap<>();
                int predictedClassIndex = 0;
                float maxProb = -1.0f;

                for (int i = 0; i < probabilities.length; i++) {
                    float prob = probabilities[i];
                    classProbabilities.put(i, prob);
                    if (prob > maxProb) {
                        maxProb = prob;
                        predictedClassIndex = i;
                    }
                }

                String predictedClassName = manifest.getTargetClassName(predictedClassIndex);

                return new PredictionResult(
                        activeMetadata.getId(),
                        activeMetadata.getVersion(),
                        predictedClassIndex,
                        predictedClassName,
                        classProbabilities,
                        manifest.getTargetClasses(),
                        input.getUsedFeaturesCount(),
                        input.getTotalFeaturesCount(),
                        Instant.now()
                );
            }
        } catch (OrtException e) {
            throw new IllegalStateException("ONNX inference failed for model: " + activeMetadata.getId(), e);
        } finally {
            if (inputTensor != null) {
                inputTensor.close();
            }
        }
    }

    private float[] extractProbabilities(OrtSession.Result result) throws OrtException {
        // Strategy 1: Check by name (probabilities, output_probability, etc.)
        for (Map.Entry<String, OnnxValue> entry : result) {
            String name = entry.getKey().toLowerCase();
            if (name.contains("prob") || name.contains("output")) {
                float[] probs = extractFloatArrayFromValue(entry.getValue());
                if (probs != null) {
                    return probs;
                }
            }
        }

        // Strategy 2: If result has >= 2 outputs, the second output is typically probabilities
        int count = 0;
        for (Map.Entry<String, OnnxValue> entry : result) {
            if (count == 1) {
                float[] probs = extractFloatArrayFromValue(entry.getValue());
                if (probs != null) {
                    return probs;
                }
            }
            count++;
        }

        // Strategy 3: Check first output
        for (Map.Entry<String, OnnxValue> entry : result) {
            float[] probs = extractFloatArrayFromValue(entry.getValue());
            if (probs != null) {
                return probs;
            }
        }

        throw new IllegalStateException("Unable to extract probability vector from ONNX output tensors");
    }

    private float[] extractFloatArrayFromValue(OnnxValue value) throws OrtException {
        Object raw = value.getValue();
        if (raw instanceof float[][]) {
            float[][] matrix = (float[][]) raw;
            if (matrix.length > 0) {
                return sanitizeProbabilities(matrix[0]);
            }
        } else if (raw instanceof float[]) {
            return sanitizeProbabilities((float[]) raw);
        } else if (raw instanceof List) {
            List<?> list = (List<?>) raw;
            if (!list.isEmpty()) {
                Object first = list.get(0);
                Map<?, ?> map = null;
                if (first instanceof Map) {
                    map = (Map<?, ?>) first;
                } else if (first instanceof OnnxValue) {
                    Object inner = ((OnnxValue) first).getValue();
                    if (inner instanceof Map) {
                        map = (Map<?, ?>) inner;
                    }
                }
                if (map != null) {
                    return extractProbabilitiesFromMap(map);
                }
            }
        } else if (raw instanceof Map) {
            return extractProbabilitiesFromMap((Map<?, ?>) raw);
        }
        return null;
    }

    private float[] extractProbabilitiesFromMap(Map<?, ?> map) {
        int maxKey = 0;
        for (Object k : map.keySet()) {
            if (k instanceof Number) {
                maxKey = Math.max(maxKey, ((Number) k).intValue());
            }
        }
        float[] probs = new float[maxKey + 1];
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() instanceof Number && entry.getValue() instanceof Number) {
                int key = ((Number) entry.getKey()).intValue();
                float val = ((Number) entry.getValue()).floatValue();
                if (key >= 0 && key < probs.length) {
                    probs[key] = val;
                }
            }
        }
        return sanitizeProbabilities(probs);
    }

    private float[] sanitizeProbabilities(float[] raw) {
        if (raw == null || raw.length == 0) {
            return new float[]{0.5f, 0.5f};
        }

        if (raw.length == 1) {
            float p1 = Math.max(0.0f, Math.min(1.0f, raw[0]));
            return new float[]{1.0f - p1, p1};
        }

        boolean needsSoftmax = false;
        float sum = 0.0f;
        for (float v : raw) {
            sum += v;
            if (v < 0.0f || v > 1.0f) {
                needsSoftmax = true;
                break;
            }
        }

        if (needsSoftmax || Math.abs(sum - 1.0f) > 0.05f) {
            float max = Float.NEGATIVE_INFINITY;
            for (float v : raw) {
                if (v > max) max = v;
            }
            float expSum = 0.0f;
            float[] probs = new float[raw.length];
            for (int i = 0; i < raw.length; i++) {
                probs[i] = (float) Math.exp(raw[i] - max);
                expSum += probs[i];
            }
            if (expSum > 0.0f) {
                for (int i = 0; i < probs.length; i++) {
                    probs[i] /= expSum;
                }
            }
            return probs;
        }

        return raw;
    }

    private void closeActiveSession() {
        if (activeSession != null) {
            try {
                activeSession.close();
            } catch (OrtException ignored) {
            }
            activeSession = null;
            activeMetadata = null;
            resolvedInputName = null;
            resolvedInputDim = 0;
        }
    }

    @Override
    public synchronized ModelMetadata getActiveModelMetadata() {
        return activeMetadata;
    }

    @Override
    public ModelManifest getManifest() {
        return manifest;
    }

    @Override
    public List<String> getAvailableModelIds() {
        List<String> ids = new ArrayList<>();
        for (ModelMetadata model : manifest.getModels()) {
            ids.add(model.getId());
        }
        return Collections.unmodifiableList(ids);
    }

    @Override
    public synchronized boolean isModelLoaded() {
        return activeSession != null;
    }

    public void setChecksumVerificationEnabled(boolean enabled) {
        this.checksumVerificationEnabled = enabled;
    }

    public boolean isChecksumVerificationEnabled() {
        return checksumVerificationEnabled;
    }

    public void setNormalizeEnabled(boolean enabled) {
        this.normalizer.setNormalizeEnabled(enabled);
    }

    public boolean isNormalizeEnabled() {
        return this.normalizer.isNormalizeEnabled();
    }

    public void setClampEnabled(boolean enabled) {
        this.normalizer.setClampEnabled(enabled);
    }

    public boolean isClampEnabled() {
        return this.normalizer.isClampEnabled();
    }

    public void setDefaultFallbackEnabled(boolean enabled) {
        this.normalizer.setDefaultFallbackEnabled(enabled);
    }

    public boolean isDefaultFallbackEnabled() {
        return this.normalizer.isDefaultFallbackEnabled();
    }

    @Override
    public synchronized void close() {
        closeActiveSession();
    }
}
