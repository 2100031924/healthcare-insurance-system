package com.healthcare.claimsystem.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimFingerprintTest {

    private static final LocalDate ADMISSION_DATE = LocalDate.of(2026, 10, 1);

    @Test
    void generatesStableFingerprintForIdenticalClaims() {
        String first = ClaimFingerprint.generate(
                1L, 2L, 3L, "PROC-100", ADMISSION_DATE, ADMISSION_DATE.plusDays(2), new BigDecimal("15000.00")
        );
        String second = ClaimFingerprint.generate(
                1L, 2L, 3L, " proc-100 ", ADMISSION_DATE, ADMISSION_DATE.plusDays(2), new BigDecimal("15000")
        );

        assertThat(first).hasSize(64).isEqualTo(second);
    }

    @Test
    void generatesDifferentFingerprintWhenClaimDetailsChange() {
        String base = ClaimFingerprint.generate(
                1L, 2L, 3L, "PROC-100", ADMISSION_DATE, ADMISSION_DATE.plusDays(2), new BigDecimal("15000.00")
        );
        String differentAmount = ClaimFingerprint.generate(
                1L, 2L, 3L, "PROC-100", ADMISSION_DATE, ADMISSION_DATE.plusDays(2), new BigDecimal("16000.00")
        );
        String differentHospital = ClaimFingerprint.generate(
                1L, 2L, 4L, "PROC-100", ADMISSION_DATE, ADMISSION_DATE.plusDays(2), new BigDecimal("15000.00")
        );

        assertThat(base).isNotEqualTo(differentAmount).isNotEqualTo(differentHospital);
    }
}
