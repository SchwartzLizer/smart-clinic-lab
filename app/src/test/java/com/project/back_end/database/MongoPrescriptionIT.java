package com.project.back_end.database;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.project.back_end.models.Prescription;
import com.project.back_end.repo.PrescriptionRepository;

import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataMongoTest
class MongoPrescriptionIT {

    @Container
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired
    private PrescriptionRepository prescriptions;

    @Test
    void savesAndFindsPrescriptionByAppointmentId() {
        Prescription prescription = new Prescription("Patient One", 900L, "Amoxicillin", "500mg", "Take twice daily");
        prescriptions.save(prescription);

        List<Prescription> found = prescriptions.findByAppointmentId(900L);

        assertEquals(1, found.size());
        assertEquals("Amoxicillin", found.get(0).getMedication());
    }
}
