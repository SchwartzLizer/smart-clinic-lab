package com.project.back_end.services;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.back_end.DTO.prescription.PrescriptionCreateRequest;
import com.project.back_end.DTO.prescription.PrescriptionResponse;
import com.project.back_end.exceptions.ForbiddenOperationException;
import com.project.back_end.exceptions.ResourceConflictException;
import com.project.back_end.exceptions.ResourceNotFoundException;
import com.project.back_end.mappers.PrescriptionMapper;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Prescription;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.PrescriptionRepository;
import com.project.back_end.security.AuthenticatedUser;
import com.project.back_end.security.Role;

@Service
public class PrescriptionService {
    private final PrescriptionRepository prescriptions;
    private final AppointmentRepository appointments;
    private final PrescriptionMapper mapper;

    /** Compatibility constructor for course-era tests/callers. */
    public PrescriptionService(PrescriptionRepository prescriptions) {
        this(prescriptions, null, new PrescriptionMapper());
    }

    public PrescriptionService(PrescriptionRepository prescriptions, AppointmentRepository appointments) {
        this(prescriptions, appointments, new PrescriptionMapper());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public PrescriptionService(PrescriptionRepository prescriptions, AppointmentRepository appointments,
            PrescriptionMapper mapper) {
        this.prescriptions = prescriptions;
        this.appointments = appointments;
        this.mapper = mapper;
    }

    /**
     * MongoDB and MySQL do not share an atomic transaction. The prescription is
     * written first, then the appointment status is updated. A retry is safe
     * because appointment ID duplicate detection prevents a second document.
     */
    @Transactional
    public PrescriptionResponse createPrescription(AuthenticatedUser principal, PrescriptionCreateRequest request) {
        requireRole(principal, Role.DOCTOR);
        Appointment appointment = appointment(request.appointmentId());
        verifyDoctorOwnership(principal, appointment);
        if (!prescriptions.findByAppointmentId(request.appointmentId()).isEmpty()) {
            throw new ResourceConflictException("Prescription already exists for appointment");
        }
        Prescription prescription = new Prescription(request.patientName(), request.appointmentId(),
                request.medication(), request.dosage(), request.doctorNotes());
        Prescription saved = prescriptions.save(prescription);
        appointments.updateStatus(1, request.appointmentId());
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PrescriptionResponse getPrescription(AuthenticatedUser principal, Long appointmentId) {
        requireRole(principal, Role.DOCTOR, Role.PATIENT);
        Appointment appointment = appointment(appointmentId);
        if (principal.role() == Role.DOCTOR) {
            verifyDoctorOwnership(principal, appointment);
        } else if (appointment.getPatient() == null || !principal.accountId().equals(appointment.getPatient().getId())) {
            throw new ForbiddenOperationException("Patient does not own appointment");
        }
        Prescription prescription = prescriptions.findByAppointmentId(appointmentId).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));
        return mapper.toResponse(prescription);
    }

    private Appointment appointment(Long appointmentId) {
        if (appointments == null) throw new IllegalStateException("Appointment repository is not configured");
        return appointments.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
    }

    private void verifyDoctorOwnership(AuthenticatedUser principal, Appointment appointment) {
        if (appointment.getDoctor() == null || !principal.accountId().equals(appointment.getDoctor().getId())) {
            throw new ForbiddenOperationException("Doctor is not assigned to appointment");
        }
    }

    private void requireRole(AuthenticatedUser principal, Role... allowed) {
        if (principal == null || java.util.Arrays.stream(allowed).noneMatch(role -> role == principal.role())) {
            throw new ForbiddenOperationException("Role cannot perform this prescription operation");
        }
    }

    // Course-era APIs retained until assignment evidence is migrated.
    @Transactional
    public ResponseEntity<Map<String, String>> savePrescription(Prescription prescription) {
        try {
            if (!prescriptions.findByAppointmentId(prescription.getAppointmentId()).isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Prescription already exists"));
            }
            prescriptions.save(prescription);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Prescription saved"));
        } catch (RuntimeException exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Unable to save prescription"));
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPrescription(Long appointmentId) {
        try {
            List<Prescription> found = prescriptions.findByAppointmentId(appointmentId);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("prescription", found);
            return ResponseEntity.ok(body);
        } catch (RuntimeException exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Unable to retrieve prescription"));
        }
    }
}
