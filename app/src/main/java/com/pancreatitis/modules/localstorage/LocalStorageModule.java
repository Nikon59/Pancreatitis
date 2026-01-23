package com.pancreatitis.modules.localstorage;

import android.content.Context;
import android.os.Environment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pancreatitis.models.Doctor;
import com.pancreatitis.models.Questionnaire;
import com.pancreatitis.models.RegistrationForm;
import com.pancreatitis.models.Update;
import com.pancreatitis.models.User;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class LocalStorageModule {
    private Context context;
    private static LocalStorageModule instance;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Базовый путь для хранения данных
    private File storageBaseDir;
    private static final String APP_DIR_NAME = "PancreatitisApp";
    private static final String USERS_DIR = "users";

    private LocalStorageModule(Context context) {
        this.context = context;
        initializeStorage();
    }

    public static LocalStorageModule getInstance(Context context) {
        if (instance == null) {
            synchronized (LocalStorageModule.class) {
                if (instance == null) {
                    instance = new LocalStorageModule(context);
                }
            }
        }
        return instance;
    }

    private void initializeStorage() {
        // Инициализация папок в фоне
        executorService.execute(() -> {
            createFolder(USERS_DIR);
        });
    }

    private File getStorageBaseDir() {
        if (storageBaseDir == null) {
            // Используем внешнее хранилище, если доступно, иначе внутреннее
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                storageBaseDir = new File(context.getExternalFilesDir(null), APP_DIR_NAME);
            } else {
                storageBaseDir = new File(context.getFilesDir(), APP_DIR_NAME);
            }
        }
        return storageBaseDir;
    }

    private boolean createFolder(String folderName) {
        File folder = new File(getStorageBaseDir(), folderName);
        if (!folder.exists()) {
            return folder.mkdirs();
        }
        return true;
    }

    private File getFolder(String folderName) {
        return new File(getStorageBaseDir(), folderName);
    }

    public boolean isUserFolderExists(String login) {
        try {
            Future<Boolean> future = executorService.submit(() -> {
                File userFolder = new File(getFolder(USERS_DIR), login);
                return userFolder.exists() && userFolder.isDirectory();
            });
            return future.get();
        } catch (Exception e) {
            return false;
        }
    }

    public byte[] downloadUserKey(String login) throws Exception {
        Callable<byte[]> task = () -> {
            File keyFile = new File(new File(getFolder(USERS_DIR), login), "key_user.enc");

            if (!keyFile.exists()) {
                throw new FileNotFoundException("File not found: " + keyFile.getAbsolutePath());
            }

            return readFileAsBytes(keyFile);
        };

        return executorService.submit(task).get();
    }

    public boolean isUserKeyExists(String login) throws Exception {
        Callable<Boolean> task = () -> {
            File keyFile = new File(new File(getFolder(USERS_DIR), login), "key_user.enc");
            return keyFile.exists();
        };

        return executorService.submit(task).get();
    }

    public User downloadUserInfo(String login) throws Exception {
        Callable<User> task = () -> {
            File userInfoFile = new File(new File(getFolder(USERS_DIR), login), "user_info.json");

            if (!userInfoFile.exists()) {
                throw new FileNotFoundException("User info not found: " + userInfoFile.getAbsolutePath());
            }

            byte[] jsonBytes = readFileAsBytes(userInfoFile);
            String json = new String(jsonBytes, StandardCharsets.UTF_8);

            return parseUserFromJson(json, login);
        };

        return executorService.submit(task).get();
    }

    private User parseUserFromJson(String json, String login) throws Exception {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(json);

            User user = User.getInstance();

            if (rootNode.has("email")) {
                user.setEmail(rootNode.get("email").asText());
            }

            if (rootNode.has("fullName")) {
                Doctor doctor = new Doctor();
                doctor.setFio(rootNode.get("fullName").asText());
                user.setDoctor(doctor);
            }

            if (rootNode.has("phone")) {
                user.setPhone(rootNode.get("phone").asText());
            }

            user.setLogin(login);

            return user;
        } catch (Exception e) {
            throw new IOException("Failed to parse user JSON: " + e.getMessage(), e);
        }
    }

    public byte[] getTrainingData() throws Exception {
        Callable<byte[]> task = () -> {
            File algorithmFile = new File(getStorageBaseDir(), "algorithm.txt");

            if (!algorithmFile.exists()) {
                throw new FileNotFoundException("File not found: " + algorithmFile.getAbsolutePath());
            }

            return readFileAsBytes(algorithmFile);
        };

        return executorService.submit(task).get();
    }

    public void downloadAlgorithmFile(byte[] algorithmData) throws Exception {
        Callable<Boolean> task = () -> {
            File algorithmFile = new File(getStorageBaseDir(), "algorithm.txt");
            return writeFile(algorithmFile, algorithmData);
        };

        executorService.submit(task).get();
    }

    // Вспомогательные методы для работы с файлами
    private byte[] readFileAsBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }

            return bos.toByteArray();
        }
    }

    private boolean writeFile(File file, byte[] data) throws IOException {
        // Создаем родительские директории, если их нет
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
            fos.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Метод для получения полного пути к файлу пользователя (для отладки)
    public String getUserFolderPath(String login) {
        File userFolder = new File(getFolder(USERS_DIR), login);
        return userFolder.getAbsolutePath();
    }

    // Метод для проверки доступности хранилища
    public boolean isStorageAvailable() {
        return getStorageBaseDir() != null && getStorageBaseDir().canWrite();
    }

    // Метод для очистки ресурсов
    public void shutdown() {
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    // Метод для удаления данных пользователя
    public boolean deleteUserData(String login) throws Exception {
        Callable<Boolean> task = () -> {
            File userFolder = new File(getFolder(USERS_DIR), login);
            return deleteRecursive(userFolder);
        };

        return executorService.submit(task).get();
    }

    private boolean deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        return fileOrDirectory.delete();
    }
}