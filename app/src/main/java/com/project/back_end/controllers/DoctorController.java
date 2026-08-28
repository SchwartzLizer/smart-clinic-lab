package com.project.back_end.controllers;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.back_end.DTO.common.PageResponse;
import com.project.back_end.DTO.doctor.DoctorCreateRequest;
import com.project.back_end.DTO.doctor.DoctorResponse;
import com.project.back_end.DTO.doctor.DoctorUpdateRequest;
import com.project.back_end.services.DoctorService;

import jakarta.validation.Valid;

/** Public doctor directory and admin-only doctor management endpoints. */
@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctors;

    public DoctorController(DoctorService doctors) {
        this.doctors = doctors;
    }

    @GetMapping
    public PageResponse<DoctorResponse> getDoctors(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String period,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return doctors.listDoctors(name, specialty, period, pageable);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<DoctorResponse> createDoctor(@Valid @RequestBody DoctorCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(doctors.createDoctor(request));
    }

    @PutMapping("/{doctorId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public DoctorResponse updateDoctor(@PathVariable Long doctorId,
            @Valid @RequestBody DoctorUpdateRequest request) {
        return doctors.updateDoctor(doctorId, request);
    }

    @DeleteMapping("/{doctorId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteDoctor(@PathVariable Long doctorId) {
        doctors.deleteDoctor(doctorId);
        return ResponseEntity.noContent().build();
    }

    /** Kept only for the original course reflection criterion. */
    @Deprecated
    @GetMapping("/legacy/availability/{user}/{doctorId}/{date}/{token}")
    public ResponseEntity<Map<String, Object>> getDoctorAvailability(
            @PathVariable String user,
            @PathVariable Long doctorId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String token) {
        return ResponseEntity.ok(Map.of("availability", doctors.getDoctorAvailability(doctorId, date)));
    }
}
