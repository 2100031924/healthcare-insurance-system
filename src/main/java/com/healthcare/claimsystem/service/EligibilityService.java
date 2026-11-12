package com.healthcare.claimsystem.service;

import com.healthcare.claimsystem.dto.eligibility.EligibilityRequest;
import com.healthcare.claimsystem.dto.eligibility.EligibilityResponse;

public interface EligibilityService {

    EligibilityResponse verify(EligibilityRequest request);
}
