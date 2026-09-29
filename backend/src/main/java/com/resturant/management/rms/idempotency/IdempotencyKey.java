package com.resturant.management.rms.idempotency;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One attempt at a write that must not happen twice.
 *
 * <p>A row is claimed before the request is handled and completed after, so the
 * unique constraint on {@code keyValue} is what stops two simultaneous requests
 * both proceeding — not a check in a service, which two threads pass together.
 *
 * <p>{@code method} and {@code path} are recorded so a key reused against a
 * different endpoint is caught rather than answered with the wrong reply.
 */
@Entity
@Table(name = "idempotency_key")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKey {

    public enum State { IN_PROGRESS, COMPLETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key_value", nullable = false, unique = true, length = 80)
    private String keyValue;

    @Column(name = "method", nullable = false, length = 10)
    private String method;

    @Column(name = "path", nullable = false, length = 255)
    private String path;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private State state = State.IN_PROGRESS;

    @Column(name = "response_status")
    private Integer responseStatus;

    /**
     * The finished reply, replayed verbatim.
     *
     * <p>Null on a completed row means the reply was too large to store. That
     * is recorded honestly rather than truncated — half a JSON document
     * replayed as if whole is worse than admitting the reply is gone.
     */
    @Column(name = "response_body", length = 8000)
    private String responseBody;

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** True when this row can answer a repeat of the same request. */
    public boolean isReplayable() {
        return state == State.COMPLETED && responseStatus != null && responseBody != null;
    }

    /** True when the key is being reused for something other than it was claimed for. */
    public boolean matches(String method, String path) {
        return this.method.equals(method) && this.path.equals(path);
    }
}
