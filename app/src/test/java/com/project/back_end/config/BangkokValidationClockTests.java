package com.project.back_end.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import com.project.back_end.repo.PrescriptionRepository;

import jakarta.validation.Validator;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "app.demo-data.enabled=false"
})
@AutoConfigureMockMvc
@Import(BangkokValidationClockTests.FixedBangkokClockConfig.class)
class BangkokValidationClockTests {

    private static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");

    @Autowired
    private Validator validator;

    @MockitoBean private AdminRepository admins;
    @MockitoBean private AppointmentRepository appointments;
    @MockitoBean private DoctorRepository doctors;
    @MockitoBean private PatientRepository patients;
    @MockitoBean private PrescriptionRepository prescriptions;

    @Test
    void futureValidationUsesConfiguredBangkokClockInsteadOfJvmDefault() {
        assertThat(validator.validate(appointmentAt(LocalDateTime.of(2030, 1, 10, 8, 0))))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("appointmentTime");
        assertThat(validator.validate(appointmentAt(LocalDateTime.of(2030, 1, 10, 9, 0))))
                .isEmpty();
    }

    private Appointment appointmentAt(LocalDateTime time) {
        Appointment appointment = new Appointment();
        appointment.setDoctor(new Doctor());
        appointment.setPatient(new Patient());
        appointment.setAppointmentTime(time);
        return appointment;
    }

    @TestConfiguration
    static class FixedBangkokClockConfig {
        @Bean
        @Primary
        Clock fixedBangkokClock() {
            return Clock.fixed(Instant.parse("2030-01-10T01:00:00Z"), BANGKOK);
        }
    }
}
