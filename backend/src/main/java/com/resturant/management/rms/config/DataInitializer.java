package com.resturant.management.rms.config;

import com.resturant.management.rms.staff.Staff;
import com.resturant.management.rms.staff.StaffRepository;
import com.resturant.management.rms.user.Role;
import com.resturant.management.rms.user.UserInfm;
import com.resturant.management.rms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Creates the initial login accounts.
 *
 * <p>Not done in a Flyway migration because passwords must be BCrypt-hashed by
 * the application — a hash baked into SQL would be a shared, public credential.
 *
 * <p>Idempotent: does nothing once any user exists.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String DEFAULT_BIZ_KEY = "RMS001";

    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.debug("Users already present — skipping initial account creation.");
            return;
        }

        UserInfm admin = createUser("admin", "Administrator", Role.ADMIN, "admin@rms.local", "A001");
        UserInfm cashier = createUser("cashier", "Sok Dara", Role.CASHIER, "cashier@rms.local", "C001");

        // Link the seeded staff rows to their login accounts where they match.
        staffRepository.findByStaffCode("EMP-001").ifPresent(s -> link(s, admin));
        staffRepository.findByStaffCode("EMP-014").ifPresent(s -> link(s, cashier));

        log.warn("""

                ================================================================
                 Initial accounts created with the DEVELOPMENT password.
                   admin   / ChangeMe123!   (ADMIN)
                   cashier / ChangeMe123!   (CASHIER)
                 Change both before this reaches anything but localhost.
                ================================================================
                """);
    }

    private UserInfm createUser(String userId, String name, Role role, String email, String bizSuffix) {
        UserInfm user = UserInfm.builder()
                .bizKey(DEFAULT_BIZ_KEY.substring(0, 3) + bizSuffix)
                .userId(userId)
                .userNm(name)
                .userPwd(passwordEncoder.encode("ChangeMe123!"))
                .eml(email)
                .role(role)
                .lockYn("N")
                .loginFailedCnt(0)
                .actYn("Y")
                .regId("system")
                .regDtm(LocalDateTime.now())
                .build();
        return userRepository.save(user);
    }

    private void link(Staff staff, UserInfm user) {
        staff.setUser(user);
        staffRepository.save(staff);
    }
}
