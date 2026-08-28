package com.project.back_end.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.project.back_end.config.SecurityConfig;
import com.project.back_end.config.properties.JwtProperties;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import com.project.back_end.repo.PrescriptionRepository;
import com.project.back_end.security.Role;
import com.project.back_end.services.PatientService;
import com.project.back_end.services.TokenService;
import com.project.back_end.models.Patient;

@WebMvcTest(controllers = PatientController.class, properties = "app.demo-data.enabled=false")
@EnableConfigurationProperties(JwtProperties.class)
@Import({SecurityConfig.class, PatientService.class, TokenService.class})
class PatientControllerTests {

    @Autowired private MockMvc mvc;
    @Autowired private TokenService tokens;

    @MockitoBean private AdminRepository admins;
    @MockitoBean private AppointmentRepository appointments;
    @MockitoBean private DoctorRepository doctors;
    @MockitoBean private PatientRepository patients;
    @MockitoBean private PrescriptionRepository prescriptions;

    @Test
    void patientCanReadOwnProfile() throws Exception {
        Patient patient = new Patient();
        patient.setId(10L);
        patient.setName("Patient One");
        patient.setEmail("patient@example.com");
        patient.setPassword("$2a$10$stored");
        patient.setPhone("0812345678");
        patient.setAddress("Bangkok");
        when(patients.findById(10L)).thenReturn(java.util.Optional.of(patient));
        String token = tokens.generateToken(10L, "patient@example.com", Role.PATIENT);

        mvc.perform(get("/api/patients/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("patient@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void doctorCannotReadPatientProfile() throws Exception {
        String token = tokens.generateToken(7L, "doctor@example.com", Role.DOCTOR);
        mvc.perform(get("/api/patients/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

}
