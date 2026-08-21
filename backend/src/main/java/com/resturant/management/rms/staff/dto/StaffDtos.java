package com.resturant.management.rms.staff.dto;

import com.resturant.management.rms.staff.Gender;
import com.resturant.management.rms.staff.Shift;
import com.resturant.management.rms.staff.StaffStatus;
import com.resturant.management.rms.user.Role;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class StaffDtos {

    private StaffDtos() {
    }

    public record StaffRequest(
            @NotBlank(message = "{valid.required}") @Size(max = 20) String staffCode,
            @NotBlank(message = "{valid.required}") @Size(max = 100) String staffName,
            Gender gender,
            @Past(message = "{valid.past}") LocalDate dateOfBirth,
            @Size(max = 30) String phone,
            @Email(message = "{valid.email}") @Size(max = 120) String email,
            @NotNull(message = "{valid.required}") Role role,
            Shift shift,
            @PositiveOrZero(message = "{valid.notNegative}") BigDecimal salary,
            LocalDate hireDate,
            @Size(max = 500) String address,
            StaffStatus status
    ) {}

    public record StaffResponse(
            Long id,
            String staffCode,
            String staffName,
            Gender gender,
            LocalDate dateOfBirth,
            String phone,
            String email,
            Role role,
            Shift shift,
            BigDecimal salary,
            LocalDate hireDate,
            String address,
            StaffStatus status,
            Long userId
    ) {}
}
