package com.healthcare.claimsystem.service;

import com.healthcare.claimsystem.dto.hospital.HospitalRequest;
import com.healthcare.claimsystem.dto.hospital.HospitalResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HospitalService {

    HospitalResponse create(HospitalRequest request);

    HospitalResponse update(Long id, HospitalRequest request);

    HospitalResponse get(Long id);

    Page<HospitalResponse> list(Boolean active, String search, Pageable pageable);
}
