package com.healthcare.claimsystem.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI healthcareOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Healthcare Insurance Claim & Pre-Authorization Management System")
                        .description("REST API for policy management, eligibility verification, pre-authorization, "
                                + "claim adjudication, duplicate detection, settlements and audit management.")
                        .version("1.0.0")
                        .contact(new Contact().name("Healthcare Insurance Platform"))
                        .license(new License().name("Proprietary")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME, new SecurityScheme()
                                .name(SECURITY_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }
}
