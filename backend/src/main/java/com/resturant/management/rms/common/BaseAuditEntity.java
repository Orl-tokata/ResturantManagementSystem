package com.resturant.management.rms.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Audit columns shared by every business entity.
 *
 * <p>Column <em>names</em> follow the NIEI-Y4 convention ({@code reg_id},
 * {@code reg_dtm}, {@code mod_id}, {@code mod_dtm}, {@code act_yn}) so the two
 * schemas stay recognisable to each other. The <em>types</em> deliberately do
 * not: the old project stores timestamps as {@code String}, which cannot be
 * sorted or range-queried in SQL. Here they are real {@code TIMESTAMP} columns
 * populated by Spring Data JPA auditing.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditEntity {

    /** Soft-delete / active flag: {@code "Y"} or {@code "N"}. */
    @Column(name = "act_yn", nullable = false, length = 1)
    private String actYn = "Y";

    @CreatedBy
    @Column(name = "reg_id", length = 50, updatable = false)
    private String regId;

    @CreatedDate
    @Column(name = "reg_dtm", updatable = false)
    private LocalDateTime regDtm;

    @LastModifiedBy
    @Column(name = "mod_id", length = 50)
    private String modId;

    @LastModifiedDate
    @Column(name = "mod_dtm")
    private LocalDateTime modDtm;

    public boolean isActive() {
        return "Y".equals(actYn);
    }

    public void deactivate() {
        this.actYn = "N";
    }
}
