package com.resturant.management.rms.user;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Login account. Carried over from NIEI-Y4 including the lockout behaviour;
 * audit timestamps are real {@code TIMESTAMP} columns here rather than strings.
 */
@Entity
@Table(name = "users_infm")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfm implements UserDetails {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "biz_key", nullable = false, unique = true, length = 10)
    private String bizKey;

    @Column(name = "user_id", nullable = false, unique = true, length = 50)
    private String userId;

    @Column(name = "user_nm", nullable = false, length = 100)
    private String userNm;

    @Column(name = "user_pwd", nullable = false)
    private String userPwd;

    @Column(name = "tel", length = 30)
    private String tel;

    @Column(name = "eml", length = 120)
    private String eml;

    @Column(name = "usr_img")
    private String usrImg;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Builder.Default
    @Column(name = "lock_yn", nullable = false, length = 1)
    private String lockYn = "N";

    @Builder.Default
    @Column(name = "login_failed_cnt", nullable = false)
    private Integer loginFailedCnt = 0;

    @Column(name = "lst_lgn_dtm")
    private LocalDateTime lstLgnDtm;

    @Builder.Default
    @Column(name = "act_yn", nullable = false, length = 1)
    private String actYn = "Y";

    @Column(name = "reg_id", length = 50)
    private String regId;

    @Column(name = "reg_dtm")
    private LocalDateTime regDtm;

    @Column(name = "mod_id", length = 50)
    private String modId;

    @Column(name = "mod_dtm")
    private LocalDateTime modDtm;

    /* ---- UserDetails ---------------------------------------------------- */

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security's hasRole() expects the ROLE_ prefix.
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return userId;
    }

    @Override
    public String getPassword() {
        return userPwd;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !"Y".equals(lockYn);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "Y".equals(actYn);
    }

    /* ---- Lockout -------------------------------------------------------- */

    public void incrementFailedLoginAttempts() {
        this.loginFailedCnt = (this.loginFailedCnt == null) ? 1 : this.loginFailedCnt + 1;
        if (this.loginFailedCnt >= MAX_FAILED_ATTEMPTS) {
            this.lockYn = "Y";
        }
    }

    public void resetFailedLoginAttempts() {
        this.loginFailedCnt = 0;
        this.lockYn = "N";
    }

    public boolean isLocked() {
        return "Y".equals(this.lockYn);
    }
}
