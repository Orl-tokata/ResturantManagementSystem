package com.resturant.management.rms.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One recorded change. Written once, never updated, never deleted.
 *
 * <p>No {@code act_yn} and no modification columns, unlike every other entity
 * here — a row that can be retracted is not evidence of anything.
 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    public enum Action { CREATE, UPDATE, DELETE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_ref")
    private Long userRef;

    /** Kept as text so the log still names someone after the account is gone. */
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private Action action;

    @Column(name = "entity", nullable = false, length = 50)
    private String entity;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "before_json", length = 4000)
    private String beforeJson;

    @Column(name = "after_json", length = 4000)
    private String afterJson;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
