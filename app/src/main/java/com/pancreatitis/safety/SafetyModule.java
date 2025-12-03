package safety;

import javax.crypto.SecretKey;

public class SafetyModule {
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
}
