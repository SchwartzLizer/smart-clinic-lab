package com.project.back_end.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.project.back_end.DTO.patient.PatientResponse;
import com.project.back_end.security.AuthenticatedUser;
import com.project.back_end.services.PatientService;

@RestController
@RequestMapping("/api/patients")
public class PatientController {
    private final PatientService patients;

    public PatientController(PatientService patients) {
        this.patients = patients;
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public PatientResponse getMyProfile(@AuthenticationPrincipal AuthenticatedUser principal) {
        return patients.getMyProfile(principal.accountId());
    }
}
