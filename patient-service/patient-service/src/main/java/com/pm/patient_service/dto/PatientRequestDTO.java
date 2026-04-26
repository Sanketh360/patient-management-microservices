package com.pm.patient_service.dto;

import com.pm.patient_service.dto.validators.CreatePatientValidationGroup;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class PatientRequestDTO {
    @NotBlank
    @Size(max = 100 ,message = "Name cannot exceed 100 character")
    private String name;

    @NotBlank(message = "Email is Required")
    @Email(message = "Email should be Valid")
    private String email;

    @NotBlank(message = "Address is Required")
    private String address;

    @NotNull(message = "Date of Birth is Required")
    private LocalDate dateOfBirth;


    @NotNull(groups = CreatePatientValidationGroup.class, message =
            "Registered date is required")
    private LocalDate registeredDate;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public LocalDate getRegisteredDate() {
        return registeredDate;
    }

    public void setRegisteredDate(LocalDate registeredDate) {
        this.registeredDate = registeredDate;
    }
}
