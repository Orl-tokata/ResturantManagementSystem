package com.resturant.management.rms.common.i18n;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Resolves a message key against the locale of the request being served.
 *
 * <p>Exists so callers never touch {@link LocaleContextHolder} directly. The
 * locale is request state, and spreading reads of it through the services is
 * how it ends up being read on a thread that does not have one.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Messages {

    private final MessageSource messageSource;

    /** Resolves for the current request's locale. */
    public String get(String key, Object... args) {
        return get(LocaleContextHolder.getLocale(), key, args);
    }

    public String get(Locale locale, String key, Object... args) {
        try {
            return messageSource.getMessage(key, args, locale);
        } catch (NoSuchMessageException e) {
            // A missing key is a bug, not a runtime condition. Throwing here
            // would turn a cosmetic gap into a 500 on a path that was already
            // failing for some other reason, and would hide the original
            // error. Return the key so the response still identifies itself,
            // and log loudly enough that it gets fixed. MessageBundleTest is
            // what should catch this before it ships.
            log.error("No message for key '{}' in locale '{}'", key, locale);
            return key;
        }
    }
}
