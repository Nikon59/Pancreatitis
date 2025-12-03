package com.pancreatitis.modules.authorization;

import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.safety.SafetyModule;

import javax.crypto.SecretKey;

public class AuthorizationModule {
    CloudStorageModule cloudStorageModule;
    SafetyModule safetyModule;
    public AuthorizationModule(CloudStorageModule cloudStorageModule, SafetyModule safetyModule){
        cloudStorageModule = cloudStorageModule;
        safetyModule = safetyModule;
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
