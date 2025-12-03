package com.pancreatitis.models;

public class RegistrationForm {
    private String login;
    private String fullName;
    private String email;
    // другие поля...

    public RegistrationForm(String login, String fullName, String email) {
        this.login = login;
        this.fullName = fullName;
        this.email = email;
    }

    public String getLogin() {
        return login;
    }

    public String toJson() {
        // Простая реализация JSON сериализации
        return String.format(
                "{\"login\":\"%s\",\"fullName\":\"%s\",\"email\":\"%s\"}",
                escapeJson(login),
                escapeJson(fullName),
                escapeJson(email)
        );
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
