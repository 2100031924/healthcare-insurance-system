package com.healthcare.claimsystem.service.impl;

import com.healthcare.claimsystem.dto.patient.PatientRequest;
import com.healthcare.claimsystem.dto.patient.PatientResponse;
import com.healthcare.claimsystem.entity.Patient;
import com.healthcare.claimsystem.entity.enums.AuditAction;
import com.healthcare.claimsystem.entity.enums.AuditEntityType;
import com.healthcare.claimsystem.exception.ResourceNotFoundException;
import com.healthcare.claimsystem.mapper.PatientMapper;
import com.healthcare.claimsystem.repository.PatientRepository;
import com.healthcare.claimsystem.service.AuditService;
import com.healthcare.claimsystem.service.PatientService;
import com.healthcare.claimsystem.specification.PatientSpecifications;
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
public class PatientServiceImpl implements PatientService {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "patientCode", "firstName", "lastName", "dateOfBirth");

    private final PatientRepository patientRepository;
    private final ReferenceNumberGenerator referenceNumberGenerator;
    private final AuditService auditService;

    @Override
    @Transactional
    public PatientResponse create(PatientRequest request) {
        Patient patient = new Patient();
        patient.setPatientCode(referenceNumberGenerator.generate("PAT"));
        apply(patient, request);
        Patient saved = patientRepository.save(patient);
        auditService.recordEntity(
                AuditEntityType.PATIENT,
                saved.getId(),
                AuditAction.PATIENT_CREATED,
                "Patient " + saved.getPatientCode() + " created"
        );
        return PatientMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PatientResponse update(Long id, PatientRequest request) {
        Patient patient = findPatient(id);
        apply(patient, request);
        Patient saved = patientRepository.save(patient);
        auditService.recordEntity(
                AuditEntityType.PATIENT,
                saved.getId(),
                AuditAction.PATIENT_UPDATED,
                "Patient " + saved.getPatientCode() + " updated"
        );
        return PatientMapper.toResponse(saved);
    }

    @Override
    public PatientResponse get(Long id) {
        return PatientMapper.toResponse(findPatient(id));
    }

    @Override
    public Page<PatientResponse> list(Boolean active, String search, Pageable pageable) {
        Pageable sanitized = PageableUtils.sanitize(pageable, SORT_FIELDS, "createdAt");
        return patientRepository.findAll(PatientSpecifications.withFilters(active, search), sanitized)
                .map(PatientMapper::toResponse);
    }

    private void apply(Patient patient, PatientRequest request) {
        patient.setFirstName(request.firstName().trim());
        patient.setLastName(request.lastName().trim());
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setGender(request.gender());
        patient.setEmail(request.email() == null ? null : request.email().trim().toLowerCase());
        patient.setPhone(request.phone().trim());
        patient.setAddress(request.address() == null ? null : request.address().trim());
        if (request.active() != null) {
            patient.setActive(request.active());
        }
    }

    private Patient findPatient(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));
    }
}
