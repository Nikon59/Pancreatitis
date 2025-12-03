package com.pancreatitis;

import android.app.Application;

import com.pancreatitis.modules.safety.*;
import com.pancreatitis.modules.registration.*;
import com.pancreatitis.modules.prediction.*;
import com.pancreatitis.modules.database.*;
import com.pancreatitis.modules.cloudstorage.*;
import com.pancreatitis.modules.authorization.*;

public class Modules extends Application {
    private static Modules instance;

    // Объявляем все ваши модули
    private SafetyModule safetyModule;
    private RegistrationModule registrationModule;
    private DiagnosisPredictor diagnosisPredictor;
    private DatabaseModule databaseModule;
    private CloudStorageModule cloudStorageModule;
    private AuthorizationModule authorizationModule;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        initializeModules();
    }

    private void initializeModules() {
        databaseModule = new DatabaseModule(getApplicationContext());
        safetyModule = new SafetyModule();
        cloudStorageModule = new CloudStorageModule();
        authorizationModule = new AuthorizationModule(cloudStorageModule, safetyModule);
        diagnosisPredictor = new DiagnosisPredictor();
        registrationModule = new RegistrationModule(cloudStorageModule);
    }

    public static Modules getInstance() {
        return instance;
    }

    public SafetyModule getSafetyModule() {
        return safetyModule;
    }

    public RegistrationModule getRegistrationModule() {
        return registrationModule;
    }

    public DiagnosisPredictor getDiagnosisPredictor() {
        return diagnosisPredictor;
    }

    public DatabaseModule getDatabaseModule() {
        return databaseModule;
    }

    public CloudStorageModule getCloudStorageModule() {
        return cloudStorageModule;
    }

    public AuthorizationModule getAuthorizationModule() {
        return authorizationModule;
    }
}
