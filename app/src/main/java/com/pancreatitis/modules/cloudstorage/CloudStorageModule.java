package com.pancreatitis.modules.cloudstorage;

import android.content.Context;
import android.util.Log;

import com.pancreatitis.BuildConfig;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pancreatitis.models.Doctor;
import com.pancreatitis.models.Patient;
import com.pancreatitis.models.Questionnaire;
import com.pancreatitis.models.RegistrationForm;
import com.pancreatitis.models.Update;
import com.pancreatitis.models.User;
import com.pancreatitis.modules.safety.SafetyModule;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.crypto.SecretKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class CloudStorageModule {
    private final String authToken = BuildConfig.API_KEY;
    private static final String API_URL = "https://cloud-api.yandex.net/v1/disk/resources";
    private static CloudStorageModule instance;
    private static SafetyModule safetyModule;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
    // Константы для работы с алгоритмом и подписью
    private static final String ALGORITHM_FILE_PREFIX = "algorithm_";
    private static final String ALGORITHM_FILE_SUFFIX = ".txt";
    private static final String SIGNATURE_SUFFIX = ".sig";
    private static final String PUBLIC_KEY_ASSET = "ed25519_public.key";
    private Context context;

    private CloudStorageModule() {
        // Инициализация папок в фоне
        executorService.execute(() -> {
            createFolder("/users/");
            createFolder("/update/");
            createFolder("/registration_requests/");
        });
        safetyModule = SafetyModule.getInstance();
    }

    public static CloudStorageModule getInstance() {
        if (instance == null) {
            synchronized (CloudStorageModule.class) {
                if (instance == null) {
                    instance = new CloudStorageModule();
                }
            }
        }
        return instance;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    // Регистрация контекста для использования публичного ключа
    public Context getContext() {
        return context;
    }

    private boolean createFolder(String path) {
        try {
            String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString());
            String url = API_URL + "?path=" + encodedPath;

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Authorization", "OAuth " + authToken);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            connection.disconnect();

            // 201 - created, 409 - already exists (acceptable)
            return responseCode == 201 || responseCode == 409;
        } catch (Exception e) {
            return false;
        }
    }

    // Старые публичные методы с той же сигнатурой
    public boolean isUserFolderExists(String login) {
        try {
            Future<Boolean> future = executorService.submit(() -> {
                String path = "/users/" + login;
                String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString());
                String url = API_URL + "?path=" + encodedPath;

                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", "OAuth " + authToken);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                connection.disconnect();

                // 200 - exists
                return responseCode == 200;
            });

            return future.get(); // Блокируем до получения результата
        } catch (Exception e) {
            return false;
        }
    }

    public byte[] downloadUserKey(String login) throws Exception {
        Callable<byte[]> task = () -> {
            String filePath = "/users/" + login + "/key_user.enc";
            String downloadUrl = getDownloadUrl(filePath);

            if (downloadUrl == null) {
                throw new FileNotFoundException("File not found: " + filePath);
            }

            return downloadFileAsBytes(downloadUrl);
        };

        return executorService.submit(task).get();
    }

    public boolean uploadQuestionnaire(Update update, String login) throws Exception {
        Callable<Boolean> task = () -> {
            String datetime = LocalDateTime.now().format(formatter);
            String fileName = login + "_" + UUID.randomUUID().toString() + ".json";
            String filePath = "/update/" + fileName;

            update.setLogin(login);
            update.setDateUpload(datetime);

            String jsonData = encryptUpdate(update);
            String uploadUrl = getUploadUrl(filePath, true);
            if (uploadUrl == null) {
                System.err.println("Не удалось получить URL для загрузки файла: " + filePath);
                return false;
            }

            return uploadFile(uploadUrl, jsonData.getBytes(StandardCharsets.UTF_8));
        };

        return executorService.submit(task).get();
    }

    private String encryptUpdate(Update update) throws Exception {
        SecretKey key = User.getInstance().getKey();
        return safetyModule.encryptString(update.toJson(), key);
    }

    public boolean uploadRegistrationRequest(RegistrationForm registrationForm) throws Exception {
        Callable<Boolean> task = () -> {
            String jsonData = registrationForm.toJson();

            String fileName = registrationForm.getLogin() + ".json";
            String filePath = "/registration_requests/" + fileName;

            String uploadUrl = getUploadUrl(filePath, true);
            if (uploadUrl == null) {
                System.err.println("Не удалось получить URL для загрузки файла: " + filePath);
                return false;
            }

            return uploadFile(uploadUrl, jsonData.getBytes(StandardCharsets.UTF_8));
        };

        return executorService.submit(task).get();
    }

    // ---------------------------------------------------------------------------
    // Методы для работы с алгоритмом и подписью
    // ---------------------------------------------------------------------------

    /**
     * Загружает содержимое текущего файла алгоритма из облака.
     */
    public byte[] downloadTrainingData() throws Exception {
        Callable<byte[]> task = () -> {
            String fileName = findAlgorithmFileNameInCloud();
            if (fileName == null) {
                throw new FileNotFoundException("Algorithm file not found in cloud root");
            }
            String filePath = "/" + fileName;
            String downloadUrl = getDownloadUrl(filePath);
            if (downloadUrl == null) {
                throw new IOException("Failed to obtain download URL for algorithm file");
            }
            return downloadFileAsBytes(downloadUrl);
        };
        return executorService.submit(task).get();
    }

    /**
     * Загружает подпись для текущего файла алгоритма.
     */
    public byte[] downloadTrainingDataSignature() throws Exception {
        Callable<byte[]> task = () -> {
            String algoFileName = findAlgorithmFileNameInCloud();
            if (algoFileName == null) {
                throw new FileNotFoundException("Algorithm file not found, cannot determine signature file name");
            }
            String sigFileName = algoFileName + SIGNATURE_SUFFIX;
            String filePath = "/" + sigFileName;
            String downloadUrl = getDownloadUrl(filePath);
            if (downloadUrl == null) {
                throw new FileNotFoundException("Отсутствует цифровая подпись: " + sigFileName);
            }
            return downloadFileAsBytes(downloadUrl);
        };
        return executorService.submit(task).get();
    }

    /**
     * Проверяет Ed25519-подпись данных алгоритма.
     * Публичный ключ загружается из assets приложения (файл с именем PUBLIC_KEY_ASSET).
     */
    public boolean verifyTrainingDataSignature(byte[] trainingData, byte[] signatureBytes) throws Exception {
        if (context == null) {
            throw new IllegalStateException("Context is not set. Call setContext() before verifying signature.");
        }
        // Загружаем публичный ключ из assets
        PublicKey publicKey = loadPublicKeyFromAssets(PUBLIC_KEY_ASSET);

        Signature sig = Signature.getInstance("Ed25519");
        sig.initVerify(publicKey);
        sig.update(trainingData);
        return sig.verify(signatureBytes);
    }

    /**
     * Извлекает временную метку из имени текущего файла алгоритма в облаке.
     */
    public LocalDateTime getTrainingDataTimestamp() throws Exception {
        String fileName = findAlgorithmFileNameInCloud();
        if (fileName == null) {
            throw new FileNotFoundException("Файл алгоритм отсутствует в облаке");
        }
        return parseDateTimeFromAlgorithmFileName(fileName);
    }

    // Вспомогательные методы для поиска и парсинга

    /**
     * Ищет в корне облачного диска файл, начинающийся с algorithm_ и заканчивающийся .txt.
     */
    private String findAlgorithmFileNameInCloud() throws Exception {
        // Получаем список файлов в корневой папке
        String url = API_URL + "?path=%2F&limit=100"; // path=/ закодирован
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "OAuth " + authToken);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    // Парсим JSON ответа: ищем элементы с "name", начинающиеся с algorithm_ и заканчивающиеся .txt
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(response.toString());
                    JsonNode items = root.path("_embedded").path("items");
                    if (items.isArray()) {
                        for (JsonNode item : items) {
                            String name = item.path("name").asText();
                            if (name != null && name.startsWith(ALGORITHM_FILE_PREFIX) && name.endsWith(ALGORITHM_FILE_SUFFIX)) {
                                return name;
                            }
                        }
                    }
                }
            }
            return null;
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Извлекает дату и время из имени файла алгоритма (например, "algorithm_2026_04_24_12_30_00.txt").
     */
    private LocalDateTime parseDateTimeFromAlgorithmFileName(String fileName) {
        if (fileName == null || !fileName.startsWith(ALGORITHM_FILE_PREFIX) || !fileName.endsWith(ALGORITHM_FILE_SUFFIX)) {
            throw new IllegalArgumentException("Некорректное имя файла алгоритма: " + fileName);
        }
        String dateTimeStr = fileName.substring(ALGORITHM_FILE_PREFIX.length(), fileName.length() - ALGORITHM_FILE_SUFFIX.length());
        return LocalDateTime.parse(dateTimeStr, formatter);
    }

    /**
     * Загружает публичный ключ Ed25519 из assets приложения.
     */
    private PublicKey loadPublicKeyFromAssets(String assetFileName) throws Exception {
        try (InputStream is = context.getAssets().open(assetFileName)) {
            byte[] keyBytes = readAllBytes(is);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            Security.removeProvider("BC");
            Security.insertProviderAt(new BouncyCastleProvider(), 1);
            KeyFactory kf = KeyFactory.getInstance("ED25519", "BC");
            return kf.generatePublic(spec);
        }
    }

    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int n;
        while ((n = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, n);
        }
        return buffer.toByteArray();
    }

    /**
     * Загружает зашифрованный архив в папку fetch/upload.
     * @param encryptedData зашифрованные байты архива
     * @param fileName имя файла (например, "archive_2025_04_05_10_30_00.zip.enc")
     * @return true при успехе
     */
    public boolean uploadArchive(byte[] encryptedData, String fileName) throws Exception {
        String folderPath = "/fetch/upload";
        String filePath = folderPath + "/" + fileName;
        String uploadUrl = getUploadUrl(filePath, true);
        if (uploadUrl == null) {
            throw new IOException("Не удалось получить URL для загрузки архива");
        }
        return uploadFile(uploadUrl, encryptedData);
    }

    /**
     * Создаёт пустой файл-триггер в папке fetch/requests
     * @param login логин пользователя
     * @param timestamp временная метка (формат yyyy_MM_dd_HH_mm_ss)
     * @return true при успехе
     */
    public boolean createRequestTrigger(String login, String timestamp) throws Exception {
        String folderPath = "/fetch/requests";
        String fileName = login + "_" + timestamp + ".req";
        String filePath = folderPath + "/" + fileName;
        String uploadUrl = getUploadUrl(filePath, true);
        if (uploadUrl == null) return false;
        return uploadFile(uploadUrl, new byte[0]);
    }

    /**
     * Получает список файлов в папке fetch/download, содержащих указанный логин
     * @param login логин пользователя
     * @return список имён файлов (полные пути)
     */
    public List<String> listDownloadFilesForUser(String login) throws Exception {
        List<String> result = new ArrayList<>();
        String folderPath = "/fetch/download";
        String encodedPath = URLEncoder.encode(folderPath, StandardCharsets.UTF_8.toString());
        String url = API_URL + "?path=" + encodedPath + "&limit=100";

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "OAuth " + authToken);

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);

                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(response.toString());
                    JsonNode items = root.path("_embedded").path("items");
                    if (items.isArray()) {
                        for (JsonNode item : items) {
                            String name = item.path("name").asText();
                            if (name != null && name.contains(login) && name.endsWith(".zip.enc")) {
                                result.add(folderPath + "/" + name);
                            }
                        }
                    }
                }
            } else if (responseCode == 404) {
                // Папка не существует – возвращаем пустой список
                return result;
            }
        } finally {
            connection.disconnect();
        }
        return result;
    }

    /**
     * Скачивает файл по полному пути и возвращает его байты
     */
    public byte[] downloadFile(String filePath) throws Exception {
        String downloadUrl = getDownloadUrl(filePath);
        if (downloadUrl == null) throw new FileNotFoundException("Файл не найден: " + filePath);
        return downloadFileAsBytes(downloadUrl);
    }

    /**
     * Удаляет файл из облака (опционально)
     */
    public boolean deleteFile(String filePath) {
        try {
            String encodedPath = URLEncoder.encode(filePath, StandardCharsets.UTF_8.toString());
            String url = API_URL + "?path=" + encodedPath;
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("DELETE");
            connection.setRequestProperty("Authorization", "OAuth " + authToken);
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            return responseCode == 202 || responseCode == 204;
        } catch (Exception e) {
            return false;
        }
    }
    public boolean fileExists(String path) throws Exception {
        String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString());
        String url = API_URL + "?path=" + encodedPath;
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "OAuth " + authToken);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        try {
            int responseCode = connection.getResponseCode();
            return responseCode == 200;
        } finally {
            connection.disconnect();
        }
    }

    // Вспомогательные методы (остаются теми же)
    private byte[] downloadFileAsBytes(String downloadUrl) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(downloadUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (InputStream inputStream = connection.getInputStream();
                     ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    return outputStream.toByteArray();
                }
            } else {
                throw new IOException("Failed to download file. Response code: " + responseCode);
            }
        } finally {
            connection.disconnect();
        }
    }

    private String getDownloadUrl(String path) throws Exception {
        String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString());
        String url = API_URL + "/download?path=" + encodedPath;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "OAuth " + authToken);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    // Парсим JSON для получения ссылки
                    String responseStr = response.toString();
                    int hrefIndex = responseStr.indexOf("\"href\":\"");
                    if (hrefIndex != -1) {
                        int start = hrefIndex + 8;
                        int end = responseStr.indexOf("\"", start);
                        return responseStr.substring(start, end);
                    }
                }
            }
            return null;
        } finally {
            connection.disconnect();
        }
    }

    private String getUploadUrl(String path, boolean overwrite) throws Exception {
        String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString());
        String url = API_URL + "/upload?path=" + encodedPath + "&overwrite=" + overwrite;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "OAuth " + authToken);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    String responseStr = response.toString();
                    int hrefIndex = responseStr.indexOf("\"href\":\"");
                    if (hrefIndex != -1) {
                        int start = hrefIndex + 8;
                        int end = responseStr.indexOf("\"", start);
                        return responseStr.substring(start, end);
                    }
                }
            }
            return null;
        } finally {
            connection.disconnect();
        }
    }

    private boolean uploadFile(String uploadUrl, byte[] data) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(uploadUrl).openConnection();
        connection.setRequestMethod("PUT");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/octet-stream");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(data);
            outputStream.flush();
        }

        int responseCode = connection.getResponseCode();
        connection.disconnect();

        return responseCode == 201 || responseCode == 200;
    }

    // Метод для очистки ресурсов
    public void shutdown() {
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}