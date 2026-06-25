package com.jettra.jwt.request;

import com.jettra.server.openapi.annotations.Schema;

public class LoginResponse {
    
    @Schema(description = "The generated JWT token for authentication", example = "Bearer eyJhb...")
    private String token;

    public LoginResponse() {}

    public LoginResponse(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
