package com.resturant.management.rms.audit;

import com.resturant.management.rms.common.ApiResponse;
import com.resturant.management.rms.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Reading the change history.
 *
 * <p>There is no POST, PUT, PATCH or DELETE here, and there will not be. Entries
 * are written as a side effect of the changes they describe; anything that could
 * add or remove one by hand would make the whole table worthless as evidence.
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Audit", description = "Who changed what, when, and from what to what")
public class AuditController {

    private final AuditLogRepository repository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search the audit log",
            description = "Newest first. Filter by entity, by the record's id, by user, "
                    + "or by date. `to` covers the whole day given.")
    public ApiResponse<PageResponse<AuditEntry>> search(
            @RequestParam(required = false) String entity,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Newest first, and by id within a timestamp — two changes in the same
        // millisecond are otherwise ordered arbitrarily, which makes a log read
        // top to bottom occasionally lie about which came first.
        var pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));

        var results = repository.findAll(
                AuditLogRepository.matching(
                        blankToNull(entity),
                        entityId,
                        blankToNull(userId),
                        from == null ? null : from.atStartOfDay(),
                        // A change made at 14:05 must fall inside a range ending
                        // today; atStartOfDay would exclude all but midnight.
                        to == null ? null : to.atTime(LocalTime.MAX)),
                pageable);

        return ApiResponse.ok(PageResponse.from(results.map(AuditEntry::from)));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /** What the log shows. The entity is never exposed directly. */
    public record AuditEntry(
            Long id,
            String userId,
            String action,
            String entity,
            Long entityId,
            String before,
            String after,
            String ip,
            LocalDateTime at
    ) {
        static AuditEntry from(AuditLog a) {
            return new AuditEntry(
                    a.getId(), a.getUserId(), a.getAction().name(), a.getEntity(),
                    a.getEntityId(), a.getBeforeJson(), a.getAfterJson(), a.getIp(), a.getCreatedAt());
        }
    }
}
