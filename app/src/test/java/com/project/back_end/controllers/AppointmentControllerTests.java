package com.project.back_end.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.project.back_end.DTO.appointment.AppointmentResponse;
import com.project.back_end.config.SecurityConfig;
import com.project.back_end.config.ClinicTimeConfig;
import com.project.back_end.config.properties.JwtProperties;
import com.project.back_end.mappers.AppointmentMapper;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import com.project.back_end.repo.PrescriptionRepository;
import com.project.back_end.security.Role;
import com.project.back_end.security.RestAccessDeniedHandler;
import com.project.back_end.security.RestAuthenticationEntryPoint;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.TokenService;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;

@WebMvcTest(controllers = AppointmentController.class, properties = "app.demo-data.enabled=false")
@EnableConfigurationProperties(JwtProperties.class)
@Import({SecurityConfig.class, ClinicTimeConfig.class, AppointmentService.class, TokenService.class, AppointmentMapper.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AppointmentControllerTests {

    @Autowired private MockMvc mvc;
    @Autowired private TokenService tokens;

    @MockitoBean private AdminRepository admins;
    @MockitoBean private AppointmentRepository appointmentRepository;
    @MockitoBean private DoctorRepository doctors;
    @MockitoBean private PatientRepository patients;
    @MockitoBean private PrescriptionRepository prescriptions;

    @Test
    void patientCanCreateAppointmentAndReceivesCreatedResponse() throws Exception {
        Patient patient = patient();
        Doctor doctor = doctor();
        when(patients.findById(10L)).thenReturn(java.util.Optional.of(patient));
        when(doctors.findById(7L)).thenReturn(java.util.Optional.of(doctor));
        when(appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(appointmentRepository.save(org.mockito.ArgumentMatchers.any(Appointment.class))).thenAnswer(invocation -> {
            Appointment appointment = invocation.getArgument(0);
            appointment.setId(50L);
            return appointment;
        });
        String token = tokens.generateToken(10L, "patient@example.com", Role.PATIENT);

        mvc.perform(post("/api/appointments")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"doctorId\":7,\"appointmentTime\":\"2030-01-10T09:00:00\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$", org.hamcrest.Matchers.aMapWithSize(5)))
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.doctor.id").value(7))
                .andExpect(jsonPath("$.doctor.name").value("Dr. One"))
                .andExpect(jsonPath("$.doctor.specialty").value("Cardiology"))
                .andExpect(jsonPath("$.patient.id").value(10))
                .andExpect(jsonPath("$.patient.name").value("Patient One"))
                .andExpect(jsonPath("$.appointmentTime").value("2030-01-10T09:00:00"))
                .andExpect(jsonPath("$.status").value(0))
                .andExpect(jsonPath("$.appointmentDate").doesNotExist())
                .andExpect(jsonPath("$.appointmentTimeOnly").doesNotExist());
    }

    @Test
    void unauthenticatedAppointmentReadIsUnauthorizedProblemDetail() throws Exception {
        mvc.perform(get("/api/appointments"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void doctorCannotCreatePatientAppointment() throws Exception {
        String token = tokens.generateToken(7L, "doctor@example.com", Role.DOCTOR);

        mvc.perform(post("/api/appointments")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"doctorId\":7,\"appointmentTime\":\"2030-01-10T09:00:00\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void patientCanUpdateAndDeleteOwnedAppointment() throws Exception {
        Appointment appointment = appointment();
        when(appointmentRepository.findById(50L)).thenReturn(java.util.Optional.of(appointment));
        when(appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(appointmentRepository.save(org.mockito.ArgumentMatchers.any(Appointment.class))).thenReturn(appointment);
        String token = tokens.generateToken(10L, "patient@example.com", Role.PATIENT);

        mvc.perform(put("/api/appointments/50")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"appointmentTime\":\"2030-01-10T10:00:00\"}"))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/appointments/50")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    private static AppointmentResponse response(Long id) {
        return new AppointmentResponse(id,
                new AppointmentResponse.DoctorSummary(7L, "Dr. One", "Cardiology"),
                new AppointmentResponse.PatientSummary(10L, "Patient One"),
                java.time.LocalDateTime.of(2030, 1, 10, 9, 0), 0);
    }

    private static Patient patient() {
        Patient patient = new Patient();
        patient.setId(10L);
        patient.setName("Patient One");
        patient.setEmail("patient@example.com");
        return patient;
    }

    private static Doctor doctor() {
        Doctor doctor = new Doctor();
        doctor.setId(7L);
        doctor.setName("Dr. One");
        doctor.setSpecialty("Cardiology");
        doctor.setAvailableTimes(List.of("09:00-10:00", "10:00-11:00"));
        return doctor;
    }

    private static Appointment appointment() {
        Appointment appointment = new Appointment();
        appointment.setId(50L);
        appointment.setDoctor(doctor());
        appointment.setPatient(patient());
        appointment.setAppointmentTime(java.time.LocalDateTime.of(2030, 1, 10, 9, 0));
        return appointment;
    }
}
