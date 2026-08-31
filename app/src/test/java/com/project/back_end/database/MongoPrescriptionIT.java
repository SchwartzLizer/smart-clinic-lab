package com.project.back_end.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.project.back_end.models.Prescription;
import com.project.back_end.config.properties.JwtProperties;
import com.project.back_end.repo.PrescriptionRepository;
import com.project.back_end.config.PrescriptionMongoIndexConfig;

import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataMongoTest(properties = {
        "jwt.secret=test-signing-key-that-is-at-least-32-bytes-long",
        "jwt.expiration=1h",
        "app.mongo.index.enabled=true"
})
@EnableConfigurationProperties(JwtProperties.class)
@ContextConfiguration(classes = MongoPrescriptionIT.MongoTestConfiguration.class)
@Import(PrescriptionMongoIndexConfig.class)
@TestMethodOrder(OrderAnnotation.class)
class MongoPrescriptionIT {

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMongoRepositories(basePackageClasses = PrescriptionRepository.class)
    static class MongoTestConfiguration {
    }

    @Container
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired
    private PrescriptionRepository prescriptions;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    @Order(1)
    void savesAndFindsPrescriptionByAppointmentId() {
        Prescription prescription = new Prescription("Patient One", 900L, "Amoxicillin", "500mg", "Take twice daily");
        prescriptions.save(prescription);

        List<Prescription> found = prescriptions.findByAppointmentId(900L);

        assertEquals(1, found.size());
        assertEquals("Amoxicillin", found.get(0).getMedication());
    }

    @Test
    @Order(2)
    void createsNamedUniqueAppointmentIndex() {
        boolean found = mongoTemplate.getCollection("prescriptions").listIndexes()
                .into(new java.util.ArrayList<>()).stream()
                .anyMatch(index -> "uq_prescription_appointment_id".equals(index.getString("name"))
                        && Boolean.TRUE.equals(index.getBoolean("unique")));
        org.junit.jupiter.api.Assertions.assertTrue(found);
    }

    @Test
    @Order(3)
    void uniqueIndexRejectsDuplicateAndPreexistingDuplicatesAreNotCleaned() {
        Prescription duplicate = new Prescription("Patient Two", 900L, "Other medication", "250mg", null);
        assertThrows(org.springframework.dao.DuplicateKeyException.class, () -> prescriptions.save(duplicate));

        mongoTemplate.getCollection("prescriptions").drop();
        org.bson.Document first = new org.bson.Document("appointmentId", 901L)
                .append("patientName", "Patient One").append("medication", "Medicine").append("dosage", "100mg");
        mongoTemplate.getCollection("prescriptions").insertMany(java.util.List.of(first, new org.bson.Document(first)));

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> mongoTemplate.indexOps(Prescription.class).ensureIndex(new org.springframework.data.mongodb.core.index.Index()
                        .on("appointmentId", org.springframework.data.domain.Sort.Direction.ASC)
                        .unique().named("uq_prescription_appointment_id")));
        assertEquals(2, mongoTemplate.getCollection("prescriptions").countDocuments());
    }
}
