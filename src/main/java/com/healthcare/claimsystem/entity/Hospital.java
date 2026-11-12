package com.healthcare.claimsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "hospitals",
        indexes = {
                @Index(name = "idx_hospitals_name", columnList = "name"),
                @Index(name = "idx_hospitals_active", columnList = "active")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Hospital extends BaseEntity {

    @Column(name = "hospital_code", nullable = false, unique = true, length = 30)
    private String hospitalCode;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "license_number", nullable = false, unique = true, length = 60)
    private String licenseNumber;

    @Column(name = "email", nullable = false, length = 120)
    private String email;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "contact_person", nullable = false, length = 120)
    private String contactPerson;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
