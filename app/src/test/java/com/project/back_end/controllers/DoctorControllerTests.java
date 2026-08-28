package com.project.back_end.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.project.back_end.config.SecurityConfig;
import com.project.back_end.config.properties.JwtProperties;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import com.project.back_end.repo.PrescriptionRepository;
import com.project.back_end.security.Role;
import com.project.back_end.services.DoctorService;
import com.project.back_end.services.TokenService;

@WebMvcTest(controllers = DoctorController.class, properties = "app.demo-data.enabled=false")
@Import({ SecurityConfig.class, DoctorService.class, DoctorControllerTests.TestTokenConfiguration.class })
class DoctorControllerTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TokenService tokens;

    @MockitoBean
    private AdminRepository admins;

    @MockitoBean
    private AppointmentRepository appointments;

    @MockitoBean
    private DoctorRepository doctors;

    @MockitoBean
    private PatientRepository patients;

    @MockitoBean
    private PrescriptionRepository prescriptions;

    @Test
    void doctorDirectoryIsPublicAndOmitsPassword() throws Exception {
        Doctor doctor = doctor(7L);
        when(doctors.findAll(Sort.by(Sort.Direction.ASC, "name"))).thenReturn(List.of(doctor));

        mvc.perform(get("/api/doctors").param("name", "").param("specialty", "")
                .param("period", "").param("page", "0").param("size", "20").param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Dr. One"))
                .andExpect(jsonPath("$.content[0].password").doesNotExist());
    }

    @Test
    void doctorManagementRequiresAdminRole() throws Exception {
        String token = tokens.generateToken(8L, "doctor@example.com", Role.DOCTOR);

        mvc.perform(post("/api/doctors")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"name\":\"Dr. One\",\"specialty\":\"ER\",\"email\":\"doctor@example.com\","
                        + "\"password\":\"secret\",\"phone\":\"0812345678\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void missingDoctorDeleteReturnsProblemDetail404() throws Exception {
        when(doctors.existsById(99L)).thenReturn(false);
        String token = tokens.generateToken(1L, "admin", Role.ADMIN);

        mvc.perform(delete("/api/doctors/99").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @TestConfiguration
    static class TestTokenConfiguration {
        @Bean
        @Primary
        TokenService doctorTokenService() {
            return new TokenService(new JwtProperties(
                    "test-signing-key-that-is-at-least-32-bytes-long", Duration.ofHours(1)), Clock.systemUTC());
        }
    }

    private static Doctor doctor(Long id) {
        Doctor doctor = new Doctor();
        doctor.setId(id);
        doctor.setName("Dr. One");
        doctor.setSpecialty("Cardiology");
        doctor.setEmail("doctor@example.com");
        doctor.setPassword("secret");
        doctor.setPhone("0812345678");
        doctor.setAvailableTimes(List.of("09:00-10:00"));
        return doctor;
    }
}
