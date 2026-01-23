package com.pancreatitis.modules.authorization;

import android.content.Context;

import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.safety.SafetyModule;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.crypto.SecretKey;

public class AuthorizationModule {
    private static CloudStorageModule cloudStorageModule;
    private static SafetyModule safetyModule;
    private static AuthorizationModule instance;
    private Context context;
    private AuthorizationModule(){
        cloudStorageModule = CloudStorageModule.getInstance();
        safetyModule = SafetyModule.getInstance();
    }
    public static AuthorizationModule getInstance(){
        if (instance == null) {
            instance = new AuthorizationModule();
        }
        return instance;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }
    public SecretKey authenticateUser(String login, String password) throws Exception {
//        if (!cloudStorageModule.isUserFolderExists(login)){
//            return null;
//        }

        byte[] encrypted_key = getKey(login);
        SecretKey key = safetyModule.decryptKey(encrypted_key, password);
        return key;
    }

    private byte[] getKey(String login) throws Exception {
        byte[] key = loadFileFromInternalStorage(login + "_key_user.enc");
        if (key == null) {
            key = cloudStorageModule.downloadUserKey(login);
            saveFileToInternalStorage(key, login + "_key_user.enc");
        }
        return key;
    }

    private void saveFileToInternalStorage(byte[] data, String fileName) throws Exception {
        File filesDir = context.getFilesDir();
        File outputFile = new File(filesDir, fileName);

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(data);
            fos.flush();
        }
    }

    private byte[] loadFileFromInternalStorage(String fileName) throws Exception {
        File filesDir = context.getFilesDir();
        File file = new File(filesDir, fileName);

        // Проверяем существование файла
        if (!file.exists() || file.length() == 0) {
            return null;
        }

        try (FileInputStream inputStream = new FileInputStream(file);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            return outputStream.toByteArray();
        }
    }
}
