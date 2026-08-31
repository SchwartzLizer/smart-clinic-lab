package com.project.back_end.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ApplicationArguments;

import com.project.back_end.models.Prescription;
import com.project.back_end.repo.PrescriptionRepository;

class DemoMongoDataInitializerTests {

    private final ApplicationArguments arguments = Mockito.mock(ApplicationArguments.class);

    @Test
    void insertsSeedPrescriptionWhenAppointmentHasNoPrescription() throws Exception {
        PrescriptionRepository repository = Mockito.mock(PrescriptionRepository.class);
        when(repository.findByAppointmentId(1L)).thenReturn(List.of());

        new DemoMongoDataInitializer(repository).run(arguments);

        verify(repository).save(argThat(prescription -> prescription.getAppointmentId().equals(1L)));
    }

    @Test
    void doesNotOverwriteExistingPrescription() throws Exception {
        PrescriptionRepository repository = Mockito.mock(PrescriptionRepository.class);
        when(repository.findByAppointmentId(1L)).thenReturn(List.of(new Prescription()));

        new DemoMongoDataInitializer(repository).run(arguments);

        verify(repository, never()).save(Mockito.any(Prescription.class));
    }

    @Test
    void initializerIsGuardedByDemoProfileProperty() {
        ConditionalOnProperty annotation = DemoMongoDataInitializer.class
                .getAnnotation(ConditionalOnProperty.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).containsExactly("app.demo-data.enabled");
        assertThat(annotation.havingValue()).isEqualTo("true");
    }
}
