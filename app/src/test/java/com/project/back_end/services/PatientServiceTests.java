package com.project.back_end.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.PatientRepository;

@ExtendWith(MockitoExtension.class)
class PatientServiceTests {

    @Mock private PatientRepository patients;
    @Mock private AppointmentRepository appointments;

    @Test
    void profileUsesAuthenticatedAccountIdAndOmitsPassword() {
        Patient patient = patient(10L);
        when(patients.findById(10L)).thenReturn(Optional.of(patient));

        var response = new PatientService(patients, appointments, null).getMyProfile(10L);

        assertEquals(10L, response.id());
        assertEquals("patient@example.com", response.email());
    }

    @Test
    void missingProfileIsNotFound() {
        when(patients.findById(10L)).thenReturn(Optional.empty());
        assertThrows(com.project.back_end.exceptions.ResourceNotFoundException.class,
                () -> new PatientService(patients, appointments, null).getMyProfile(10L));
    }

    private static Patient patient(Long id) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.setName("Patient One");
        patient.setEmail("patient@example.com");
        patient.setPassword("$2a$10$stored");
        patient.setPhone("0812345678");
        patient.setAddress("Bangkok");
        return patient;
    }
}
