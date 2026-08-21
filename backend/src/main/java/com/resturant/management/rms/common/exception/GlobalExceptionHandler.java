package com.resturant.management.rms.common.exception;

import com.resturant.management.rms.common.ApiResponse;
import com.resturant.management.rms.common.i18n.Messages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Single place where exceptions become {@link ApiResponse} payloads, so every
 * error the frontend sees has the same shape as every success — and the only
 * place that turns a message key into text, because it is the only place that
 * knows the locale of the request being answered.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Messages messages;

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApi(ApiException ex) {
        String text = messages.get(ex.getMessageKey(), resolveArgs(ex.getArgs()));
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getStatus().value(), text));
    }

    /**
     * Resolves any argument that is itself a message key.
     *
     * <p>"Cannot delete Supplier 'Acme': it is still used by 3 purchase order(s)"
     * has three translatable nouns inside one translatable sentence. The
     * services mark those with {@link LocalizedArg} because they cannot resolve
     * them — this unwraps them just before formatting.
     */
    private Object[] resolveArgs(Object[] args) {
        if (args == null || args.length == 0) return args;
        Object[] out = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof LocalizedArg key) {
                out[i] = messages.get(key.key());
            } else {
                // Stringified deliberately. MessageFormat hands a numeric
                // argument to the locale's NumberFormat, which groups it: the
                // id 999999 came back as "999.999" under km and "999,999"
                // under en, and a grouped identifier is not an identifier.
                // The same formatting would print a BigDecimal 12.50 as "12,5"
                // in a comma-decimal locale, silently changing an amount in an
                // error message about that amount. toString() is exact for
                // both, and nothing here wants a formatted number.
                out[i] = String.valueOf(args[i]);
            }
        }
        return out;
    }

    /** Bean Validation failures — report every invalid field, not just the first. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(this::describe)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), detail));
    }

    /**
     * The field name is translated too. Leaving it raw would produce a sentence
     * that is half Khmer and half Java identifier — "amountTendered: ត្រូវបំពេញ".
     * A field with no {@code field.*} entry falls back to its own name, which
     * is still more useful than failing.
     */
    private String describe(FieldError e) {
        return fieldName(e.getField()) + ": " + e.getDefaultMessage();
    }

    /**
     * The display name for a request field.
     *
     * <p>Used for validation messages only, which a user reads under the form
     * they just submitted. Malformed-payload errors deliberately keep the raw
     * JSON key — see handleUnreadableBody.
     *
     * <p>Falls back to the raw name when the bundle has no entry: a sentence
     * naming "amountTendered" is worse than one naming "ប្រាក់ដែលបានទទួល", but far
     * better than one that cannot be produced at all. Messages.get returns the
     * key itself on a miss, which is how that is detected.
     */
    private String fieldName(String field) {
        String key = "field." + field;
        String resolved = messages.get(key);
        return resolved.equals(key) ? field : resolved;
    }

    /* ---- Authentication ------------------------------------------------- */

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        // Deliberately vague: do not reveal whether the username exists.
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, messages.get("error.auth.badCredentials")));
    }

    /**
     * The thrown message is ignored on purpose. Spring's LockedException is
     * constructed deep in the login flow with no locale available, so the text
     * is decided here instead.
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocked(LockedException ex) {
        return ResponseEntity.status(HttpStatus.LOCKED)
                .body(ApiResponse.error(423, messages.get("error.auth.locked")));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabled(DisabledException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(403, messages.get("error.auth.disabled")));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(403, messages.get("error.auth.forbidden")));
    }

    /**
     * Safety net for constraint violations the services did not anticipate.
     * Services should check first and raise a {@link ConflictException} with a
     * specific message — this only stops a raw SQL error reaching the client.
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleIntegrity(
            org.springframework.dao.DataIntegrityViolationException ex) {
        log.warn("Database constraint violated: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(409, messages.get("error.request.conflict")));
    }

    /**
     * A request body Jackson could not read: an enum value that matches no
     * constant, a non-numeric amount, truncated JSON. That is the caller's
     * mistake, so it must be a 400 — without this it falls through to the
     * catch-all and reports 500.
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {

        Throwable cause = ex.getMostSpecificCause();
        String detail = messages.get("error.request.unreadable");

        // Name the offending field and the accepted values — a bare "malformed
        // JSON" tells the caller nothing actionable.
        if (cause instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException ife) {
            String field = ife.getPath().isEmpty() ? "value" : ife.getPath().get(0).getFieldName();
            Class<?> target = ife.getTargetType();
            if (target != null && target.isEnum()) {
                // Field name and accepted values both stay raw here, unlike
                // in a validation message. This error means the payload could
                // not be parsed at all, which a form with a fixed set of
                // options cannot provoke — so the reader is whoever is calling
                // the API, and they need the exact JSON key and the exact
                // strings to send, not prose describing them.
                detail = messages.get("error.request.invalidValueEnum",
                        field, ife.getValue(), Arrays.toString(target.getEnumConstants()));
            } else {
                detail = messages.get("error.request.invalidValue", field, ife.getValue());
            }
        }

        log.debug("Unreadable request body: {}", cause.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(400, detail));
    }

    /** Enum path/query parameters that do not match any constant. */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(400,
                // Same reasoning as above: a path or query parameter that
                // will not convert is a caller error, so name it as the caller
                // wrote it.
                messages.get("error.request.invalidValue", ex.getName(), ex.getValue())));
    }

    /* ---- Client mistakes that must not read as server failures ----------
       Without these, the catch-all below turns a URL typo, a wrong verb or a
       missing query parameter into a 500 — which tells the caller the server
       broke when in fact the request did.
       -------------------------------------------------------------------- */

    /** Unknown path. Both types occur depending on how the request is dispatched. */
    @ExceptionHandler({
            org.springframework.web.servlet.NoHandlerFoundException.class,
            org.springframework.web.servlet.resource.NoResourceFoundException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, messages.get("error.request.noEndpoint")));
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(
            org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        String allowed = ex.getSupportedHttpMethods() == null
                ? ""
                : ex.getSupportedHttpMethods().toString();
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(405,
                        messages.get("error.request.methodNotAllowed", ex.getMethod(), allowed)));
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedMediaType(
            org.springframework.web.HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error(415,
                        messages.get("error.request.mediaType", String.valueOf(ex.getContentType()))));
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(
            org.springframework.web.bind.MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(400,
                messages.get("error.request.missingParam", ex.getParameterName())));
    }

    /** Violations on @RequestParam / @PathVariable, which bypass @Valid. */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            jakarta.validation.ConstraintViolationException ex) {
        String detail = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiResponse.error(400, detail));
    }

    /* ---- Everything else ------------------------------------------------ */

    /**
     * Last resort. Logs the full stack trace server-side but tells the client
     * nothing about it — an exception message can leak table names, file paths
     * or library versions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, messages.get("error.request.internal")));
    }
}
