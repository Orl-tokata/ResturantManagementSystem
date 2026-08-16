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
            @NotBlank(message = "is required") @Size(max = 20) String staffCode,
            @NotBlank(message = "is required") @Size(max = 100) String staffName,
            Gender gender,
            @Past(message = "must be in the past") LocalDate dateOfBirth,
            @Size(max = 30) String phone,
            @Email(message = "must be a valid email address") @Size(max = 120) String email,
            @NotNull(message = "is required") Role role,
            Shift shift,
            @PositiveOrZero(message = "cannot be negative") BigDecimal salary,
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
