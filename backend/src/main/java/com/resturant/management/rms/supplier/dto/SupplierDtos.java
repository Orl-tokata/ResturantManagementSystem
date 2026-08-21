package com.resturant.management.rms.supplier.dto;

import com.resturant.management.rms.common.RecordStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public final class SupplierDtos {

    private SupplierDtos() {
    }

    public record SupplierRequest(
            @NotBlank(message = "{valid.required}") @Size(max = 20) String supplierCode,
            @NotBlank(message = "{valid.required}") @Size(max = 150) String company,
            @Size(max = 100) String contactPerson,
            @Size(max = 30) String phone,
            @Email(message = "{valid.email}") @Size(max = 120) String email,
            @Size(max = 30) String supplyType,
            @Size(max = 500) String address,
            RecordStatus status
    ) {}

    public record SupplierResponse(
            Long id,
            String supplierCode,
            String company,
            String contactPerson,
            String phone,
            String email,
            String supplyType,
            String address,
            BigDecimal balance,
            RecordStatus status
    ) {}
}
