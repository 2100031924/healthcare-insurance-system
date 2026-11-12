CREATE DATABASE IF NOT EXISTS healthcare_insurance
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE healthcare_insurance;

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(120) NOT NULL,
    password VARCHAR(100) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_enabled ON users (enabled);

CREATE TABLE hospitals (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    hospital_code VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    license_number VARCHAR(60) NOT NULL,
    email VARCHAR(120) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address VARCHAR(255) NOT NULL,
    contact_person VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_hospitals_code UNIQUE (hospital_code),
    CONSTRAINT uk_hospitals_license UNIQUE (license_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_hospitals_name ON hospitals (name);
CREATE INDEX idx_hospitals_active ON hospitals (active);

CREATE TABLE patients (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    patient_code VARCHAR(30) NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(20) NOT NULL,
    email VARCHAR(120) NULL,
    phone VARCHAR(20) NOT NULL,
    address VARCHAR(255) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_patients_code UNIQUE (patient_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_patients_last_name ON patients (last_name);
CREATE INDEX idx_patients_active ON patients (active);

CREATE TABLE insurance_policies (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    policy_number VARCHAR(50) NOT NULL,
    patient_id BIGINT NOT NULL,
    provider_name VARCHAR(150) NOT NULL,
    policy_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    sum_insured DECIMAL(14, 2) NOT NULL,
    utilized_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    deductible_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    co_payment_percentage DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    waiting_period_days INT NOT NULL DEFAULT 0,
    premium_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    renewal_count INT NOT NULL DEFAULT 0,
    last_renewed_at DATETIME(6) NULL,
    cancellation_reason VARCHAR(500) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_policies_number UNIQUE (policy_number),
    CONSTRAINT fk_policies_patient FOREIGN KEY (patient_id) REFERENCES patients (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_policies_patient ON insurance_policies (patient_id);
CREATE INDEX idx_policies_status ON insurance_policies (status);
CREATE INDEX idx_policies_end_date ON insurance_policies (end_date);

CREATE TABLE policy_coverages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    policy_id BIGINT NOT NULL,
    coverage_type VARCHAR(30) NOT NULL,
    coverage_limit DECIMAL(14, 2) NOT NULL,
    utilized_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_policy_coverage_type UNIQUE (policy_id, coverage_type),
    CONSTRAINT fk_coverages_policy FOREIGN KEY (policy_id) REFERENCES insurance_policies (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_coverages_policy ON policy_coverages (policy_id);

CREATE TABLE policy_exclusions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    policy_id BIGINT NOT NULL,
    treatment_category VARCHAR(30) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_policy_exclusion_type UNIQUE (policy_id, treatment_category),
    CONSTRAINT fk_exclusions_policy FOREIGN KEY (policy_id) REFERENCES insurance_policies (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_exclusions_policy ON policy_exclusions (policy_id);

CREATE TABLE pre_authorizations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    reference_number VARCHAR(40) NOT NULL,
    patient_id BIGINT NOT NULL,
    policy_id BIGINT NOT NULL,
    hospital_id BIGINT NOT NULL,
    treating_doctor VARCHAR(120) NOT NULL,
    diagnosis_code VARCHAR(20) NOT NULL,
    procedure_code VARCHAR(20) NOT NULL,
    treatment_category VARCHAR(30) NOT NULL,
    treatment_description VARCHAR(500) NOT NULL,
    estimated_cost DECIMAL(14, 2) NOT NULL,
    requested_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    approved_amount DECIMAL(14, 2) NULL,
    valid_until DATE NULL,
    remarks VARCHAR(1000) NULL,
    requested_by BIGINT NULL,
    reviewed_by BIGINT NULL,
    decision_date DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_preauth_reference UNIQUE (reference_number),
    CONSTRAINT fk_preauth_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_preauth_policy FOREIGN KEY (policy_id) REFERENCES insurance_policies (id),
    CONSTRAINT fk_preauth_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals (id),
    CONSTRAINT fk_preauth_requested_by FOREIGN KEY (requested_by) REFERENCES users (id),
    CONSTRAINT fk_preauth_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_preauth_patient ON pre_authorizations (patient_id);
CREATE INDEX idx_preauth_hospital ON pre_authorizations (hospital_id);
CREATE INDEX idx_preauth_policy ON pre_authorizations (policy_id);
CREATE INDEX idx_preauth_status ON pre_authorizations (status);
CREATE INDEX idx_preauth_requested_date ON pre_authorizations (requested_date);

CREATE TABLE claims (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    claim_number VARCHAR(40) NOT NULL,
    claim_fingerprint VARCHAR(64) NOT NULL,
    patient_id BIGINT NOT NULL,
    policy_id BIGINT NOT NULL,
    hospital_id BIGINT NOT NULL,
    pre_authorization_id BIGINT NULL,
    treating_doctor VARCHAR(120) NOT NULL,
    diagnosis_code VARCHAR(20) NOT NULL,
    procedure_code VARCHAR(20) NOT NULL,
    treatment_category VARCHAR(30) NOT NULL,
    admission_date DATE NOT NULL,
    discharge_date DATE NULL,
    claim_date DATE NOT NULL,
    claimed_amount DECIMAL(14, 2) NOT NULL,
    invoice_number VARCHAR(80) NOT NULL,
    description VARCHAR(1000) NULL,
    status VARCHAR(30) NOT NULL,
    deductible_applied DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    co_payment_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    non_covered_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    approved_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    patient_responsibility DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    rejection_reason VARCHAR(1000) NULL,
    adjudication_notes VARCHAR(1000) NULL,
    submitted_by BIGINT NULL,
    reviewed_by BIGINT NULL,
    adjudicated_by BIGINT NULL,
    reviewed_at DATETIME(6) NULL,
    adjudicated_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_claims_number UNIQUE (claim_number),
    CONSTRAINT uk_claims_fingerprint UNIQUE (claim_fingerprint),
    CONSTRAINT uk_claims_hospital_invoice UNIQUE (hospital_id, invoice_number),
    CONSTRAINT fk_claims_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_claims_policy FOREIGN KEY (policy_id) REFERENCES insurance_policies (id),
    CONSTRAINT fk_claims_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals (id),
    CONSTRAINT fk_claims_preauth FOREIGN KEY (pre_authorization_id) REFERENCES pre_authorizations (id),
    CONSTRAINT fk_claims_submitted_by FOREIGN KEY (submitted_by) REFERENCES users (id),
    CONSTRAINT fk_claims_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users (id),
    CONSTRAINT fk_claims_adjudicated_by FOREIGN KEY (adjudicated_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_claims_patient ON claims (patient_id);
CREATE INDEX idx_claims_hospital ON claims (hospital_id);
CREATE INDEX idx_claims_policy ON claims (policy_id);
CREATE INDEX idx_claims_status ON claims (status);
CREATE INDEX idx_claims_claim_date ON claims (claim_date);

CREATE TABLE claim_settlements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    settlement_number VARCHAR(40) NOT NULL,
    claim_id BIGINT NOT NULL,
    approved_amount DECIMAL(14, 2) NOT NULL,
    paid_amount DECIMAL(14, 2) NOT NULL,
    patient_responsibility DECIMAL(14, 2) NOT NULL,
    payment_mode VARCHAR(30) NOT NULL,
    payment_reference VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    processed_by BIGINT NULL,
    processed_at DATETIME(6) NOT NULL,
    remarks VARCHAR(1000) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_settlements_number UNIQUE (settlement_number),
    CONSTRAINT uk_settlements_claim UNIQUE (claim_id),
    CONSTRAINT uk_settlements_reference UNIQUE (payment_reference),
    CONSTRAINT fk_settlements_claim FOREIGN KEY (claim_id) REFERENCES claims (id),
    CONSTRAINT fk_settlements_processed_by FOREIGN KEY (processed_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_settlements_claim ON claim_settlements (claim_id);
CREATE INDEX idx_settlements_status ON claim_settlements (status);

CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    claim_id BIGINT NULL,
    entity_type VARCHAR(30) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    performed_by BIGINT NULL,
    details VARCHAR(2000) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_audit_claim FOREIGN KEY (claim_id) REFERENCES claims (id),
    CONSTRAINT fk_audit_performed_by FOREIGN KEY (performed_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_claim ON audit_logs (claim_id);
CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_action ON audit_logs (action);
CREATE INDEX idx_audit_created_at ON audit_logs (created_at);
