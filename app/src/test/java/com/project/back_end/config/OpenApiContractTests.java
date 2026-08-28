package com.project.back_end.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import com.project.back_end.repo.PrescriptionRepository;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration",
        "spring.profiles.active=default",
        "app.demo-data.enabled=false"
})
@AutoConfigureMockMvc
class OpenApiContractTests {

    @Autowired
    private MockMvc mvc;

    @MockitoBean private AdminRepository admins;
    @MockitoBean private AppointmentRepository appointments;
    @MockitoBean private DoctorRepository doctors;
    @MockitoBean private PatientRepository patients;
    @MockitoBean private PrescriptionRepository prescriptions;

    @Test
    void publishesApprovedPathsBearerSchemeAndSafeResponseSchemas() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/admin/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/auth/doctors/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/auth/patients/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/patients'].post").exists())
                .andExpect(jsonPath("$.paths['/api/patients/me'].get").exists())
                .andExpect(jsonPath("$.paths['/api/doctors'].get").exists())
                .andExpect(jsonPath("$.paths['/api/appointments'].get").exists())
                .andExpect(jsonPath("$.paths['/api/appointments'].post").exists())
                .andExpect(jsonPath("$.paths['/api/prescriptions/{appointmentId}'].get").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.paths['/api/appointments'].get.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/patients/me'].get.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/auth/admin/login'].post.security").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.DoctorResponse.properties.password").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.PatientResponse.properties.password").doesNotExist());
    }
}
