package com.igdm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class AuthDto {

    public static class RegisterRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        private String name;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class AuthResponse {
        private String token;
        private String tokenType = "Bearer";
        private UserProfileDto user;

        public AuthResponse(String token, UserProfileDto user) {
            this.token = token;
            this.user = user;
        }

        public String getToken() { return token; }
        public String getTokenType() { return tokenType; }
        public UserProfileDto getUser() { return user; }
    }

    public static class UserProfileDto {
        private UUID id;
        private String email;
        private String name;
        private String planTier;

        public UserProfileDto(UUID id, String email, String name, String planTier) {
            this.id = id;
            this.email = email;
            this.name = name;
            this.planTier = planTier;
        }

        public UUID getId() { return id; }
        public String getEmail() { return email; }
        public String getName() { return name; }
        public String getPlanTier() { return planTier; }
    }
}
