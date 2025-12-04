package com.pancreatitis.modules.safety;

import com.pancreatitis.models.Patient;
import com.pancreatitis.models.Questionnaire;
import com.pancreatitis.modules.authorization.AuthorizationModule;

import javax.crypto.SecretKey;

public class SafetyModule {
    private static SafetyModule instance;
    public static SafetyModule getInstance(){
        if (instance == null) {
            instance = new SafetyModule();
        }
        return instance;
    }
    private String decryptString(String encryptedText, SecretKey key) throws Exception {
        return Decryptable.decryptString(encryptedText, key);
    }

    private String encryptString(String plainText, SecretKey key) throws Exception {
        return Encryptable.encryptString(plainText, key);
    }

    public SecretKey decryptKey(byte[] encrypted_key, String password) throws Exception {
        return Decryptable.decryptWithPassword(encrypted_key, password);
    }

    public byte[] encryptKey(SecretKey decrypted_key, String password) throws Exception {
        return Encryptable.encryptWithPassword(decrypted_key, password);
    }

    public void encryptQuestionnaire(Questionnaire questionnaire, SecretKey key) throws Exception {
        Patient patient = questionnaire.getPatient();
        String fio = patient.getFio();
        String ecnrypt_fio = encryptString(fio, key);
        patient.setFio(ecnrypt_fio);
    }

    public void decryptQuestionnaire(Questionnaire questionnaire, SecretKey key) throws Exception {
        Patient patient = questionnaire.getPatient();
        String ecnrypt_fio = patient.getFio();
        String fio = decryptString(ecnrypt_fio, key);
        patient.setFio(fio);
    }
}
