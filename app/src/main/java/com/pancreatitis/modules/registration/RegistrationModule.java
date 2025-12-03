package com.pancreatitis.modules.registration;

import com.pancreatitis.models.RegistrationForm;

import com.pancreatitis.modules.cloudstorage.CloudStorageModule;

public class RegistrationModule {
    CloudStorageModule cloudStorageModule;
    public RegistrationModule(CloudStorageModule cloudStorageModule){
        cloudStorageModule = cloudStorageModule;
    }
    public boolean submitRegistrationRequest(RegistrationForm registrationForm) throws Exception {
        return cloudStorageModule.uploadRegistrationRequest(registrationForm);
    }
}

