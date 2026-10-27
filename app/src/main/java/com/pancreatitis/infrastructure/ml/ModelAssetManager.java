package com.pancreatitis.infrastructure.ml;

import android.content.Context;
import android.util.Log;

import com.pancreatitis.core.ml.ChecksumVerifier;
import com.pancreatitis.core.model.ml.ManifestParser;
import com.pancreatitis.core.model.ml.ModelManifest;
import com.pancreatitis.core.model.ml.ModelMetadata;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

public final class ModelAssetManager {

    private static final String TAG = "ModelAssetManager";
    private static final String ASSETS_MODELS_DIR = "models";
    private static final String MANIFEST_FILE_NAME = "model_manifest.json";

    private ModelAssetManager() {
    }

    public static synchronized File ensureModelsExtracted(Context context) throws IOException {
        Objects.requireNonNull(context, "context must not be null");

        File targetDir = new File(context.getFilesDir(), ASSETS_MODELS_DIR);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new IOException("Failed to create models directory: " + targetDir.getAbsolutePath());
        }

        ModelManifest manifest;
        try (InputStream manifestStream = context.getAssets().open(ASSETS_MODELS_DIR + "/" + MANIFEST_FILE_NAME)) {
            manifest = ManifestParser.parse(manifestStream);
        } catch (Exception e) {
            throw new IOException("Failed to load model manifest from assets", e);
        }

        File targetManifestFile = new File(targetDir, MANIFEST_FILE_NAME);
        copyAssetToFile(context, ASSETS_MODELS_DIR + "/" + MANIFEST_FILE_NAME, targetManifestFile);

        for (ModelMetadata modelMeta : manifest.getModels()) {
            File targetModelFile = new File(targetDir, modelMeta.getFileName());
            boolean needsCopy = true;

            if (targetModelFile.exists()) {
                try {
                    boolean valid = ChecksumVerifier.verifyFileSha256(targetModelFile.toPath(), modelMeta.getSha256());
                    if (valid) {
                        needsCopy = false;
                    } else {
                        Log.w(TAG, "Existing model checksum mismatch: " + modelMeta.getId() + ", extracting fresh copy");
                    }
                } catch (Exception e) {
                    needsCopy = true;
                }
            }

            if (needsCopy) {
                copyAssetToFile(context, ASSETS_MODELS_DIR + "/" + modelMeta.getFileName(), targetModelFile);
                boolean verified = ChecksumVerifier.verifyFileSha256(targetModelFile.toPath(), modelMeta.getSha256());
                if (!verified) {
                    targetModelFile.delete();
                    throw new SecurityException("Extracted model failed SHA-256 validation: " + modelMeta.getId());
                }
                Log.i(TAG, "Successfully extracted and verified model: " + modelMeta.getId());
            }
        }

        return targetDir;
    }

    public static synchronized File forceReExtract(Context context) throws IOException {
        File targetDir = new File(context.getFilesDir(), ASSETS_MODELS_DIR);
        if (targetDir.exists()) {
            File[] files = targetDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
        }
        return ensureModelsExtracted(context);
    }

    private static void copyAssetToFile(Context context, String assetPath, File destinationFile) throws IOException {
        File tempFile = new File(destinationFile.getParentFile(), destinationFile.getName() + ".tmp");
        try (InputStream in = context.getAssets().open(assetPath);
             OutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        }

        if (destinationFile.exists() && !destinationFile.delete()) {
            Log.w(TAG, "Could not delete old file: " + destinationFile.getAbsolutePath());
        }

        if (!tempFile.renameTo(destinationFile)) {
            throw new IOException("Failed to rename " + tempFile.getName() + " to " + destinationFile.getName());
        }
    }
}
