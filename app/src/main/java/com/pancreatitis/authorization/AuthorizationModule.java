package authorization;

import cloudstorage.CloudStorageModule;
import safety.SafetyModule;

import javax.crypto.SecretKey;

public class AuthorizationModule {
    public SecretKey authenticateUser(String login, String password) throws Exception {
        CloudStorageModule cloudStorageModule = new CloudStorageModule();
        SafetyModule safetyModule = new SafetyModule();

        if (!cloudStorageModule.isUserFolderExists(login)){
            return null;
        }

        byte[] encrypted_key = cloudStorageModule.downloadUserKey(login);
        SecretKey key = safetyModule.decryptKey(encrypted_key, password);
        return key;
    }
}
