package com.healthcare.claimsystem.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@ConfigurationProperties(prefix = "app")
@Validated
@Getter
@Setter
public class AppProperties {

    @Valid
    private Jwt jwt = new Jwt();

    @Valid
    private Cors cors = new Cors();

    @Valid
    private BootstrapAdmin bootstrapAdmin = new BootstrapAdmin();

    @Valid
    private DemoData demoData = new DemoData();

    @Valid
    private Scheduling scheduling = new Scheduling();

    @Getter
    @Setter
    public static class Jwt {

        @NotBlank(message = "JWT secret is required")
        @Size(min = 32, message = "JWT secret must be at least 32 characters")
        private String secret;

        @NotBlank(message = "JWT issuer is required")
        private String issuer = "healthcare-insurance-system";

        @Min(value = 1, message = "Access token expiration must be at least one minute")
        private long accessTokenExpirationMinutes = 60;

        @Min(value = 1, message = "Refresh token expiration must be at least one day")
        private long refreshTokenExpirationDays = 7;
    }

    @Getter
    @Setter
    public static class Cors {

        @NotEmpty(message = "At least one CORS origin is required")
        private List<String> allowedOrigins = List.of("http://localhost:3000", "http://localhost:8080");
    }

    @Getter
    @Setter
    public static class BootstrapAdmin {

        @NotBlank(message = "Bootstrap admin username is required")
        private String username = "admin";

        @NotBlank(message = "Bootstrap admin email is required")
        @Email(message = "Bootstrap admin email must be valid")
        private String email = "admin@healthcare.com";

        @NotBlank(message = "Bootstrap admin password is required")
        private String password = "Admin@12345";

        @NotBlank(message = "Bootstrap admin full name is required")
        private String fullName = "System Administrator";
    }

    @Getter
    @Setter
    public static class DemoData {

        private boolean enabled;
    }

    @Getter
    @Setter
    public static class Scheduling {

        private boolean enabled = true;
    }
}
