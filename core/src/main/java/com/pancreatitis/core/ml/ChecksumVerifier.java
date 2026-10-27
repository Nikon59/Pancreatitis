package com.pancreatitis.core.ml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public final class ChecksumVerifier {

    private ChecksumVerifier() {
    }

    public static String calculateSha256(Path filePath) throws IOException {
        Objects.requireNonNull(filePath, "filePath must not be null");
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("File does not exist: " + filePath.toAbsolutePath());
        }

        try (InputStream inputStream = Files.newInputStream(filePath)) {
            return calculateSha256(inputStream);
        }
    }

    public static String calculateSha256(InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream must not be null");
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }

        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            digest.update(buffer, 0, bytesRead);
        }

        byte[] hashBytes = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    public static String calculateSha256(byte[] data) {
        Objects.requireNonNull(data, "data must not be null");
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
        byte[] hashBytes = digest.digest(data);
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    public static boolean verifyFileSha256(Path filePath, String expectedSha256) throws IOException {
        Objects.requireNonNull(expectedSha256, "expectedSha256 must not be null");
        String actualHash = calculateSha256(filePath);
        return actualHash.equalsIgnoreCase(expectedSha256.trim());
    }

    public static boolean verifyBytesSha256(byte[] data, String expectedSha256) {
        Objects.requireNonNull(expectedSha256, "expectedSha256 must not be null");
        String actualHash = calculateSha256(data);
        return actualHash.equalsIgnoreCase(expectedSha256.trim());
    }
}
