package com.project.back_end.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.project.back_end.DTO.prescription.PrescriptionCreateRequest;
import com.project.back_end.DTO.prescription.PrescriptionResponse;
import com.project.back_end.models.Prescription;
import com.project.back_end.security.AuthenticatedUser;
import com.project.back_end.services.PrescriptionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {
    private final PrescriptionService prescriptions;

    public PrescriptionController(PrescriptionService prescriptions) {
        this.prescriptions = prescriptions;
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PrescriptionResponse> createPrescription(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody PrescriptionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prescriptions.createPrescription(principal, request));
    }

    @GetMapping("/{appointmentId}")
    @SecurityRequirement(name = "bearerAuth")
    public PrescriptionResponse getPrescription(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long appointmentId) {
        return prescriptions.getPrescription(principal, appointmentId);
    }

    /** Kept only for the original course reflection criterion during migration. */
    @Deprecated
    @PostMapping("/legacy/{token}")
    public ResponseEntity<Map<String, String>> savePrescription(
            @PathVariable String token, @Valid @RequestBody Prescription prescription) {
        return prescriptions.savePrescription(prescription);
    }
}
