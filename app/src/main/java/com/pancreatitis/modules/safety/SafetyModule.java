package com.pancreatitis.modules.safety;

import com.pancreatitis.models.Patient;
import com.pancreatitis.models.Questionnaire;
import com.pancreatitis.modules.authorization.AuthorizationModule;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.SecretKey;

public class SafetyModule {
    private static SafetyModule instance;
    public static SafetyModule getInstance(){
        if (instance == null) {
            instance = new SafetyModule();
        }
        return instance;
    }
    public String decryptString(String encryptedText, SecretKey key) throws Exception {
        return Decryptable.decryptString(encryptedText, key);
    }

    public String encryptString(String plainText, SecretKey key) throws Exception {
        return Encryptable.encryptString(plainText, key);
    }

    public SecretKey decryptKey(byte[] encrypted_key, String password) throws Exception {
        return Decryptable.decryptWithPassword(encrypted_key, password);
    }

    public byte[] encryptKey(SecretKey decrypted_key, String password) throws Exception {
        return Encryptable.encryptWithPassword(decrypted_key, password);
    }

    public String encodeToSha256(String base) {
        try {
            // Создаем экземпляр MessageDigest с алгоритмом SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Получаем массив байт из строки и вычисляем хеш
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));

            // Преобразуем массив байт в шестнадцатеричный формат
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
