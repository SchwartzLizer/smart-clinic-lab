package com.project.back_end.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class ModelValidationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void doctorMapsAvailabilityAndRejectsInvalidInput() throws Exception {
        assertNotNull(Doctor.class.getAnnotation(Entity.class));
        assertNotNull(field(Doctor.class, "id").getAnnotation(Id.class));
        assertNotNull(field(Doctor.class, "id").getAnnotation(GeneratedValue.class));
        assertEquals(List.class, field(Doctor.class, "availableTimes").getType());
        assertNotNull(field(Doctor.class, "availableTimes").getAnnotation(ElementCollection.class));

        Doctor doctor = new Doctor();
        set(doctor, "name", "Al");
        set(doctor, "specialty", "ER");
        set(doctor, "email", "not-an-email");
        set(doctor, "password", "123");
        set(doctor, "phone", "123");

        assertTrue(validator.validate(doctor).size() >= 5);
        JsonProperty passwordJson = field(Doctor.class, "password").getAnnotation(JsonProperty.class);
        assertNotNull(passwordJson);
        assertEquals(JsonProperty.Access.WRITE_ONLY, passwordJson.access());
    }

    @Test
    void appointmentMapsRelationshipsAndDerivesOneHourWindow() throws Exception {
        assertNotNull(Appointment.class.getAnnotation(Entity.class));
        assertNotNull(field(Appointment.class, "doctor").getAnnotation(ManyToOne.class));
        assertNotNull(field(Appointment.class, "patient").getAnnotation(ManyToOne.class));

        Appointment appointment = new Appointment();
        LocalDateTime start = LocalDateTime.of(2030, 6, 15, 9, 30);
        set(appointment, "appointmentTime", start);

        assertEquals(LocalDateTime.of(2030, 6, 15, 10, 30), invoke(appointment, "getEndTime"));
        assertEquals(LocalDate.of(2030, 6, 15), invoke(appointment, "getAppointmentDate"));
        assertEquals(LocalTime.of(9, 30), invoke(appointment, "getAppointmentTimeOnly"));
    }

    @Test
    void adminAndPatientEnforceRequiredCredentials() throws Exception {
        assertNotNull(Admin.class.getAnnotation(Entity.class));
        assertFalse(validator.validate(new Admin()).isEmpty());
        JsonProperty adminPassword = field(Admin.class, "password").getAnnotation(JsonProperty.class);
        assertNotNull(adminPassword);
        assertEquals(JsonProperty.Access.WRITE_ONLY, adminPassword.access());

        assertNotNull(Patient.class.getAnnotation(Entity.class));
        assertTrue(validator.validate(new Patient()).size() >= 5);
    }

    @Test
    void prescriptionIsValidatedMongoDocument() throws Exception {
        Document document = Prescription.class.getAnnotation(Document.class);
        assertNotNull(document);
        assertEquals("prescriptions", document.collection());
        assertFalse(validator.validate(new Prescription()).isEmpty());
        assertEquals(String.class, field(Prescription.class, "id").getType());
        assertEquals(Long.class, field(Prescription.class, "appointmentId").getType());
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void set(Object target, String fieldName, Object value) throws Exception {
        field(target.getClass(), fieldName).set(target, value);
    }

    private static Object invoke(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        return method.invoke(target);
    }
}
