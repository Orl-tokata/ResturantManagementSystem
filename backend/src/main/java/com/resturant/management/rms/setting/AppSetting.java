package com.resturant.management.rms.setting;

import com.resturant.management.rms.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Key/value configuration editable from {@code /admin/settings} — restaurant
 * details, VAT rate, KHR rate, feature toggles.
 */
@Entity
@Table(name = "app_setting")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppSetting extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String settingKey;

    @Column(name = "setting_value", length = 1000)
    private String settingValue;

    @Column(name = "description", length = 255)
    private String description;
}
