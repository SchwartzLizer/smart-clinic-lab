package com.project.back_end.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import com.project.back_end.models.Prescription;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.mongo.index.enabled", havingValue = "true", matchIfMissing = true)
public class PrescriptionMongoIndexConfig {

    @Bean
    InitializingBean prescriptionAppointmentIdUniqueIndex(MongoTemplate mongoTemplate) {
        return () -> mongoTemplate.indexOps(Prescription.class)
                .ensureIndex(new Index().on("appointmentId", Direction.ASC)
                        .unique().named("uq_prescription_appointment_id"));
    }
}
