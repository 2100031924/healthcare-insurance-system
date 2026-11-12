package com.healthcare.claimsystem.dto.hospital;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HospitalRequest(
        @Size(max = 30, message = "Hospital code must not exceed 30 characters")
        @Pattern(regexp = "^$|^[A-Za-z0-9-]+$", message = "Hospital code may contain only letters, digits and hyphens")
        String hospitalCode,

        @NotBlank(message = "Hospital name is required")
        @Size(max = 150, message = "Hospital name must not exceed 150 characters")
        String name,

        @NotBlank(message = "License number is required")
        @Size(max = 60, message = "License number must not exceed 60 characters")
        String licenseNumber,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 120, message = "Email must not exceed 120 characters")
        String email,

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[0-9+\\-() ]{7,20}$", message = "Phone number is invalid")
        String phone,

        @NotBlank(message = "Address is required")
        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address,

        @NotBlank(message = "Contact person is required")
        @Size(max = 120, message = "Contact person must not exceed 120 characters")
        String contactPerson,

        Boolean active
) {
}
