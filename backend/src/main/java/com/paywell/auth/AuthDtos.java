package com.paywell.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}

    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).+$";
    public static final String PASSWORD_MESSAGE = "Password must be 10-72 characters and contain a letter and a digit";

    public record SignupRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 10, max = 72, message = PASSWORD_MESSAGE) @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MESSAGE) String password,
            @NotBlank @Size(min = 2, max = 120) String fullName,
            @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "phone must be 7-15 digits, optionally starting with +") String phone,
            @Pattern(regexp = "^[A-Za-z0-9._-]{3,40}$", message = "handle must be 3-40 characters: letters, digits . _ -") String handle) {}

    public record LoginRequest(@NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(max = 72) String password) {}

    public record RefreshRequest(@NotBlank @Size(max = 200) String refreshToken) {}

    public record ForgotPasswordRequest(@NotBlank @Email @Size(max = 254) String email) {}

    public record ResetPasswordRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "code must be 6 digits") String code,
            @NotBlank @Size(min = 10, max = 72, message = PASSWORD_MESSAGE) @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MESSAGE) String newPassword) {}

    public record UserSummary(UUID id, String email, String fullName, String handle) {}

    public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresInSeconds, UserSummary user) {}

    /** Same shape whether or not the account exists (no user enumeration). deliveryConfigured is a global fact, not per-account. */
    public record ForgotPasswordResponse(boolean accepted, boolean deliveryConfigured, String message) {}
}
