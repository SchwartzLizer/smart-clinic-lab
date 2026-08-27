package com.project.back_end.DTO;
import jakarta.validation.constraints.NotBlank;
public class Login {
    @NotBlank private String identifier;
    @NotBlank private String password;
    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }
    public String getEmail() { return identifier; }
    public void setEmail(String email) { this.identifier = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
