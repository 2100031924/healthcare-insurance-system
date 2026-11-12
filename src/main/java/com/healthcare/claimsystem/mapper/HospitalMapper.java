package com.healthcare.claimsystem.mapper;

import com.healthcare.claimsystem.dto.hospital.HospitalResponse;
import com.healthcare.claimsystem.entity.Hospital;

public final class HospitalMapper {

    private HospitalMapper() {
    }

    public static HospitalResponse toResponse(Hospital hospital) {
        return new HospitalResponse(
                hospital.getId(),
                hospital.getHospitalCode(),
                hospital.getName(),
                hospital.getLicenseNumber(),
                hospital.getEmail(),
                hospital.getPhone(),
                hospital.getAddress(),
                hospital.getContactPerson(),
                hospital.isActive(),
                hospital.getCreatedAt()
        );
    }
}
