package com.resturant.management.rms.auth;

import com.resturant.management.rms.auth.dto.AuthDtos.*;
import com.resturant.management.rms.common.exception.BadRequestException;
import com.resturant.management.rms.common.exception.ConflictException;
import com.resturant.management.rms.user.Role;
import com.resturant.management.rms.user.UserInfm;
import com.resturant.management.rms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int OTP_VALID_MINUTES = 10;
    private static final int RESET_TOKEN_VALID_MINUTES = 15;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;

    @Value("${app.security.default-biz-key:RMS}")
    private String defaultBizKey;

    /* ===================================================================== */
    /* Registration                                                          */
    /* ===================================================================== */

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUserId(request.username())) {
            throw new ConflictException("Username already taken: " + request.username());
        }
        if (request.email() != null && userRepository.existsByEml(request.email())) {
            throw new ConflictException("Email already registered: " + request.email());
        }

        UserInfm user = UserInfm.builder()
                .bizKey(generateBizKey())
                .userId(request.username())
                .userNm(request.fullName())
                .userPwd(passwordEncoder.encode(request.password()))
                .eml(request.email())
                .tel(request.phone())
                .role(request.role() != null ? request.role() : Role.CASHIER)
                .lockYn("N")
                .loginFailedCnt(0)
                .actYn("Y")
                .regId("self-register")
                .regDtm(LocalDateTime.now())
                .build();

        return toResponse(userRepository.save(user));
    }

    /* ===================================================================== */
    /* Login                                                                 */
    /* ===================================================================== */

    /**
     * Authenticates manually rather than through {@code AuthenticationManager}
     * so the failed-attempt counter and lockout on {@link UserInfm} are actually
     * maintained — a plain {@code DaoAuthenticationProvider} would not touch them.
     */
    @Transactional
    public LoginResult login(LoginRequest request) {
        UserInfm user = userRepository.findByUserId(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (user.isLocked()) {
            throw new LockedException(
                    "Account locked after too many failed attempts. Contact an administrator.");
        }
        if (!user.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.password(), user.getUserPwd())) {
            user.incrementFailedLoginAttempts();
            userRepository.save(user);
            log.warn("Failed login for '{}' (attempt {})", user.getUserId(), user.getLoginFailedCnt());
            throw new BadCredentialsException("Invalid username or password");
        }

        user.resetFailedLoginAttempts();
        user.setLstLgnDtm(LocalDateTime.now());
        userRepository.save(user);

        String access = jwtService.generateAccessToken(user.getUserId(), user.getRole().name());
        String refresh = jwtService.generateRefreshToken(user.getUserId());

        return new LoginResult(
                AuthResponse.of(access, jwtService.getAccessExpirationSeconds(), toResponse(user)),
                refresh);
    }

    /** Access token for the response body, refresh token for the httpOnly cookie. */
    public record LoginResult(AuthResponse body, String refreshToken) {}

    /* ===================================================================== */
    /* Refresh                                                               */
    /* ===================================================================== */

    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        if (refreshToken == null || !jwtService.isValid(refreshToken, false)) {
            throw new BadRequestException("Refresh token is missing, invalid or expired");
        }

        String username = jwtService.extractUsername(refreshToken);
        UserInfm user = userRepository.findByUserId(username)
                .orElseThrow(() -> new BadRequestException("Refresh token no longer valid"));

        if (!user.isEnabled() || user.isLocked()) {
            throw new BadRequestException("Account is no longer active");
        }

        String access = jwtService.generateAccessToken(user.getUserId(), user.getRole().name());
        return AuthResponse.of(access, jwtService.getAccessExpirationSeconds(), toResponse(user));
    }

    /* ===================================================================== */
    /* Forgot / reset password                                               */
    /* ===================================================================== */

    /**
     * Always reports success, even for an unknown address — otherwise this
     * endpoint becomes a way to enumerate registered email addresses.
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEml(request.email()).ifPresentOrElse(user -> {
            tokenRepository.invalidateAllForUser(user);

            String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
            PasswordResetToken token = PasswordResetToken.builder()
                    .token(UUID.randomUUID().toString())
                    .otpCode(code)
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusMinutes(OTP_VALID_MINUTES))
                    .usedYn("N")
                    .createdAt(LocalDateTime.now())
                    .build();
            tokenRepository.save(token);

            mailService.sendOtp(user.getEml(), code, OTP_VALID_MINUTES);
        }, () -> log.info("Password reset requested for unknown email — responding as success"));
    }

    @Transactional
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        UserInfm user = userRepository.findByEml(request.email())
                .orElseThrow(() -> new BadRequestException("Invalid code"));

        PasswordResetToken token = tokenRepository
                .findFirstByUserAndOtpCodeAndUsedYnOrderByCreatedAtDesc(user, request.code(), "N")
                .orElseThrow(() -> new BadRequestException("Invalid code"));

        if (!token.isUsable()) {
            throw new BadRequestException("Code has expired. Request a new one.");
        }

        // The OTP is spent here; the returned reset token carries the flow forward
        // with a fresh, longer window.
        token.setOtpCode(null);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_VALID_MINUTES));
        tokenRepository.save(token);

        return new VerifyOtpResponse(token.getToken(), token.getExpiresAt());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = tokenRepository.findByToken(request.resetToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (!token.isUsable()) {
            throw new BadRequestException("Invalid or expired reset token");
        }

        UserInfm user = token.getUser();
        user.setUserPwd(passwordEncoder.encode(request.newPassword()));
        user.resetFailedLoginAttempts();
        user.setModId("password-reset");
        user.setModDtm(LocalDateTime.now());
        userRepository.save(user);

        token.consume();
        tokenRepository.save(token);

        log.info("Password reset completed for '{}'", user.getUserId());
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        UserInfm user = userRepository.findByUserId(username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getUserPwd())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getUserPwd())) {
            throw new BadRequestException("New password must differ from the current one");
        }

        user.setUserPwd(passwordEncoder.encode(request.newPassword()));
        user.setModId(username);
        user.setModDtm(LocalDateTime.now());
        userRepository.save(user);
    }

    /* ===================================================================== */
    /* Helpers                                                               */
    /* ===================================================================== */

    /**
     * Self-service profile edit.
     *
     * <p>Only name, email and phone. Role and username are not accepted here —
     * letting a user PUT their own role would be a privilege-escalation hole.
     */
    @Transactional
    public UserResponse updateProfile(String username, UpdateProfileRequest request) {
        UserInfm user = userRepository.findByUserId(username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (request.email() != null && !request.email().isBlank()
                && !request.email().equalsIgnoreCase(user.getEml())) {
            userRepository.findByEml(request.email())
                    .filter(other -> !other.getId().equals(user.getId()))
                    .ifPresent(other -> {
                        throw new ConflictException("Email already registered: " + request.email());
                    });
        }

        user.setUserNm(request.fullName());
        user.setEml(request.email());
        user.setTel(request.phone());
        user.setModId(username);
        user.setModDtm(LocalDateTime.now());

        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String username) {
        return userRepository.findByUserId(username)
                .map(this::toResponse)
                .orElseThrow(() -> new BadRequestException("User not found"));
    }

    private UserResponse toResponse(UserInfm u) {
        return new UserResponse(
                u.getId(), u.getUserId(), u.getUserNm(), u.getEml(), u.getTel(),
                u.getRole(), u.isLocked(), u.getLstLgnDtm());
    }

    private String generateBizKey() {
        // 10-char column; keep it short and unique enough for a single tenant.
        return (defaultBizKey + UUID.randomUUID().toString().replace("-", ""))
                .substring(0, 10).toUpperCase();
    }
}
