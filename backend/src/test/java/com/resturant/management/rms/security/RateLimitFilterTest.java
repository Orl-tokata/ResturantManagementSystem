package com.resturant.management.rms.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The limit over the real filter chain.
 *
 * <p>Enabled and tightened here with properties, because the suite runs on the
 * dev profile where it is off — every other test signs in from the same
 * address, so a shared limit would make them fail depending on how many ran
 * first.
 */
@SpringBootTest(properties = {
        "app.security.rate-limit.enabled=true",
        "app.security.rate-limit.login.limit=3",
        "app.security.rate-limit.login.window=1m",
        "app.security.rate-limit.forgot-password.limit=2",
        "app.security.rate-limit.forgot-password.window=10m",
})
@AutoConfigureMockMvc
@Transactional
class RateLimitFilterTest {

    private static final String GOOD = """
            {"username":"admin","password":"ChangeMe123!"}""";
    private static final String BAD = """
            {"username":"admin","password":"wrong-on-purpose"}""";

    @Autowired MockMvc mvc;
    @Autowired RateLimiter rateLimiter;

    @BeforeEach
    void clearBuckets() {
        // The limiter is a singleton, so one test's spending would otherwise
        // decide the next one's result — and the order is not guaranteed.
        rateLimiter.reset();
    }

    /** A login attempt that appears to come from a given address. */
    private MockHttpServletRequestBuilder loginFrom(String ip, String body) {
        return post("/api/auth/login")
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    @Test
    @DisplayName("a fourth attempt against a limit of three is refused with 429")
    void refusesOverTheLimit() throws Exception {
        for (int i = 1; i <= 3; i++) {
            mvc.perform(loginFrom("10.0.0.1", BAD))
                    .andExpect(status().isUnauthorized());
        }

        mvc.perform(loginFrom("10.0.0.1", BAD))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }

    /**
     * The limit is on attempts, not on failures. Checking a password is the
     * expensive part of a login, so a flood has to be refused before it gets
     * that far — otherwise correct guesses still cost a BCrypt comparison each.
     */
    @Test
    @DisplayName("correct credentials are refused too once the limit is spent")
    void appliesBeforeAuthentication() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(loginFrom("10.0.0.2", BAD));
        }

        mvc.perform(loginFrom("10.0.0.2", GOOD))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("one caller's flood does not block another")
    void limitsPerCaller() throws Exception {
        for (int i = 0; i < 4; i++) {
            mvc.perform(loginFrom("10.0.0.3", BAD));
        }
        mvc.perform(loginFrom("10.0.0.3", BAD)).andExpect(status().isTooManyRequests());

        mvc.perform(loginFrom("10.0.0.4", GOOD))
                .andExpect(status().isOk());
    }

    /**
     * X-Forwarded-For is caller supplied. With nothing upstream rewriting it,
     * honouring it would let an attacker send a fresh value per request and
     * never be limited at all — so the default must ignore it.
     */
    @Test
    @DisplayName("a forged X-Forwarded-For does not buy a fresh allowance")
    void ignoresForwardedHeaderByDefault() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(loginFrom("10.0.0.5", BAD).header("X-Forwarded-For", "1.2.3." + i));
        }

        mvc.perform(loginFrom("10.0.0.5", BAD).header("X-Forwarded-For", "9.9.9.9"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("each endpoint has its own budget")
    void budgetsAreSeparatePerEndpoint() throws Exception {
        for (int i = 0; i < 4; i++) {
            mvc.perform(loginFrom("10.0.0.6", BAD));
        }
        mvc.perform(loginFrom("10.0.0.6", BAD)).andExpect(status().isTooManyRequests());

        // Spending the login allowance must not also stop a password reset.
        mvc.perform(post("/api/auth/forgot-password")
                        .with(r -> { r.setRemoteAddr("10.0.0.6"); return r; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@rms.local"}"""))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("the refusal is translated like every other error")
    void messageIsLocalized() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(loginFrom("10.0.0.7", BAD));
        }

        MvcResult result = mvc.perform(loginFrom("10.0.0.7", BAD)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "km"))
                .andExpect(status().isTooManyRequests())
                .andReturn();

        String message = result.getResponse().getContentAsString();
        assertThat(message.chars().anyMatch(c -> c >= 0x1780 && c <= 0x17FF))
                .as("expected Khmer in: %s", message)
                .isTrue();
    }

    @Test
    @DisplayName("endpoints that need a token are not rate limited")
    void leavesAuthenticatedEndpointsAlone() throws Exception {
        // Well past the login limit, but this path is not one of the four.
        for (int i = 0; i < 10; i++) {
            mvc.perform(post("/api/auth/logout")
                            .with(r -> { r.setRemoteAddr("10.0.0.8"); return r; }))
                    .andExpect(status().isOk());
        }
    }
}
