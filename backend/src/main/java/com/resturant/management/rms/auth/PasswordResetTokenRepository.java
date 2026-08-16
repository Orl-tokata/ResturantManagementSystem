package com.resturant.management.rms.auth;

import com.resturant.management.rms.user.UserInfm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findFirstByUserAndOtpCodeAndUsedYnOrderByCreatedAtDesc(
            UserInfm user, String otpCode, String usedYn);

    /** Invalidate any outstanding tokens before issuing a new one. */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.usedYn = 'Y' WHERE t.user = :user AND t.usedYn = 'N'")
    int invalidateAllForUser(@Param("user") UserInfm user);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
