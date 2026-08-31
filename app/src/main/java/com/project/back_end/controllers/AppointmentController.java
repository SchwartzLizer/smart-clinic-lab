package com.project.back_end.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.project.back_end.DTO.appointment.AppointmentCreateRequest;
import com.project.back_end.DTO.appointment.AppointmentResponse;
import com.project.back_end.DTO.appointment.AppointmentUpdateRequest;
import com.project.back_end.security.AuthenticatedUser;
import com.project.back_end.services.AppointmentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService appointments;

    public AppointmentController(AppointmentService appointments) {
        this.appointments = appointments;
    }

    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    public List<AppointmentResponse> getAppointments(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String patientName) {
        return appointments.listAppointments(principal, status, date, patientName);
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AppointmentResponse> createAppointment(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody AppointmentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointments.createAppointment(principal, request));
    }

    @PutMapping("/{appointmentId}")
    @SecurityRequirement(name = "bearerAuth")
    public AppointmentResponse updateAppointment(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long appointmentId,
            @Valid @RequestBody AppointmentUpdateRequest request) {
        return appointments.updateAppointment(principal, appointmentId, request);
    }

    @DeleteMapping("/{appointmentId}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteAppointment(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long appointmentId) {
        appointments.deleteAppointment(principal, appointmentId);
        return ResponseEntity.noContent().build();
    }
}
