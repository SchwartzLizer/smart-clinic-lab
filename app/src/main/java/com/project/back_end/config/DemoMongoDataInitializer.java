package com.project.back_end.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.project.back_end.models.Prescription;
import com.project.back_end.repo.PrescriptionRepository;

@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoMongoDataInitializer implements ApplicationRunner {

    private final PrescriptionRepository prescriptions;

    public DemoMongoDataInitializer(PrescriptionRepository prescriptions) {
        this.prescriptions = prescriptions;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedPrescription(1L, new Prescription(
                "Jane Doe", 1L, "Amoxicillin", "500mg", "Take with food twice daily."));
    }

    private void seedPrescription(Long appointmentId, Prescription prescription) {
        List<Prescription> existing = prescriptions.findByAppointmentId(appointmentId);
        if (existing == null || existing.isEmpty()) {
            prescriptions.save(prescription);
        }
    }
}
