package com.healthcare.claimsystem.mapper;

import com.healthcare.claimsystem.dto.patient.PatientResponse;
import com.healthcare.claimsystem.entity.Patient;

public final class PatientMapper {

    private PatientMapper() {
    }

    public static PatientResponse toResponse(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getPatientCode(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getFirstName() + " " + patient.getLastName(),
                patient.getDateOfBirth(),
                patient.getGender(),
                patient.getEmail(),
                patient.getPhone(),
                patient.getAddress(),
                patient.isActive(),
                patient.getCreatedAt()
        );
    }
}
