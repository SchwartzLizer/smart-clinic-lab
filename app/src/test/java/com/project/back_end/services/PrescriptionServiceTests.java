package com.project.back_end.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.back_end.DTO.prescription.PrescriptionCreateRequest;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.models.Prescription;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.PrescriptionRepository;
import com.project.back_end.security.AuthenticatedUser;
import com.project.back_end.security.Role;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTests {

    @Mock private PrescriptionRepository prescriptions;
    @Mock private AppointmentRepository appointments;

    private PrescriptionService service;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        service = new PrescriptionService(prescriptions, appointments);
        Doctor doctor = new Doctor();
        doctor.setId(7L);
        doctor.setName("Dr. One");
        Patient patient = new Patient();
        patient.setId(10L);
        patient.setName("Patient One");
        appointment = new Appointment();
        appointment.setId(50L);
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setAppointmentTime(LocalDateTime.of(2030, 1, 10, 9, 0));
        org.mockito.Mockito.lenient().when(appointments.updateStatus(1, 50L)).thenReturn(1);
    }

    @Test
    void assignedDoctorCanCreatePrescriptionAndCompleteAppointment() {
        when(appointments.findById(50L)).thenReturn(Optional.of(appointment));
        when(prescriptions.findByAppointmentId(50L)).thenReturn(List.of());
        when(prescriptions.save(any(Prescription.class))).thenAnswer(invocation -> {
            Prescription saved = invocation.getArgument(0);
            saved.setId("rx-1");
            return saved;
        });

        var response = service.createPrescription(new AuthenticatedUser(7L, "doctor@example.com", Role.DOCTOR),
                new PrescriptionCreateRequest("Patient One", 50L, "Amoxicillin", "500mg", "Take twice daily"));

        assertEquals("rx-1", response.response().id());
        assertEquals(org.springframework.http.HttpStatus.CREATED, response.status());
        verify(appointments).updateStatus(1, 50L);
    }

    @Test
    void identicalPrescriptionRetryRepairsAppointmentAndReturnsOk() {
        when(appointments.findById(50L)).thenReturn(Optional.of(appointment));
        Prescription existing = new Prescription("Patient One", 50L, "Amoxicillin", "500mg", "");
        existing.setId("rx-1");
        when(prescriptions.findByAppointmentId(50L)).thenReturn(List.of(existing));

        var response = service.createPrescription(new AuthenticatedUser(7L, "doctor@example.com", Role.DOCTOR),
                new PrescriptionCreateRequest("Patient One", 50L, "Amoxicillin", "500mg", null));

        assertEquals(org.springframework.http.HttpStatus.OK, response.status());
        assertEquals("rx-1", response.response().id());
        verify(appointments).updateStatus(1, 50L);
        verify(prescriptions, never()).save(any(Prescription.class));
    }

    @Test
    void differentPrescriptionRetryIsConflictWithoutCompletingAppointment() {
        when(appointments.findById(50L)).thenReturn(Optional.of(appointment));
        when(prescriptions.findByAppointmentId(50L)).thenReturn(List.of(
                new Prescription("Patient One", 50L, "Amoxicillin", "500mg", null)));

        assertThrows(com.project.back_end.exceptions.ResourceConflictException.class,
                () -> service.createPrescription(new AuthenticatedUser(7L, "doctor@example.com", Role.DOCTOR),
                        new PrescriptionCreateRequest("Patient One", 50L, "Other medication", "500mg", null)));
        verify(appointments, never()).updateStatus(1, 50L);
    }

    @Test
    void duplicateKeyRaceReturnsExistingIdenticalPrescription() {
        when(appointments.findById(50L)).thenReturn(Optional.of(appointment));
        Prescription existing = new Prescription("Patient One", 50L, "Amoxicillin", "500mg", null);
        existing.setId("rx-1");
        when(prescriptions.findByAppointmentId(50L)).thenReturn(List.of(), List.of(existing));
        when(prescriptions.save(any(Prescription.class)))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("duplicate"));

        var response = service.createPrescription(new AuthenticatedUser(7L, "doctor@example.com", Role.DOCTOR),
                new PrescriptionCreateRequest("Patient One", 50L, "Amoxicillin", "500mg", ""));

        assertEquals(org.springframework.http.HttpStatus.OK, response.status());
        assertEquals("rx-1", response.response().id());
        verify(appointments).updateStatus(1, 50L);
    }

    @Test
    void zeroRowCompletionUpdateDoesNotReportPrescriptionSuccess() {
        when(appointments.findById(50L)).thenReturn(Optional.of(appointment));
        when(prescriptions.findByAppointmentId(50L)).thenReturn(List.of());
        when(prescriptions.save(any(Prescription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(appointments.updateStatus(1, 50L)).thenReturn(0);

        assertThrows(IllegalStateException.class,
                () -> service.createPrescription(new AuthenticatedUser(7L, "doctor@example.com", Role.DOCTOR),
                        new PrescriptionCreateRequest("Patient One", 50L, "Amoxicillin", "500mg", null)));
    }

    @Test
    void doctorCannotWriteAnotherDoctorsAppointment() {
        when(appointments.findById(50L)).thenReturn(Optional.of(appointment));
        assertThrows(com.project.back_end.exceptions.ForbiddenOperationException.class,
                () -> service.createPrescription(new AuthenticatedUser(8L, "other@example.com", Role.DOCTOR),
                        new PrescriptionCreateRequest("Patient One", 50L, "Amoxicillin", "500mg", null)));
        verify(prescriptions, never()).findByAppointmentId(50L);
    }

    @Test
    void patientCanReadOwnPrescriptionButNotAnotherPatients() {
        when(appointments.findById(50L)).thenReturn(Optional.of(appointment));
        Prescription prescription = new Prescription("Patient One", 50L, "Amoxicillin", "500mg", null);
        prescription.setId("rx-1");
        when(prescriptions.findByAppointmentId(50L)).thenReturn(List.of(prescription));

        assertEquals("rx-1", service.getPrescription(new AuthenticatedUser(10L, "patient@example.com", Role.PATIENT), 50L).id());

        assertThrows(com.project.back_end.exceptions.ForbiddenOperationException.class,
                () -> service.getPrescription(new AuthenticatedUser(99L, "other@example.com", Role.PATIENT), 50L));
    }
}
