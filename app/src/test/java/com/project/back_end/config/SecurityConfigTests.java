package com.project.back_end.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;

import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import com.project.back_end.repo.PrescriptionRepository;
import com.project.back_end.security.AuthenticatedUser;
import com.project.back_end.security.Role;
import com.project.back_end.services.TokenService;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration",
        "app.demo-data.enabled=false"
})
@AutoConfigureMockMvc
@Import(SecurityConfigTests.SecurityProbeController.class)
class SecurityConfigTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TokenService tokens;

    @MockitoBean
    private AdminRepository adminRepository;

    @MockitoBean
    private AppointmentRepository appointmentRepository;

    @MockitoBean
    private DoctorRepository doctorRepository;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private PrescriptionRepository prescriptionRepository;

    @Test
    void landingAndHealthArePublic() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk());
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void apiWithoutBearerTokenReturnsProblemDetail401() throws Exception {
        mvc.perform(get("/api/security-probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void bearerPrincipalCarriesRoleAndWrongRoleIsForbidden() throws Exception {
        String doctorToken = tokens.generateToken(7L, "doctor@example.com", Role.DOCTOR);

        mvc.perform(get("/api/security-probe").header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(content().string("doctor@example.com"));
        mvc.perform(get("/api/security-probe/admin").header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void doctorCannotUseAdminDoctorManagementRoute() throws Exception {
        String doctorToken = tokens.generateToken(7L, "doctor@example.com", Role.DOCTOR);

        mvc.perform(post("/api/doctors")
                .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @RestController
    static class SecurityProbeController {
        @GetMapping("/api/security-probe")
        String authenticated(@AuthenticationPrincipal AuthenticatedUser user) {
            return user.subject();
        }

        @GetMapping("/api/security-probe/admin")
        @PreAuthorize("hasRole('ADMIN')")
        String adminOnly() {
            return "admin";
        }
    }
}
