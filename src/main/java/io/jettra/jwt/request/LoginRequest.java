package io.jettra.jwt.request;

import io.jettra.server.openapi.annotations.Schema;

public class LoginRequest {
    
    @Schema(description = "Username for authentication", example = "admin")
    private String username;
    
    @Schema(description = "Password for authentication", example = "secret")
    private String password;

    public LoginRequest() {}

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
