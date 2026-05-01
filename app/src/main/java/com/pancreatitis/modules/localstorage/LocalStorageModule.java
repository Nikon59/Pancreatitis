package com.pancreatitis.modules.localstorage;

import android.content.Context;
import android.os.Environment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pancreatitis.models.Doctor;
import com.pancreatitis.models.Questionnaire;
import com.pancreatitis.models.RegistrationForm;
import com.pancreatitis.models.Update;
import com.pancreatitis.models.User;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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

    private LocalStorageModule() {
        initializeStorage();
    }

    public static LocalStorageModule getInstance() {
        if (instance == null) {
            synchronized (LocalStorageModule.class) {
                if (instance == null) {
                    instance = new LocalStorageModule();
                }
            }
        }
        return instance;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    private void initializeStorage() {
        // Инициализация папок в фоне
        executorService.execute(() -> {
            createFolder(USERS_DIR);
        });
    }

    private File getStorageBaseDir() {
        if (storageBaseDir == null) {
            storageBaseDir = new File(context.getFilesDir(), APP_DIR_NAME);
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

    public boolean createUserFolder(String login) {
        try {
            Future<Boolean> future = executorService.submit(() -> {
                File userFolder = new File(getFolder(USERS_DIR), login);

                if (userFolder.exists()) {
                    return true;
                }

                boolean created = userFolder.mkdirs();

                if (created) {
                    System.out.println("Папка пользователя создана: " + userFolder.getAbsolutePath());
                } else {
                    System.out.println("Не удалось создать папку пользователя: " + login);
                }

                return created;
            });

            return future.get();
        } catch (Exception e) {
            return false;
        }
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

    public boolean saveLocalUserKey(String login, byte[] keyData) throws Exception {
        File userFolder = new File(getFolder(USERS_DIR), login);

        if (!userFolder.exists()) {
            if (!userFolder.mkdirs()) {
                throw new IOException("Не удалось создать папку пользователя: " + login);
            }
        }

        File keyFile = new File(userFolder, "key_user.enc");

        try (FileOutputStream fos = new FileOutputStream(keyFile)) {
            fos.write(keyData);
            fos.flush();
            return true;
        } catch (IOException e) {
            throw new IOException("Ошибка при сохранении ключа: " + e.getMessage(), e);
        }
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

    public boolean saveUserInfo(User user) throws Exception {
        Callable<Boolean> task = () -> {
            if (user == null || user.getLogin() == null || user.getLogin().isEmpty()) {
                throw new IllegalArgumentException("User or login cannot be null or empty");
            }

            // Создаем папку пользователя, если её нет
            File userFolder = new File(getFolder(USERS_DIR), user.getLogin());
            if (!userFolder.exists()) {
                if (!userFolder.mkdirs()) {
                    throw new IOException("Failed to create user folder: " + user.getLogin());
                }
            }

            // Создаем объект для сериализации в JSON
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode userJson = mapper.createObjectNode();

            // Заполняем JSON данными из объекта User
            if (user.getEmail() != null) {
                userJson.put("email", user.getEmail());
            }

            if (user.getDoctor() != null && user.getDoctor().getFio() != null) {
                userJson.put("fullName", user.getDoctor().getFio());
            }

            if (user.getPhone() != null) {
                userJson.put("phone", user.getPhone());
            }

            userJson.put("login", user.getLogin());
            userJson.put("updatedAt", LocalDateTime.now().format(formatter));

            // Конвертируем в JSON строку
            String jsonData = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(userJson);

            // Сохраняем в файл
            File userInfoFile = new File(userFolder, "user_info.json");
            return writeFile(userInfoFile, jsonData.getBytes(StandardCharsets.UTF_8));
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

    /**
     * Загружает словарь BIP39 из assets приложения (файл wordlist_en.txt).
     * Возвращает список из 2048 слов.
     */
    public List<String> loadBip39WordList() throws Exception {
        if (context == null) throw new IllegalStateException("Context is not set");
        Callable<List<String>> task = () -> {
            List<String> words = new ArrayList<>(2048);
            try (InputStream is = context.getAssets().open("wordlist_en.txt");
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String word = line.trim();
                    if (!word.isEmpty()) words.add(word);
                }
            }
            if (words.size() != 2048) {
                throw new IllegalArgumentException("Invalid BIP39 word list size: " + words.size() + ", expected 2048");
            }
            return words;
        };
        return executorService.submit(task).get();
    }

    // ---------------------------------------------------------------------------
    // Методы для работы с алгоритмом
    // ---------------------------------------------------------------------------
    public byte[] getTrainingData() throws Exception {
        Callable<byte[]> task = () -> {
            File algorithmFile = findAlgorithmFile();
            if (algorithmFile == null) {
                throw new FileNotFoundException("Algorithm file not found");
            }
            return readFileAsBytes(algorithmFile);
        };
        return executorService.submit(task).get();
    }

    public boolean isTrainingData() throws Exception {
        Callable<Boolean> task = () -> findAlgorithmFile() != null;
        return executorService.submit(task).get();
    }

    public void downloadAlgorithmFile(byte[] algorithmData, String fileName) throws Exception {
        Callable<Boolean> task = () -> {
            deleteAllAlgorithmFiles();
            File algorithmFile = new File(getStorageBaseDir(), fileName);
            return writeFile(algorithmFile, algorithmData);
        };
        executorService.submit(task).get();
    }

    private File findAlgorithmFile() {
        File[] files = getStorageBaseDir().listFiles((dir, name) -> name.startsWith("algorithm_") && name.endsWith(".txt"));
        if (files == null || files.length == 0) {
            return null;
        }
        // Если больше одного, можно вернуть первый или выбросить ошибку. Возьмем первый, т.к. по идее должен быть один.
        return files[0];
    }

    private void deleteAllAlgorithmFiles() {
        File[] files = getStorageBaseDir().listFiles((dir, name) -> name.startsWith("algorithm") && name.endsWith(".txt"));
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
    }

    public void copyAlgorithmFromAssets(String assetFileName) throws Exception {
        // Проверить, что context не null
        if (context == null) throw new IllegalStateException("Context is not set");

        executorService.submit(() -> {
            // Удалить старые файлы алгоритма
            deleteAllAlgorithmFiles();

            // Скопировать из assets
            File destFile = new File(getStorageBaseDir(), assetFileName);
            try (InputStream is = context.getAssets().open(assetFileName);
                 FileOutputStream fos = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[4096];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
                fos.flush();
            }
            return true;
        }).get();
    }

    public LocalDateTime getAlgorithmFileDateTime() throws Exception {
        File algoFile = findAlgorithmFile();
        if (algoFile == null) {
            throw new FileNotFoundException("Algorithm file not found");
        }
        return parseDateTimeFromFilename(algoFile.getName());
    }

    private LocalDateTime parseDateTimeFromFilename(String filename) {
        String prefix = "algorithm_";
        String suffix = ".txt";
        if (!filename.startsWith(prefix) || !filename.endsWith(suffix)) {
            throw new IllegalArgumentException("Invalid algorithm filename: " + filename);
        }
        String dateTimeStr = filename.substring(prefix.length(), filename.length() - suffix.length());
        DateTimeFormatter fileFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
        return LocalDateTime.parse(dateTimeStr, fileFormatter);
    }

    // ---------------------------------------------------------------------------
    // Вспомогательные методы для работы с файлами
    // ---------------------------------------------------------------------------
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