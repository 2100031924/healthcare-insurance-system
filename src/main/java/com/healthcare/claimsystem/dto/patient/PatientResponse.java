package com.healthcare.claimsystem.dto.patient;

import com.healthcare.claimsystem.entity.enums.Gender;

import java.time.Instant;
import java.time.LocalDate;

public record PatientResponse(
        Long id,
        String patientCode,
        String firstName,
        String lastName,
        String fullName,
        LocalDate dateOfBirth,
        Gender gender,
        String email,
        String phone,
        String address,
        boolean active,
        Instant createdAt
) {
}
