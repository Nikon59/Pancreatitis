package com.pancreatitis.models;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public static RegistrationForm fromJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, RegistrationForm.class);
    }
}
