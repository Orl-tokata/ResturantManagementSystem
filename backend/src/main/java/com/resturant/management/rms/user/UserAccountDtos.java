package com.resturant.management.rms.user;

import java.time.LocalDateTime;

/** What an administrator sees when looking after login accounts. */
public final class UserAccountDtos {

    private UserAccountDtos() {
    }

    public record UserAccount(
            Long id,
            String username,
            String fullName,
            String email,
            String phone,
            Role role,
            boolean active,
            boolean locked,
            /** When an automatic lock lifts; null when locked by an administrator. */
            LocalDateTime lockedUntil,
            int failedAttempts,
            LocalDateTime lastLoginAt
    ) {
        public static UserAccount of(UserInfm u) {
            return new UserAccount(
                    u.getId(),
                    u.getUserId(),
                    u.getUserNm(),
                    u.getEml(),
                    u.getTel(),
                    u.getRole(),
                    "Y".equals(u.getActYn()),
                    u.isLocked(),
                    u.lockedUntilOrNull(),
                    u.getLoginFailedCnt() == null ? 0 : u.getLoginFailedCnt(),
                    u.getLstLgnDtm());
            // No password field of any kind, not even a redacted one: a hash is
            // still a credential, and the surest way not to leak it is for it
            // never to leave the entity.
        }
    }
}
