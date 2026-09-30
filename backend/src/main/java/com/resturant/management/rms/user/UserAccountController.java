package com.resturant.management.rms.user;

import com.resturant.management.rms.common.ApiResponse;
import com.resturant.management.rms.common.PageResponse;
import com.resturant.management.rms.common.Paging;
import com.resturant.management.rms.common.exception.NotFoundException;
import com.resturant.management.rms.user.UserAccountDtos.UserAccount;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Looking after login accounts, which is not the same as looking after staff.
 *
 * <p>{@code /api/staff} keeps an employment record and does not imply a login;
 * this is the account someone signs in with. The two are deliberately separate
 * and have been mixed up in this project's history before.
 *
 * <p>Only unlocking is here. Creating and editing accounts stays where it is
 * until roles become data — see docs/ARCHITECTURE.md §5.
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User accounts", description = "Sign-in accounts and their locks")
public class UserAccountController {

    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List sign-in accounts",
            description = "Accounts carrying a lock flag come first, so the ones "
                    + "needing attention are not on page three.")
    public ApiResponse<PageResponse<UserAccount>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Sorts on the stored flag rather than the derived `locked`, which also
        // weighs the clock and so cannot be expressed in SQL. An automatic lock
        // that has just expired therefore sorts high for one more page load —
        // harmless, and it still reads as unlocked in the row.
        var pageable = Paging.of(page, size,
                Sort.by(Sort.Order.desc("lockYn"), Sort.Order.asc("userId")));
        var accounts = userRepository.findAll(pageable).map(UserAccount::of);
        return ApiResponse.ok(PageResponse.from(accounts));
    }

    /**
     * Lifts a lock.
     *
     * <p>The message a locked-out user reads says to contact an administrator.
     * Until this existed that was not true — there was no unlock anywhere, and
     * the only way back in was a password reset by email, which fails quietly
     * if the account's address is not deliverable.
     */
    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Unlock an account",
            description = "Clears the lock and the failed-attempt counter. "
                    + "Safe to call on an account that is not locked.")
    public ApiResponse<UserAccount> unlock(@PathVariable Long id,
                                           @AuthenticationPrincipal UserInfm actor) {
        UserInfm user = userRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("entity.user", id));

        boolean wasLocked = user.isLocked();
        user.unlock();
        userRepository.save(user);

        // Worth a line in the log either way: an unlock that was not needed is
        // still someone looking at an account they were concerned about.
        log.info("Account '{}' unlocked by '{}' (was locked: {})",
                user.getUserId(), actor == null ? "?" : actor.getUserId(), wasLocked);

        return ApiResponse.ok(UserAccount.of(user));
    }
}
