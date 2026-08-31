package com.project.back_end.DTO.doctor;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DoctorCreateRequest(
        @NotBlank @Size(min = 3, max = 100) String name,
        @NotBlank @Size(min = 3, max = 50) String specialty,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password,
        @NotBlank @Pattern(regexp = "\\d{10}") String phone,
        List<String> availableTimes) {
}
