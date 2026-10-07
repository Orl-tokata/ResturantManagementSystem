package com.resturant.management.rms.auth;

import com.resturant.management.rms.auth.dto.AuthDtos.*;
import com.resturant.management.rms.branch.Branch;
import com.resturant.management.rms.branch.BranchContext;
import com.resturant.management.rms.branch.BranchRepository;
import com.resturant.management.rms.common.exception.BadRequestException;
import com.resturant.management.rms.common.exception.ForbiddenException;
import com.resturant.management.rms.common.exception.NotFoundException;
import com.resturant.management.rms.common.exception.UnauthorizedException;
import com.resturant.management.rms.common.exception.ConflictException;
import com.resturant.management.rms.user.Role;
import com.resturant.management.rms.user.UserInfm;
import com.resturant.management.rms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
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
    private final BranchRepository branchRepository;

    @Value("${app.security.default-biz-key:RMS}")
    private String defaultBizKey;

    /* ===================================================================== */
    /* Registration                                                          */
    /* ===================================================================== */

    /**
     * Who is creating this account.
     *
     * <p>Stored on the row so the grant is attributable. It used to read
     * "self-register", which was accurate while anyone could create their own
     * account and is exactly the arrangement that had to go.
     */
    private String actingAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() ? auth.getName() : "system";
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUserId(request.username())) {
            throw new ConflictException("error.auth.usernameTaken", request.username());
        }
        if (request.email() != null && userRepository.existsByEml(request.email())) {
            throw new ConflictException("error.auth.emailRegistered", request.email());
        }

        // A login is only ever ADMIN or CASHIER. Without this the role came
        // straight from the request body and a WAITER or CHEF account was one
        // dropdown away from the till.
        Role role = request.role() != null ? request.role() : Role.CASHIER;
        if (!role.canSignIn()) {
            throw new BadRequestException("error.auth.roleCannotSignIn", role.name());
        }

        UserInfm user = UserInfm.builder()
                .bizKey(generateBizKey())
                .userId(request.username())
                .userNm(request.fullName())
                .userPwd(passwordEncoder.encode(request.password()))
                .eml(request.email())
                .tel(request.phone())
                .role(role)
                // The shop the admin creating them is working in. Not a field
                // on the request: who may work where is not a thing the person
                // being created gets to say, and an admin who wants somebody
                // in another branch switches to it first.
                .branchId(BranchContext.get())
                .lockYn("N")
                .loginFailedCnt(0)
                .actYn("Y")
                .regId(actingAdmin())
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
    /*
     * noRollbackFor is load-bearing, not tidiness.
     *
     * BadCredentialsException is a RuntimeException, so a plain @Transactional
     * rolled the whole method back — including the save that had just recorded
     * the failed attempt. The counter went back to zero every time, never
     * reached five, and the account never locked. The lockout had never once
     * worked; the log line below printed "attempt 1" on the fiftieth guess,
     * because the object in memory incremented and the row never did.
     *
     * A failed sign-in is the one thing that must survive the failure.
     */
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public LoginResult login(LoginRequest request) {
        UserInfm user = userRepository.findByUserId(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        // An automatic lock whose fifteen minutes are up is cleared here rather
        // than merely ignored, so the row stops claiming to be locked and the
        // admin screen does not show a lock that no longer applies.
        if (user.clearExpiredLock()) {
            userRepository.save(user);
            log.info("Automatic lock on '{}' expired", user.getUserId());
        }

        if (user.isLocked()) {
            // The message is not used: the handler answers LockedException with
            // error.auth.locked, resolved for the caller's locale.
            throw new LockedException("locked");
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

        String access = jwtService.generateAccessToken(
                user.getUserId(), user.getRole().name(), user.getBranchId());
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
            throw new UnauthorizedException("error.auth.refreshMissing");
        }

        String username = jwtService.extractUsername(refreshToken);
        UserInfm user = userRepository.findByUserId(username)
                .orElseThrow(() -> new UnauthorizedException("error.auth.refreshStale"));

        if (!user.isEnabled() || user.isLocked()) {
            throw new UnauthorizedException("error.auth.accountInactive");
        }

        /*
         * The branch comes from the user's record, not from the expiring token
         * being refreshed. Somebody who switched shop an hour ago keeps the
         * shop they switched to only until their session ends, which is the
         * conservative reading: a refresh is a continuation of an identity,
         * and the identity's home branch is what is written down.
         */
        String access = jwtService.generateAccessToken(
                user.getUserId(), user.getRole().name(), user.getBranchId());
        return AuthResponse.of(access, jwtService.getAccessExpirationSeconds(), toResponse(user));
    }

    /* ===================================================================== */
    /* Switching branch                                                      */
    /* ===================================================================== */

    /**
     * Mints a token for another shop.
     *
     * <p>Membership is checked here and the answer is signed, which is the
     * difference between this and a {@code ?branchId=} parameter: the
     * parameter would be a request to be trusted, and this is a decision the
     * server made.
     *
     * <p>Only ADMIN and MANAGER may move. A cashier belongs to a till, and a
     * till belongs to a shop.
     */
    @Transactional(readOnly = true)
    public AuthResponse switchBranch(String username, Long branchId) {
        UserInfm user = userRepository.findByUserId(username)
                .orElseThrow(() -> new UnauthorizedException("error.auth.refreshStale"));

        if (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER) {
            throw new ForbiddenException("error.branch.notPermitted");
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> NotFoundException.of("entity.branch", branchId));
        if (!"Y".equals(branch.getActYn())) {
            throw new BadRequestException("error.branch.inactive", branch.getName());
        }

        String access = jwtService.generateAccessToken(
                user.getUserId(), user.getRole().name(), branch.getId());

        log.info("{} switched to branch {} ({})", username, branch.getCode(), branch.getName());
        return AuthResponse.of(access, jwtService.getAccessExpirationSeconds(),
                toResponse(user, branch));
    }

    /** The shops this user may work in. One, unless they are senior enough to move. */
    @Transactional(readOnly = true)
    public List<BranchResponse> branchesFor(String username) {
        UserInfm user = userRepository.findByUserId(username)
                .orElseThrow(() -> new UnauthorizedException("error.auth.refreshStale"));

        List<Branch> branches = user.getRole() == Role.ADMIN || user.getRole() == Role.MANAGER
                ? branchRepository.findByActYnOrderByCodeAsc("Y")
                : branchRepository.findById(user.getBranchId()).stream().toList();

        Long current = BranchContext.get();
        return branches.stream()
                .map(b -> new BranchResponse(b.getId(), b.getCode(), b.getName(), b.getNameEn(),
                        b.getId().equals(current)))
                .toList();
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
                .orElseThrow(() -> new BadRequestException("error.auth.otpInvalid"));

        PasswordResetToken token = tokenRepository
                .findFirstByUserAndOtpCodeAndUsedYnOrderByCreatedAtDesc(user, request.code(), "N")
                .orElseThrow(() -> new BadRequestException("error.auth.otpInvalid"));

        if (!token.isUsable()) {
            throw new BadRequestException("error.auth.otpExpired");
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
                .orElseThrow(() -> new BadRequestException("error.auth.resetTokenInvalid"));

        if (!token.isUsable()) {
            throw new BadRequestException("error.auth.resetTokenInvalid");
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
                .orElseThrow(() -> new BadRequestException("error.auth.userNotFound"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getUserPwd())) {
            throw new BadRequestException("error.auth.currentPasswordWrong");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getUserPwd())) {
            throw new BadRequestException("error.auth.passwordUnchanged");
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
                .orElseThrow(() -> new BadRequestException("error.auth.userNotFound"));

        if (request.email() != null && !request.email().isBlank()
                && !request.email().equalsIgnoreCase(user.getEml())) {
            userRepository.findByEml(request.email())
                    .filter(other -> !other.getId().equals(user.getId()))
                    .ifPresent(other -> {
                        throw new ConflictException("error.auth.emailRegistered", request.email());
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
                .orElseThrow(() -> new BadRequestException("error.auth.userNotFound"));
    }

    private UserResponse toResponse(UserInfm u) {
        return toResponse(u, branchRepository.findById(u.getBranchId()).orElse(null));
    }

    private UserResponse toResponse(UserInfm u, Branch branch) {
        return new UserResponse(
                u.getId(), u.getUserId(), u.getUserNm(), u.getEml(), u.getTel(),
                u.getRole(), u.isLocked(), u.getLstLgnDtm(),
                branch != null ? branch.getId() : u.getBranchId(),
                branch != null ? branch.getName() : null);
    }

    private String generateBizKey() {
        // 10-char column; keep it short and unique enough for a single tenant.
        return (defaultBizKey + UUID.randomUUID().toString().replace("-", ""))
                .substring(0, 10).toUpperCase();
    }
}
