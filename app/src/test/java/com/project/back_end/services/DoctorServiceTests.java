package com.project.back_end.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.project.back_end.DTO.doctor.DoctorCreateRequest;
import com.project.back_end.DTO.doctor.DoctorResponse;
import com.project.back_end.DTO.doctor.DoctorUpdateRequest;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTests {

    @Mock
    private DoctorRepository doctors;

    @Mock
    private AppointmentRepository appointments;

    private DoctorService service;

    @BeforeEach
    void setUp() {
        service = new DoctorService(doctors, appointments, new TokenService(
                new com.project.back_end.config.properties.JwtProperties(
                        "test-signing-key-that-is-at-least-32-bytes-long", java.time.Duration.ofHours(1))),
                new BCryptPasswordEncoder());
    }

    @Test
    void createDoctorHashesPasswordAndReturnsResponseWithoutPassword() {
        when(doctors.findByEmail("doctor@example.com")).thenReturn(Optional.empty());
        Doctor saved = doctor(7L, "Dr. One", "Cardiology", "doctor@example.com");
        when(doctors.save(org.mockito.ArgumentMatchers.any(Doctor.class))).thenReturn(saved);

        DoctorResponse response = service.createDoctor(new DoctorCreateRequest(
                "Dr. One", "Cardiology", "doctor@example.com", "secret", "0812345678", List.of("09:00-10:00")));

        assertEquals(7L, response.id());
        ArgumentCaptor<Doctor> captor = ArgumentCaptor.forClass(Doctor.class);
        verify(doctors).save(captor.capture());
        assertEquals(false, "secret".equals(captor.getValue().getPassword()));
        assertEquals(true, new BCryptPasswordEncoder().matches("secret", captor.getValue().getPassword()));
    }

    @Test
    void duplicateEmailIsConflict() {
        when(doctors.findByEmail("doctor@example.com")).thenReturn(Optional.of(doctor(7L, "Dr. One", "ER", "doctor@example.com")));

        assertThrows(com.project.back_end.exceptions.ResourceConflictException.class,
                () -> service.createDoctor(new DoctorCreateRequest(
                        "Dr. Two", "ER", "doctor@example.com", "secret", "0812345679", List.of())));
    }

    @Test
    void listFiltersNameSpecialtyPeriodAndPaginates() {
        when(doctors.findAll(Sort.by(Sort.Direction.ASC, "name"))).thenReturn(List.of(
                doctorWithTimes(1L, "Alice", "Cardiology", List.of("09:00-10:00")),
                doctorWithTimes(2L, "Bob", "Cardiology", List.of("14:00-15:00")),
                doctorWithTimes(3L, "Alice West", "Cardiology", List.of("09:00-10:00"))));

        var page = service.listDoctors("alice", "cardiology", "AM", PageRequest.of(0, 1));

        assertEquals(1, page.content().size());
        assertEquals("Alice", page.content().get(0).name());
        assertEquals(2, page.totalElements());
        assertEquals(2, page.totalPages());
    }

    @Test
    void updateMissingDoctorIsNotFoundAndDeleteRemovesExistingDoctor() {
        when(doctors.findById(99L)).thenReturn(Optional.empty());
        assertThrows(com.project.back_end.exceptions.ResourceNotFoundException.class,
                () -> service.updateDoctor(99L, new DoctorUpdateRequest(
                        "Dr. X", "ER", "x@example.com", "secret", "0812345680", List.of())));

        when(doctors.existsById(7L)).thenReturn(true);
        service.deleteDoctor(7L);
        verify(appointments).deleteAllByDoctorId(7L);
        verify(doctors).deleteById(7L);
    }

    private static Doctor doctor(Long id, String name, String specialty, String email) {
        Doctor doctor = doctorWithTimes(id, name, specialty, List.of());
        doctor.setEmail(email);
        return doctor;
    }

    private static Doctor doctorWithTimes(Long id, String name, String specialty, List<String> times) {
        Doctor doctor = new Doctor();
        doctor.setId(id);
        doctor.setName(name);
        doctor.setSpecialty(specialty);
        doctor.setEmail(name.toLowerCase().replace(' ', '.') + "@example.com");
        doctor.setPhone("0812345678");
        doctor.setPassword("$2a$10$stored");
        doctor.setAvailableTimes(times);
        return doctor;
    }
}
