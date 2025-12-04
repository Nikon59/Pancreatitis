package com.pancreatitis.modules.authorization;

import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.safety.SafetyModule;

import javax.crypto.SecretKey;

public class AuthorizationModule {
    private static CloudStorageModule cloudStorageModule;
    private static SafetyModule safetyModule;
    private static AuthorizationModule instance;
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
    public SecretKey authenticateUser(String login, String password) throws Exception {
        if (!cloudStorageModule.isUserFolderExists(login)){
            return null;
        }

        byte[] encrypted_key = cloudStorageModule.downloadUserKey(login);
        SecretKey key = safetyModule.decryptKey(encrypted_key, password);
        return key;
    }
}
