package com.project.back_end.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import com.project.back_end.config.ClinicTimeConfig;
import com.project.back_end.config.properties.JwtProperties;
import com.project.back_end.models.Admin;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import com.project.back_end.repo.PrescriptionRepository;
import com.project.back_end.security.Role;
import com.project.back_end.security.RestAccessDeniedHandler;
import com.project.back_end.security.RestAuthenticationEntryPoint;
import com.project.back_end.services.AuthService;
import com.project.back_end.services.TokenService;

@WebMvcTest(controllers = AuthController.class, properties = "app.demo-data.enabled=false")
@EnableConfigurationProperties(JwtProperties.class)
@Import({ SecurityConfig.class, ClinicTimeConfig.class, AuthService.class, TokenService.class,
        com.project.back_end.services.AuthRateLimiter.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class })
class AuthControllerTests {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AdminRepository admins;

    @MockitoBean
    private DoctorRepository doctors;

    @MockitoBean
    private PatientRepository patients;

    @MockitoBean
    private AppointmentRepository appointments;

    @MockitoBean
    private PrescriptionRepository prescriptions;

    @Test
    void adminLoginReturnsTypedResponseWithoutPassword() throws Exception {
        Admin admin = new Admin();
        admin.setId(11L);
        admin.setUsername("admin");
        admin.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("secret"));
        when(admins.findByUsername("admin")).thenReturn(java.util.Optional.of(admin));

        mvc.perform(post("/api/auth/admin/login")
                .contentType("application/json")
                .content("{\"username\":\"admin\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void doctorLoginReturnsUnauthorizedForInvalidCredentials() throws Exception {
        when(doctors.findByEmail("doctor@example.com")).thenReturn(java.util.Optional.empty());

        mvc.perform(post("/api/auth/doctors/login")
                .contentType("application/json")
                .content("{\"email\":\"doctor@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void patientLoginValidatesRequestAndReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/auth/patients/login")
                .contentType("application/json")
                .content("{\"email\":\"not-an-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void patientRegistrationReturnsConflictForDuplicateContact() throws Exception {
        when(patients.existsByEmail("patient@example.com")).thenReturn(true);

        mvc.perform(post("/api/patients")
                .contentType("application/json")
                .content("{\"name\":\"Patient Name\",\"email\":\"patient@example.com\","
                        + "\"password\":\"secret\",\"phone\":\"0812345678\",\"address\":\"Bangkok\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void loginLimitReturnsGenericProblemRetryAfterAndDoesNotLeakAccountState() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            mvc.perform(post("/api/auth/patients/login")
                    .contentType("application/json")
                    .content("{\"email\":\"limited@example.com\",\"password\":\"wrong\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/patients/login")
                .contentType("application/json")
                .content("{\"email\":\"limited@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().exists("Retry-After"))
                .andExpect(jsonPath("$.detail").value("Too many requests"))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

}
