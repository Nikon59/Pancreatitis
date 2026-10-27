package com.pancreatitis.infrastructure.auth;

import android.content.Context;

import com.pancreatitis.infrastructure.debug.DebugPreferences;
import com.pancreatitis.models.Doctor;
import com.pancreatitis.models.User;
import com.pancreatitis.modules.database.DatabaseModule;

import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;

public final class AuthTestBridge {

    // ДЛЯ ПЕРЕКЛЮЧЕНИЯ НА БОЕВОЙ СЕРВЕР: установите значение false
    // либо удалите вызов данного моста из AuthenticationManager.
    public static boolean USE_TEST_AUTH = true;

    public static final String TEST_DOCTOR_FIO = "Тестовый Врач-исследователь";
    public static final String TEST_DOCTOR_LOGIN = "test_doctor";
    public static final String TEST_DOCTOR_ID = "00000000-0000-0000-0000-000000000001";

    private AuthTestBridge() {
    }

    public static boolean isTestAuthActive(Context context) {
        if (context != null) {
            DebugPreferences prefs = DebugPreferences.getInstance(context);
            return prefs.isMockAuthEnabled(USE_TEST_AUTH);
        }
        return USE_TEST_AUTH;
    }

    public static void setTestAuthActive(Context context, boolean active) {
        USE_TEST_AUTH = active;
        if (context != null) {
            DebugPreferences.getInstance(context).setMockAuthEnabled(active);
        }
    }

    public static void authenticateAsTestDoctor(Context context) {
        DatabaseModule databaseModule = DatabaseModule.getInstance();
        Doctor doctor = databaseModule.getDoctorByLogin(TEST_DOCTOR_LOGIN);
        if (doctor == null) {
            doctor = new Doctor();
            doctor.setLogin(TEST_DOCTOR_LOGIN);
            doctor.setFio(TEST_DOCTOR_FIO);
            doctor.setStatus(true);
            doctor.setEmail("test_doctor@med.local");
            doctor.setPhone("+79990000000");
            databaseModule.insertDoctor(doctor);
            doctor = databaseModule.getDoctorByLogin(TEST_DOCTOR_LOGIN);
        }

        User user = User.getInstance();
        user.setLogin(TEST_DOCTOR_LOGIN);
        user.setFio(TEST_DOCTOR_FIO);
        if (doctor != null) {
            user.setId(doctor.getId());
            user.setCreatedAt(doctor.getCreatedAt());
        } else {
            user.setId(1);
        }

        byte[] keyBytes = "pancreatitis_test_auth_key_32b__".getBytes(StandardCharsets.UTF_8);
        user.setKey(new SecretKeySpec(keyBytes, "AES"));
    }
}
