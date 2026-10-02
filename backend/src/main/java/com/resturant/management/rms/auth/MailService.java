package com.resturant.management.rms.auth;

import com.resturant.management.rms.common.EmailAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Sends the password-reset OTP.
 *
 * <p>The message goes out as multipart/alternative: an HTML part for clients
 * that render it and a plain-text part for those that will not, which is also
 * what most spam filters read first. A code-only email with no text part looks
 * a lot like the thing it is trying not to be mistaken for.
 *
 * <p>When no SMTP username is configured the code is logged instead of sent, so
 * the reset flow is still testable on a developer machine without wiring up a
 * mail account. That fallback is deliberately loud.
 *
 * <p>The same happens for an address at a reserved domain. Sending one costs a
 * bounce that arrives somewhere nobody is looking, half an hour later; logging
 * it says what is wrong at the moment it goes wrong, and leaves the reset
 * completable by whoever can read the server log — which, for an address like
 * {@code admin@rms.local}, is the only person it could ever have been.
 */
@Slf4j
@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String from;
    private final String fromName;
    private final boolean configured;

    /** Read once at startup; a template that will not load should fail loudly, not per email. */
    private final String htmlTemplate = read("mail/otp.html");
    private final String textTemplate = read("mail/otp.txt");

    public MailService(JavaMailSender mailSender,
                       @Value("${spring.mail.username:}") String from,
                       @Value("${app.mail.from-name:Restaurant Management System}") String fromName) {
        this.mailSender = mailSender;
        this.from = from;
        this.fromName = fromName;
        this.configured = StringUtils.hasText(from);
    }

    public void sendOtp(String to, String code, int validMinutes) {
        // Short enough to survive truncation in a phone's inbox list, where a
        // bilingual subject joined with a slash loses its second half anyway.
        String subject = "Password reset code · លេខកូដកំណត់ពាក្យសម្ងាត់";

        Map<String, String> values = Map.of(
                "code", code,
                "minutes", String.valueOf(validMinutes),
                "appName", fromName);

        if (!configured) {
            // ASCII only: the Windows console mangles non-ASCII punctuation.
            log.warn("""

                    ================================================================
                     MAIL NOT CONFIGURED - password reset code not sent by email.
                       to   : {}
                       code : {}
                     Set MAIL_USERNAME / MAIL_PASSWORD to enable real delivery.
                    ================================================================
                    """, to, code);
            return;
        }

        if (EmailAddress.isUndeliverable(to)) {
            log.warn("""

                    ================================================================
                     UNDELIVERABLE ADDRESS - password reset code not sent by email.
                       to   : {}
                       code : {}
                     '{}' is a reserved domain. It has no MX record and never
                     will, so this message would only produce a bounce.
                     Give this account a real address:
                       UPDATE users_infm SET eml = '...' WHERE eml = '{}';
                    ================================================================
                    """, to, code, EmailAddress.domainOf(to), to);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            // true = multipart, so both parts can be attached; the charset has to
            // be named or the Khmer arrives as question marks.
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            // A display name, so the inbox shows the restaurant rather than
            // whichever mailbox happens to be doing the sending.
            helper.setFrom(from, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            // Plain text first, HTML second — that is the order the helper wants,
            // and a client shows the last part it understands.
            helper.setText(fill(textTemplate, values), fill(htmlTemplate, values));

            mailSender.send(message);
            log.info("Password reset code sent to {}", mask(to));
        } catch (Exception e) {
            // Never surface SMTP details to the caller — that leaks configuration.
            log.error("Failed to send password reset email to {}", mask(to), e);
        }
    }

    /**
     * Substitutes {@code {{name}}} placeholders.
     *
     * <p>Deliberately not a template engine. The values are a six-digit code, a
     * number of minutes and a configured name, none of which a user controls,
     * and adding Thymeleaf to render two files would be a dependency for
     * nothing.
     */
    private String fill(String template, Map<String, String> values) {
        String out = template;
        for (Map.Entry<String, String> e : values.entrySet()) {
            out = out.replace("{{" + e.getKey() + "}}", e.getValue());
        }
        return out;
    }

    private static String read(String path) {
        try (var in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Missing mail template: " + path, e);
        }
    }

    private String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 2) return "***";
        return email.charAt(0) + "***" + email.substring(at - 1);
    }
}
