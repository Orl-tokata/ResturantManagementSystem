package com.resturant.management.rms.auth.dto;

import com.resturant.management.rms.user.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Request and response payloads for {@code /api/auth}.
 *
 * <p>Grouped in one file because they are small, tightly related, and only ever
 * used by {@code AuthController}.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    /** Password policy shared by register, reset and change. */
    public static final String PASSWORD_PATTERN = "^(?=.*[A-Z])(?=.*\\d).{8,}$";
    public static final String PASSWORD_MESSAGE =
            "must be at least 8 characters and contain an uppercase letter and a number";

    @Schema(name = "LoginRequest")
    public record LoginRequest(
            @NotBlank(message = "{valid.required}") String username,
            @NotBlank(message = "{valid.required}") String password
    ) {}

    @Schema(name = "RegisterRequest")
    public record RegisterRequest(
            @NotBlank(message = "{valid.required}")
            @Size(min = 3, max = 50, message = "{valid.username}")
            String username,

            @NotBlank(message = "{valid.required}")
            @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE)
            String password,

            @NotBlank(message = "{valid.required}")
            @Size(max = 100)
            String fullName,

            @Email(message = "{valid.email}")
            String email,

            @Size(max = 30)
            String phone,

            Role role
    ) {}

    @Schema(name = "AuthResponse")
    public record AuthResponse(
            String accessToken,
            String tokenType,
            long expiresInSeconds,
            UserResponse user
    ) {
        public static AuthResponse of(String token, long expiresIn, UserResponse user) {
            return new AuthResponse(token, "Bearer", expiresIn, user);
        }
    }

    @Schema(name = "UserResponse")
    public record UserResponse(
            Long id,
            String username,
            String fullName,
            String email,
            String phone,
            Role role,
            boolean locked,
            LocalDateTime lastLoginAt
    ) {}

    /** Self-service profile edit. Role and username are deliberately absent. */
    @Schema(name = "UpdateProfileRequest")
    public record UpdateProfileRequest(
            @NotBlank(message = "{valid.required}") @Size(max = 100) String fullName,
            @Email(message = "{valid.email}") @Size(max = 120) String email,
            @Size(max = 30) String phone
    ) {}

    @Schema(name = "ForgotPasswordRequest")
    public record ForgotPasswordRequest(
            @NotBlank(message = "{valid.required}")
            @Email(message = "{valid.email}")
            String email
    ) {}

    @Schema(name = "VerifyOtpRequest")
    public record VerifyOtpRequest(
            @NotBlank(message = "{valid.required}") @Email String email,
            @NotBlank(message = "{valid.required}")
            @Pattern(regexp = "^\\d{6}$", message = "{valid.otp}")
            String code
    ) {}

    /** Returned by verify-otp; {@code resetToken} is required to set a new password. */
    @Schema(name = "VerifyOtpResponse")
    public record VerifyOtpResponse(String resetToken, LocalDateTime expiresAt) {}

    @Schema(name = "ResetPasswordRequest")
    public record ResetPasswordRequest(
            @NotBlank(message = "{valid.required}") String resetToken,
            @NotBlank(message = "{valid.required}")
            @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE)
            String newPassword
    ) {}

    @Schema(name = "ChangePasswordRequest")
    public record ChangePasswordRequest(
            @NotBlank(message = "{valid.required}") String currentPassword,
            @NotBlank(message = "{valid.required}")
            @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE)
            String newPassword
    ) {}
}
