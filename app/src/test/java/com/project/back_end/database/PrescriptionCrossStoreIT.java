package com.project.back_end.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.project.back_end.DTO.prescription.PrescriptionCreateRequest;
import com.project.back_end.exceptions.ForbiddenOperationException;
import com.project.back_end.exceptions.ResourceConflictException;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import com.project.back_end.repo.PrescriptionRepository;
import com.project.back_end.security.AuthenticatedUser;
import com.project.back_end.security.Role;
import com.project.back_end.services.PrescriptionCreationResult;
import com.project.back_end.services.PrescriptionService;

/**
 * Verifies the deliberate retry-driven MySQL/Mongo reconciliation boundary with
 * real disposable stores. Faults are injected only at the repository boundary.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "app.demo-data.enabled=false", "app.mongo.index.enabled=true" })
@ActiveProfiles("test")
class PrescriptionCrossStoreIT {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("smart_clinic")
            .withUsername("smart_clinic")
            .withPassword("smart-clinic-local-only");

    @Container
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired private PrescriptionService service;
    @Autowired private PrescriptionRepository prescriptions;
    @Autowired private AppointmentRepository appointments;
    @Autowired private DoctorRepository doctors;
    @Autowired private PatientRepository patients;
    @Autowired private MongoTemplate mongoTemplate;

    @BeforeEach
    void clearMongoDocuments() {
        mongoTemplate.getCollection("prescriptions").deleteMany(new org.bson.Document());
    }

    @Test
    void mongoFailureDoesNotCompleteAppointmentAndUnauthorizedRequestChangesNeitherStore() {
        Fixture fixture = fixture();
        PrescriptionRepository failingMongo = mock(PrescriptionRepository.class);
        when(failingMongo.findByAppointmentId(fixture.appointment().getId())).thenReturn(List.of());
        doThrow(new DataAccessResourceFailureException("test mongo outage"))
                .when(failingMongo).save(any());
        PrescriptionService faulted = new PrescriptionService(failingMongo, appointments);

        assertThatThrownBy(() -> faulted.createPrescription(fixture.doctor(), request(fixture, "Amoxicillin")))
                .isInstanceOf(DataAccessResourceFailureException.class);
        assertThat(status(fixture.appointment().getId())).isZero();
        assertThat(prescriptions.count()).isZero();

        assertThatThrownBy(() -> service.createPrescription(
                new AuthenticatedUser(fixture.doctor().accountId() + 999, "other", Role.DOCTOR),
                request(fixture, "Amoxicillin"))).isInstanceOf(ForbiddenOperationException.class);
        assertThat(status(fixture.appointment().getId())).isZero();
        assertThat(prescriptions.count()).isZero();
    }

    @Test
    void retryRepairsMysqlAfterMongoSucceededButCompletionFailedAndRejectsDifferentPayload() {
        Fixture fixture = fixture();
        AppointmentRepository failingStatus = mock(AppointmentRepository.class);
        when(failingStatus.findById(fixture.appointment().getId()))
                .thenAnswer(invocation -> appointments.findById(invocation.getArgument(0)));
        when(failingStatus.updateStatus(1, fixture.appointment().getId())).thenReturn(0);
        PrescriptionService faulted = new PrescriptionService(prescriptions, failingStatus);

        assertThatThrownBy(() -> faulted.createPrescription(fixture.doctor(), request(fixture, "Amoxicillin")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(prescriptions.count()).isEqualTo(1);
        assertThat(status(fixture.appointment().getId())).isZero();

        PrescriptionCreationResult repaired = service.createPrescription(fixture.doctor(), request(fixture, "Amoxicillin"));
        assertThat(repaired.status()).isEqualTo(HttpStatus.OK);
        assertThat(status(fixture.appointment().getId())).isEqualTo(1);

        assertThatThrownBy(() -> service.createPrescription(fixture.doctor(), request(fixture, "Azithromycin")))
                .isInstanceOf(ResourceConflictException.class);
        assertThat(prescriptions.count()).isEqualTo(1);
    }

    @Test
    void concurrentIdenticalRequestsCreateOneMongoDocumentWithCreatedAndOkOutcomes() throws Exception {
        Fixture fixture = fixture();
        List<PrescriptionCreationResult> results = concurrently(
                () -> service.createPrescription(fixture.doctor(), request(fixture, "Amoxicillin")),
                () -> service.createPrescription(fixture.doctor(), request(fixture, "Amoxicillin")));

        assertThat(results).extracting(PrescriptionCreationResult::status)
                .containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.OK);
        assertThat(prescriptions.findByAppointmentId(fixture.appointment().getId())).hasSize(1);
        assertThat(status(fixture.appointment().getId())).isEqualTo(1);
    }

    @Test
    void concurrentDifferentRequestsCreateOneDocumentAndOneConflict() throws Exception {
        Fixture fixture = fixture();
        List<Object> outcomes = concurrently(
                outcome(() -> service.createPrescription(fixture.doctor(), request(fixture, "Amoxicillin"))),
                outcome(() -> service.createPrescription(fixture.doctor(), request(fixture, "Azithromycin"))));

        assertThat(outcomes.stream().filter(PrescriptionCreationResult.class::isInstance)).hasSize(1);
        assertThat(outcomes.stream().filter(ResourceConflictException.class::isInstance)).hasSize(1);
        assertThat(prescriptions.findByAppointmentId(fixture.appointment().getId())).hasSize(1);
    }

    private <T> List<T> concurrently(Callable<T> first, Callable<T> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<T> firstFuture = executor.submit(atSameTime(first, ready, start));
            Future<T> secondFuture = executor.submit(atSameTime(second, ready, start));
            ready.await();
            start.countDown();
            return List.of(firstFuture.get(), secondFuture.get());
        } finally {
            executor.shutdownNow();
        }
    }

    private <T> Callable<T> atSameTime(Callable<T> work,
            CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await();
            return work.call();
        };
    }

    private Callable<Object> outcome(Callable<PrescriptionCreationResult> call) {
        return () -> {
            try {
                return call.call();
            } catch (ResourceConflictException conflict) {
                return conflict;
            }
        };
    }

    private Fixture fixture() {
        String suffix = String.valueOf(System.nanoTime());
        Doctor doctor = new Doctor();
        doctor.setName("Doctor " + suffix);
        doctor.setSpecialty("General");
        doctor.setEmail("doctor" + suffix + "@example.test");
        doctor.setPassword("encoded-password");
        doctor.setPhone(("9" + suffix).substring(0, 10));
        doctor.setAvailableTimes(List.of("09:00"));
        doctor = doctors.save(doctor);
        Patient patient = new Patient();
        patient.setName("Patient " + suffix);
        patient.setEmail("patient" + suffix + "@example.test");
        patient.setPassword("encoded-password");
        patient.setPhone(("8" + suffix).substring(0, 10));
        patient.setAddress("Bangkok");
        patient = patients.save(patient);
        Appointment appointment = new Appointment();
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setAppointmentTime(LocalDateTime.now().plusDays(7));
        appointment.setStatus(0);
        appointment = appointments.save(appointment);
        return new Fixture(new AuthenticatedUser(doctor.getId(), doctor.getEmail(), Role.DOCTOR), appointment);
    }

    private PrescriptionCreateRequest request(Fixture fixture, String medication) {
        return new PrescriptionCreateRequest("Patient", fixture.appointment().getId(), medication, "500mg", "  ");
    }

    private int status(Long appointmentId) {
        return appointments.findById(appointmentId).orElseThrow().getStatus();
    }

    private record Fixture(AuthenticatedUser doctor, Appointment appointment) { }
}
