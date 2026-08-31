package com.project.back_end.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.back_end.DTO.appointment.AppointmentCreateRequest;
import com.project.back_end.DTO.appointment.AppointmentResponse;
import com.project.back_end.DTO.appointment.AppointmentUpdateRequest;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import com.project.back_end.security.AuthenticatedUser;
import com.project.back_end.security.Role;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTests {

    private static final LocalDateTime NOW = LocalDateTime.of(2030, 1, 10, 8, 0);

    @Mock private AppointmentRepository appointments;
    @Mock private DoctorRepository doctors;
    @Mock private PatientRepository patients;

    private AppointmentService service;
    private Patient patient;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(appointments, new TokenService(
                new com.project.back_end.config.properties.JwtProperties(
                        "test-signing-key-that-is-at-least-32-bytes-long", java.time.Duration.ofHours(1))),
                doctors, patients, Clock.fixed(Instant.parse("2030-01-10T01:00:00Z"), ZoneOffset.UTC));
        patient = new Patient();
        patient.setId(10L);
        patient.setName("Patient One");
        patient.setEmail("patient@example.com");
        doctor = new Doctor();
        doctor.setId(7L);
        doctor.setName("Dr. One");
        doctor.setSpecialty("Cardiology");
        doctor.setAvailableTimes(List.of("09:00-10:00"));
    }

    @Test
    void patientCanBookAvailableFutureSlot() {
        when(patients.findById(10L)).thenReturn(Optional.of(patient));
        when(doctors.findById(7L)).thenReturn(Optional.of(doctor));
        when(appointments.findByDoctorIdAndAppointmentTimeBetween(any(), any(), any())).thenReturn(List.of());
        when(appointments.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment saved = invocation.getArgument(0);
            saved.setId(50L);
            return saved;
        });

        AppointmentResponse response = service.createAppointment(
                new AuthenticatedUser(10L, patient.getEmail(), Role.PATIENT),
                new AppointmentCreateRequest(7L, NOW.plusHours(1)));

        assertEquals(50L, response.id());
        assertEquals(10L, response.patient().id());
        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointments).save(captor.capture());
        assertEquals(0, captor.getValue().getStatus());
    }

    @Test
    void bookingRejectsAlreadyBookedSlot() {
        when(patients.findById(10L)).thenReturn(Optional.of(patient));
        when(doctors.findById(7L)).thenReturn(Optional.of(doctor));
        Appointment booked = appointment(20L, NOW.plusHours(1));
        when(appointments.findByDoctorIdAndAppointmentTimeBetween(any(), any(), any())).thenReturn(List.of(booked));

        assertThrows(com.project.back_end.exceptions.ResourceConflictException.class, () -> service.createAppointment(
                new AuthenticatedUser(10L, patient.getEmail(), Role.PATIENT),
                new AppointmentCreateRequest(7L, NOW.plusHours(1))));
    }

    @Test
    void patientCannotReadOrUpdateAnotherPatientsAppointment() {
        Patient other = new Patient();
        other.setId(99L);
        Appointment existing = appointment(30L, NOW.plusHours(1));
        existing.setPatient(other);
        when(appointments.findById(30L)).thenReturn(Optional.of(existing));

        AuthenticatedUser owner = new AuthenticatedUser(10L, patient.getEmail(), Role.PATIENT);
        assertThrows(com.project.back_end.exceptions.ForbiddenOperationException.class,
                () -> service.updateAppointment(owner, 30L, new AppointmentUpdateRequest(NOW.plusHours(2))));
        assertThrows(com.project.back_end.exceptions.ForbiddenOperationException.class,
                () -> service.deleteAppointment(owner, 30L));
    }

    @Test
    void doctorListIsLimitedToAssignedDoctor() {
        Appointment assigned = appointment(40L, NOW.plusHours(1));
        when(appointments.findByDoctorIdOrderByAppointmentTimeAsc(7L)).thenReturn(List.of(assigned));

        var result = service.listAppointments(new AuthenticatedUser(7L, "doctor@example.com", Role.DOCTOR),
                null, null, null);

        assertEquals(1, result.size());
        assertEquals(40L, result.get(0).id());
    }

    private Appointment appointment(Long id, LocalDateTime time) {
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setAppointmentTime(time);
        return appointment;
    }
}
