package com.healthcare.claimsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HealthcareInsuranceSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthcareInsuranceSystemApplication.class, args);
    }

}
