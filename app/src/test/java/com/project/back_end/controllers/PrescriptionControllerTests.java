package com.project.back_end.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.project.back_end.config.SecurityConfig;
import com.project.back_end.config.properties.JwtProperties;
import com.project.back_end.mappers.PrescriptionMapper;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import com.project.back_end.repo.PrescriptionRepository;
import com.project.back_end.security.Role;
import com.project.back_end.security.RestAccessDeniedHandler;
import com.project.back_end.security.RestAuthenticationEntryPoint;
import com.project.back_end.services.PrescriptionService;
import com.project.back_end.services.TokenService;

@WebMvcTest(controllers = PrescriptionController.class, properties = "app.demo-data.enabled=false")
@EnableConfigurationProperties(JwtProperties.class)
@Import({SecurityConfig.class, PrescriptionService.class, TokenService.class, PrescriptionMapper.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class PrescriptionControllerTests {

    @Autowired private MockMvc mvc;
    @Autowired private TokenService tokens;

    @MockitoBean private AdminRepository admins;
    @MockitoBean private AppointmentRepository appointments;
    @MockitoBean private DoctorRepository doctors;
    @MockitoBean private PatientRepository patients;
    @MockitoBean private PrescriptionRepository prescriptions;

    @Test
    void doctorCanCreatePrescription() throws Exception {
        when(appointments.findById(50L)).thenReturn(java.util.Optional.of(appointment()));
        when(prescriptions.findByAppointmentId(50L)).thenReturn(List.of());
        when(prescriptions.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
        String token = tokens.generateToken(7L, "doctor@example.com", Role.DOCTOR);

        mvc.perform(post("/api/prescriptions")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"patientName\":\"Patient One\",\"appointmentId\":50,"+
                        "\"medication\":\"Amoxicillin\",\"dosage\":\"500mg\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith("application/json"));
    }

    @Test
    void patientCannotCreatePrescription() throws Exception {
        String token = tokens.generateToken(10L, "patient@example.com", Role.PATIENT);
        mvc.perform(post("/api/prescriptions")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"patientName\":\"Patient One\",\"appointmentId\":50,"+
                        "\"medication\":\"Amoxicillin\",\"dosage\":\"500mg\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedReadIsUnauthorized() throws Exception {
        mvc.perform(get("/api/prescriptions/50"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    private static Appointment appointment() {
        Doctor doctor = new Doctor();
        doctor.setId(7L);
        doctor.setName("Dr. One");
        Patient patient = new Patient();
        patient.setId(10L);
        patient.setName("Patient One");
        Appointment appointment = new Appointment();
        appointment.setId(50L);
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        return appointment;
    }
}
