package com.resturant.management.rms.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * Issues and validates JWTs. Uses the jjwt 0.12.x API.
 *
 * <p>Two token types share one signing key but differ in lifetime and in the
 * {@code typ} claim, so a refresh token cannot be replayed as an access token.
 */
@Slf4j
@Service
public class JwtService {

    private static final String CLAIM_TYPE = "typ";
    private static final String CLAIM_ROLE = "role";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final String secret;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    private SecretKey key;

    public JwtService(
            @Value("${app.security.jwt.secret-key:}") String secret,
            @Value("${app.security.jwt.expiration:3600000}") long accessExpirationMs,
            @Value("${app.security.jwt.refresh-token.expiration:86400000}") long refreshExpirationMs) {
        this.secret = secret;
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @PostConstruct
    void init() {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("""
                    app.security.jwt.secret-key is not set.
                    Provide it via the JWT_SECRET environment variable or application-local.yml.
                    Generate one with:  openssl rand -base64 64
                    """);
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "app.security.jwt.secret-key must be at least 32 bytes for HS256; got " + bytes.length);
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    /* ---- Issuing --------------------------------------------------------- */

    public String generateAccessToken(String username, String role) {
        return build(username, Map.of(CLAIM_TYPE, TYPE_ACCESS, CLAIM_ROLE, role), accessExpirationMs);
    }

    public String generateRefreshToken(String username) {
        return build(username, Map.of(CLAIM_TYPE, TYPE_REFRESH), refreshExpirationMs);
    }

    private String build(String subject, Map<String, ?> claims, long ttlMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(key)
                .compact();
    }

    /* ---- Reading --------------------------------------------------------- */

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public boolean isAccessToken(String token) {
        return TYPE_ACCESS.equals(parse(token).get(CLAIM_TYPE, String.class));
    }

    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(parse(token).get(CLAIM_TYPE, String.class));
    }

    /**
     * @return true when the token's signature verifies, it has not expired, and
     *         it is of the expected type.
     */
    public boolean isValid(String token, boolean expectAccess) {
        try {
            Claims claims = parse(token);
            String type = claims.get(CLAIM_TYPE, String.class);
            return (expectAccess ? TYPE_ACCESS : TYPE_REFRESH).equals(type);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected token: {}", e.getMessage());
            return false;
        }
    }

    /** Throws {@link JwtException} when the signature or expiry check fails. */
    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessExpirationSeconds() {
        return accessExpirationMs / 1000;
    }

    public long getRefreshExpirationSeconds() {
        return refreshExpirationMs / 1000;
    }
}
