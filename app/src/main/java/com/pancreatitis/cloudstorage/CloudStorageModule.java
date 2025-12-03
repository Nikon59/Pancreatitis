package com.pancreatitis.cloudstorage;

import com.pancreatitis.models.RegistrationForm;
import com.pancreatitis.registration.RegistrationModule;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CloudStorageModule {
    private final String authToken = "y0__xC7koqFBxjIhTwg6O2OwBUwreWe7AeTLwUn1hxDxrN6Pp9NW10aTKlPGw";
    private static final String API_URL = "https://cloud-api.yandex.net/v1/disk/resources";

    public CloudStorageModule() {
        createFolders();
    }

    private void createFolders() {
        createFolder("/users/");
        createFolder("/update/");
        createFolder("/registration_requests/");
    }

    private boolean createFolder(String path) {
        try {
            String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString());
            String url = API_URL + "?path=" + encodedPath;

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Authorization", "OAuth " + authToken);
            connection.setRequestProperty("Content-Type", "application/json");

            int responseCode = connection.getResponseCode();
            connection.disconnect();

            // 201 - created, 409 - already exists (acceptable)
            return responseCode == 201 || responseCode == 409;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isUserFolderExists(String login) {
        try {
            String path = "/users/" + login;
            String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString());
            String url = API_URL + "?path=" + encodedPath;

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "OAuth " + authToken);

            int responseCode = connection.getResponseCode();
            connection.disconnect();

            // 200 - exists
            return responseCode == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public byte[] downloadUserKey(String login) throws Exception {
        String filePath = "/users/" + login + "/key_user.enc";
        String downloadUrl = getDownloadUrl(filePath);

        if (downloadUrl == null) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        return downloadFileAsBytes(downloadUrl);
    }

    private byte[] downloadFileAsBytes(String downloadUrl) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(downloadUrl).openConnection();
        connection.setRequestMethod("GET");

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

    public boolean uploadRegistrationRequest(RegistrationForm requestForm) {
        try {
            String fileName = requestForm.getLogin() + ".json";
            String filePath = "/registration_requests/" + fileName;

            // Получаем URL для загрузки
            String uploadUrl = getUploadUrl(filePath, true);
            if (uploadUrl == null) {
                return false;
            }

            // Конвертируем в JSON и загружаем
            String jsonData = requestForm.toJson();
            return uploadFile(uploadUrl, jsonData.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    private String getDownloadUrl(String path) throws Exception {
        String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString());
        String url = API_URL + "/download?path=" + encodedPath;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "OAuth " + authToken);

        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Парсим JSON для получения ссылки (упрощенный вариант)
            String responseStr = response.toString();
            int hrefIndex = responseStr.indexOf("\"href\":\"");
            if (hrefIndex != -1) {
                int start = hrefIndex + 8;
                int end = responseStr.indexOf("\"", start);
                return responseStr.substring(start, end);
            }
        }

        connection.disconnect();
        return null;
    }

    private String getUploadUrl(String path, boolean overwrite) throws Exception {
        String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString());
        String url = API_URL + "/upload?path=" + encodedPath + "&overwrite=" + overwrite;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "OAuth " + authToken);

        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String responseStr = response.toString();
            int hrefIndex = responseStr.indexOf("\"href\":\"");
            if (hrefIndex != -1) {
                int start = hrefIndex + 8;
                int end = responseStr.indexOf("\"", start);
                return responseStr.substring(start, end);
            }
        }

        connection.disconnect();
        return null;
    }

    private boolean uploadFile(String uploadUrl, byte[] data) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(uploadUrl).openConnection();
        connection.setRequestMethod("PUT");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/octet-stream");

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(data);
        }

        int responseCode = connection.getResponseCode();
        connection.disconnect();

        return responseCode == 201 || responseCode == 200;
    }
}