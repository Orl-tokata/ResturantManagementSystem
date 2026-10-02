package com.resturant.management.rms.config;

import com.resturant.management.rms.common.EmailAddress;
import com.resturant.management.rms.user.UserInfm;
import com.resturant.management.rms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Says at startup which accounts cannot receive a password reset.
 *
 * <p>The failure this exists for is a quiet one. An address at a reserved
 * domain looks ordinary in the admin screen, the reset flow reports success,
 * the code is generated and stored, and the only evidence is a bounce that
 * lands in whichever mailbox the SMTP account belongs to. The person waiting
 * for the code sees nothing at all.
 *
 * <p>So it is said once, at the point where somebody is already reading the
 * log, rather than discovered on the day it matters. One line per account, and
 * silence when there is nothing to report.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailAddressCheck implements ApplicationRunner {

    private final UserRepository users;

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        List<UserInfm> unreachable = users.findAll().stream()
                .filter(UserInfm::isEnabled)
                .filter(u -> EmailAddress.isUndeliverable(u.getEml()))
                .toList();

        if (unreachable.isEmpty()) return;

        StringBuilder lines = new StringBuilder();
        for (UserInfm u : unreachable) {
            lines.append("   %-20s %s%n".formatted(u.getUserId(),
                    u.getEml() == null || u.getEml().isBlank() ? "(no address)" : u.getEml()));
        }

        // ASCII only: the Windows console mangles non-ASCII punctuation.
        log.warn("""

                ================================================================
                 {} account(s) cannot receive a password reset.
                {}\
                 These domains are reserved and have no mail server, so a reset
                 code sent to them can only bounce. Give them real addresses:
                   UPDATE users_infm SET eml = '...' WHERE user_id = '...';
                ================================================================
                """, unreachable.size(), lines);
    }
}
