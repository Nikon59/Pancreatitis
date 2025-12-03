package com.pancreatitis.registration;

import com.pancreatitis.models.RegistrationForm;

import com.pancreatitis.cloudstorage.CloudStorageModule;

public class RegistrationModule {
    public boolean submitRegistrationRequest(RegistrationForm registrationForm){
        CloudStorageModule cloudStorageModule = new CloudStorageModule();
        return cloudStorageModule.uploadRegistrationRequest(registrationForm);
    }
}

