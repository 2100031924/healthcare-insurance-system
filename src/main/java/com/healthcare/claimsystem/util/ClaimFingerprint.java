package com.healthcare.claimsystem.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Locale;

public final class ClaimFingerprint {

    private ClaimFingerprint() {
    }

    public static String generate(
            Long patientId,
            Long policyId,
            Long hospitalId,
            String procedureCode,
            LocalDate admissionDate,
            LocalDate dischargeDate,
            BigDecimal claimedAmount
    ) {
        String normalizedAmount = claimedAmount.setScale(2, RoundingMode.HALF_UP).toPlainString();
        String payload = String.join(
                "|",
                String.valueOf(patientId),
                String.valueOf(policyId),
                String.valueOf(hospitalId),
                procedureCode.trim().toUpperCase(Locale.ROOT),
                admissionDate.toString(),
                dischargeDate == null ? "" : dischargeDate.toString(),
                normalizedAmount
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }
}
