package com.pancreatitis.safety;

import javax.crypto.SecretKey;

public class SafetyModule {
    public String decryptString(String encryptedText, SecretKey key) throws Exception {
        return safety.Decryptable.decryptString(encryptedText, key);
    }

    public String encryptString(String plainText, SecretKey key) throws Exception {
        return safety.Encryptable.encryptString(plainText, key);
    }

    public SecretKey decryptKey(byte[] encrypted_key, String password) throws Exception {
        return safety.Decryptable.decryptWithPassword(encrypted_key, password);
    }

    public byte[] encryptKey(SecretKey decrypted_key, String password) throws Exception {
        return safety.Encryptable.encryptWithPassword(decrypted_key, password);
    }
}
