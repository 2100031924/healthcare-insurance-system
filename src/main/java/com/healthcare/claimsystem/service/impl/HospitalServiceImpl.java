package com.healthcare.claimsystem.service.impl;

import com.healthcare.claimsystem.dto.hospital.HospitalRequest;
import com.healthcare.claimsystem.dto.hospital.HospitalResponse;
import com.healthcare.claimsystem.entity.Hospital;
import com.healthcare.claimsystem.entity.enums.AuditAction;
import com.healthcare.claimsystem.entity.enums.AuditEntityType;
import com.healthcare.claimsystem.exception.BusinessRuleException;
import com.healthcare.claimsystem.exception.DuplicateResourceException;
import com.healthcare.claimsystem.exception.ResourceNotFoundException;
import com.healthcare.claimsystem.mapper.HospitalMapper;
import com.healthcare.claimsystem.repository.HospitalRepository;
import com.healthcare.claimsystem.service.AuditService;
import com.healthcare.claimsystem.service.HospitalService;
import com.healthcare.claimsystem.specification.HospitalSpecifications;
import com.healthcare.claimsystem.util.PageableUtils;
import com.healthcare.claimsystem.util.ReferenceNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HospitalServiceImpl implements HospitalService {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "hospitalCode", "name", "active");

    private final HospitalRepository hospitalRepository;
    private final ReferenceNumberGenerator referenceNumberGenerator;
    private final AuditService auditService;

    @Override
    @Transactional
    public HospitalResponse create(HospitalRequest request) {
        String hospitalCode = request.hospitalCode() == null || request.hospitalCode().isBlank()
                ? referenceNumberGenerator.generate("HSP")
                : request.hospitalCode().trim().toUpperCase();
        String licenseNumber = request.licenseNumber().trim();
        if (hospitalRepository.existsByHospitalCode(hospitalCode)) {
            throw new DuplicateResourceException("Hospital code already exists: " + hospitalCode);
        }
        if (hospitalRepository.existsByLicenseNumber(licenseNumber)) {
            throw new DuplicateResourceException("Hospital license number already exists: " + licenseNumber);
        }

        Hospital hospital = new Hospital();
        hospital.setHospitalCode(hospitalCode);
        hospital.setName(request.name().trim());
        hospital.setLicenseNumber(licenseNumber);
        hospital.setEmail(request.email().trim().toLowerCase());
        hospital.setPhone(request.phone().trim());
        hospital.setAddress(request.address().trim());
        hospital.setContactPerson(request.contactPerson().trim());
        hospital.setActive(request.active() == null || request.active());
        Hospital saved = hospitalRepository.save(hospital);
        auditService.recordEntity(
                AuditEntityType.HOSPITAL,
                saved.getId(),
                AuditAction.HOSPITAL_CREATED,
                "Hospital " + saved.getHospitalCode() + " created"
        );
        return HospitalMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public HospitalResponse update(Long id, HospitalRequest request) {
        Hospital hospital = findHospital(id);
        String licenseNumber = request.licenseNumber().trim();
        if (hospitalRepository.existsByLicenseNumberAndIdNot(licenseNumber, id)) {
            throw new DuplicateResourceException("Hospital license number already exists: " + licenseNumber);
        }

        hospital.setName(request.name().trim());
        hospital.setLicenseNumber(licenseNumber);
        hospital.setEmail(request.email().trim().toLowerCase());
        hospital.setPhone(request.phone().trim());
        hospital.setAddress(request.address().trim());
        hospital.setContactPerson(request.contactPerson().trim());
        if (request.active() != null) {
            hospital.setActive(request.active());
        }
        Hospital saved = hospitalRepository.save(hospital);
        auditService.recordEntity(
                AuditEntityType.HOSPITAL,
                saved.getId(),
                AuditAction.HOSPITAL_UPDATED,
                "Hospital " + saved.getHospitalCode() + " updated"
        );
        return HospitalMapper.toResponse(saved);
    }

    @Override
    public HospitalResponse get(Long id) {
        return HospitalMapper.toResponse(findHospital(id));
    }

    @Override
    public Page<HospitalResponse> list(Boolean active, String search, Pageable pageable) {
        Pageable sanitized = PageableUtils.sanitize(pageable, SORT_FIELDS, "createdAt");
        return hospitalRepository.findAll(HospitalSpecifications.withFilters(active, search), sanitized)
                .map(HospitalMapper::toResponse);
    }

    private Hospital findHospital(Long id) {
        return hospitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital", "id", id));
    }
}
