package com.resturant.management.rms.auth;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * What the till does with an address that cannot exist.
 *
 * <p>A reset code was sent to {@code admin@rms.local} — the seeded
 * placeholder — and everything reported success: the code was generated,
 * stored, and handed to SMTP without complaint. The only evidence was a bounce
 * half an hour later, in the mailbox belonging to the SMTP account rather than
 * the one the person was refreshing.
 *
 * <p>So the send is not attempted now, and the log says why. Asserting on log
 * output would be asserting on wording; what matters, and what is asserted
 * here, is that nothing was handed to the mail server.
 */
@SpringBootTest(properties = {
        // Enough configuration for MailService to believe it could send, so
        // the thing under test is the address and not the setup.
        "spring.mail.username=till@somewhere-real.example-mail.com",
})
class MailServiceTest {

    @Autowired MailService mail;

    @MockitoBean JavaMailSender mailSender;

    @Test
    @DisplayName("a code for a reserved domain is never handed to the mail server")
    void reservedDomainIsNotSent() {
        mail.sendOtp("admin@rms.local", "123456", 10);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("an ordinary address is sent as before")
    void ordinaryAddressIsSent() {
        when(mailSender.createMimeMessage())
                .thenReturn(new org.springframework.mail.javamail.JavaMailSenderImpl().createMimeMessage());

        mail.sendOtp("owner@angkor-restaurant.com.kh", "123456", 10);

        verify(mailSender).send(any(MimeMessage.class));
    }
}
