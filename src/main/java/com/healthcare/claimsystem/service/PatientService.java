package com.healthcare.claimsystem.service;

import com.healthcare.claimsystem.dto.patient.PatientRequest;
import com.healthcare.claimsystem.dto.patient.PatientResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PatientService {

    PatientResponse create(PatientRequest request);

    PatientResponse update(Long id, PatientRequest request);

    PatientResponse get(Long id);

    Page<PatientResponse> list(Boolean active, String search, Pageable pageable);
}
