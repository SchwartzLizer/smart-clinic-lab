package com.project.back_end.mappers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.back_end.DTO.appointment.AppointmentResponse;
import com.project.back_end.DTO.doctor.DoctorResponse;
import com.project.back_end.DTO.patient.PatientResponse;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;

class DtoMapperTests {

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    @Test
    void doctorAndPatientResponsesNeverExposePasswords() throws Exception {
        Doctor doctor = new Doctor();
        doctor.setId(7L);
        doctor.setName("Dr. One");
        doctor.setSpecialty("Cardiology");
        doctor.setEmail("doctor@example.com");
        doctor.setPassword("secret");
        doctor.setPhone("0812345678");
        doctor.setAvailableTimes(List.of("09:00-10:00"));

        Patient patient = new Patient();
        patient.setId(8L);
        patient.setName("Patient One");
        patient.setEmail("patient@example.com");
        patient.setPassword("secret");
        patient.setPhone("0898765432");
        patient.setAddress("Bangkok");

        String doctorJson = json.writeValueAsString(new DoctorMapper().toResponse(doctor));
        String patientJson = json.writeValueAsString(new PatientMapper().toResponse(patient));

        assertFalse(doctorJson.contains("password"));
        assertFalse(patientJson.contains("password"));
        assertTrue(doctorJson.contains("doctor@example.com"));
        assertTrue(patientJson.contains("patient@example.com"));
    }

    @Test
    void appointmentResponseUsesNestedSummariesWithoutPersistenceBackReferences() throws Exception {
        Doctor doctor = new Doctor();
        doctor.setId(7L);
        doctor.setName("Dr. One");
        doctor.setSpecialty("Cardiology");
        doctor.setEmail("doctor@example.com");
        doctor.setPassword("secret");
        doctor.setPhone("0812345678");

        Patient patient = new Patient();
        patient.setId(8L);
        patient.setName("Patient One");
        patient.setEmail("patient@example.com");
        patient.setPassword("secret");
        patient.setPhone("0898765432");
        patient.setAddress("Bangkok");

        Appointment appointment = new Appointment();
        appointment.setId(9L);
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setAppointmentTime(LocalDateTime.of(2030, 1, 1, 9, 0));
        appointment.setStatus(0);

        AppointmentResponse response = new AppointmentMapper().toResponse(appointment);
        String body = json.writeValueAsString(response);

        assertEquals(7L, response.doctor().id());
        assertEquals("Dr. One", response.doctor().name());
        assertEquals(8L, response.patient().id());
        assertFalse(body.contains("password"));
        assertFalse(body.contains("availableTimes"));
    }
}
