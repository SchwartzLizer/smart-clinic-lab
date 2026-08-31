package com.project.back_end.DTO.patient;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PatientRegistrationRequest(
        @NotBlank @Size(min = 3, max = 100) String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password,
        @NotBlank @Pattern(regexp = "\\d{10}", message = "Phone number must be 10 digits") String phone,
        @NotBlank @Size(max = 255) String address) {
}
