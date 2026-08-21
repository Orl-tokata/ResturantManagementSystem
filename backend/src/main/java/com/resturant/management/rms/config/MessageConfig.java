package com.resturant.management.rms.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * Makes every user-facing message the API returns translatable.
 *
 * <p>Locale comes from the {@code Accept-Language} header, which is the only
 * thing a stateless API can use: there is no session to hold a preference, and
 * the header is what the browser and our own axios client already send.
 */
@Configuration
public class MessageConfig {

    /** Khmer is the product language, but see {@link #localeResolver()}. */
    public static final Locale KHMER = Locale.forLanguageTag("km");

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages/messages");
        source.setDefaultEncoding("UTF-8");

        // Every value is treated as a MessageFormat pattern, even one with no
        // placeholders. Costs a parse; buys a single quoting rule across both
        // files instead of one that depends on whether a message takes an
        // argument. See the header comment in messages_en.properties.
        source.setAlwaysUseMessageFormat(true);

        // A missing key should be loud. Returning the key as its own message
        // would ship "error.order.emptyBill" to a cashier's screen.
        source.setUseCodeAsDefaultMessage(false);

        // Do not fall back to the JVM's locale: on a server in Phnom Penh that
        // silently resolves differently than on one in Frankfurt.
        source.setFallbackToSystemLocale(false);
        return source;
    }

    /**
     * English is the default rather than Khmer, deliberately.
     *
     * <p>Our own client always sends an explicit {@code Accept-Language}, so
     * this only decides what a caller with no preference sees — curl, Swagger,
     * a webhook, a test. For those, English is the more useful answer, and it
     * keeps the API's default behaviour stable for anything already reading it.
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(List.of(Locale.ENGLISH, KHMER));
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }

    /**
     * Points Bean Validation at the same bundle, so {@code @NotBlank(message =
     * "{valid.required}")} resolves from messages_km.properties rather than
     * Hibernate Validator's own ValidationMessages.properties.
     */
    @Bean
    public LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
        factory.setValidationMessageSource(messageSource);
        return factory;
    }
}
