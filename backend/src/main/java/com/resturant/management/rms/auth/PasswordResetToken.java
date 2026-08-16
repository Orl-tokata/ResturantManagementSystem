package com.resturant.management.rms.auth;

import com.resturant.management.rms.user.UserInfm;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Backs the forgot-password → OTP → reset flow.
 *
 * <p>{@code otpCode} is the 6 digits emailed to the user; {@code token} is the
 * opaque value handed back after successful OTP verification and required by
 * {@code POST /api/auth/reset-password}.
 */
@Entity
@Table(name = "password_reset_token")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "otp_code", length = 10)
    private String otpCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_ref", nullable = false)
    private UserInfm user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(name = "used_yn", nullable = false, length = 1)
    private String usedYn = "N";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return "Y".equals(usedYn);
    }

    /** Valid means: issued, not yet consumed, and still inside its window. */
    public boolean isUsable() {
        return !isUsed() && !isExpired();
    }

    public void consume() {
        this.usedYn = "Y";
    }
}
