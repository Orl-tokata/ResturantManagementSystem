package com.resturant.management.rms.auth;

import com.resturant.management.rms.auth.dto.AuthDtos.*;
import com.resturant.management.rms.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, registration, tokens and password recovery")
public class AuthController {

    /**
     * Scoped to the auth endpoints so the cookie is not attached to every API
     * call — only refresh and logout need it.
     */
    private static final String REFRESH_COOKIE = "rms_refresh";
    private static final String COOKIE_PATH = "/api/auth";

    private final AuthService authService;
    private final JwtService jwtService;

    /** Must be true in production; false locally because dev runs over plain http. */
    @Value("${app.security.cookie.secure:false}")
    private boolean secureCookie;

    /* ===================================================================== */

    @PostMapping("/register")
    @Operation(summary = "Create an account")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(authService.register(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in",
            description = "Returns an access token in the body and sets the refresh token as an httpOnly cookie.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                           HttpServletResponse response) {
        AuthService.LoginResult result = authService.login(request);
        response.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.refreshToken()).toString());
        return ResponseEntity.ok(ApiResponse.ok("Login successful", result.body()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange the refresh cookie for a new access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(refreshToken)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out", description = "Clears the refresh cookie.")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString());
        return ResponseEntity.ok(ApiResponse.ok("Logged out", null));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Current user", description = "Backs the sidebar's name and role.")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(authService.currentUser(principal.getUsername())));
    }

    /* ---- Password recovery ---------------------------------------------- */

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a reset code",
            description = "Always reports success — a different answer for unknown addresses "
                        + "would let anyone enumerate registered emails.")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(
                "If that email is registered, a reset code has been sent.", null));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify the 6-digit code and receive a reset token")
    public ResponseEntity<ApiResponse<VerifyOtpResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.verifyOtp(request)));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Set a new password using a reset token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password has been reset", null));
    }

    @PostMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Change your own password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed", null));
    }

    /* ---- Cookie helpers -------------------------------------------------- */

    private ResponseCookie buildRefreshCookie(String token) {
        return ResponseCookie.from(REFRESH_COOKIE, token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(Duration.ofSeconds(jwtService.getRefreshExpirationSeconds()))
                .build();
    }

    private ResponseCookie expiredRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
    }
}
