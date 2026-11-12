package com.healthcare.claimsystem.dto.hospital;

import java.time.Instant;

public record HospitalResponse(
        Long id,
        String hospitalCode,
        String name,
        String licenseNumber,
        String email,
        String phone,
        String address,
        String contactPerson,
        boolean active,
        Instant createdAt
) {
}
