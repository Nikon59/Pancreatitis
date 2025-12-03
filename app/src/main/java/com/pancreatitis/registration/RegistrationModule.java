package registration;

import cloudstorage.CloudStorageModule;

public class RegistrationModule {
    public boolean submitRegistrationRequest(Main.RegistrationForm registrationForm){
        CloudStorageModule cloudStorageModule = new CloudStorageModule();
        return cloudStorageModule.uploadRegistrationRequest(registrationForm);
    }
}

