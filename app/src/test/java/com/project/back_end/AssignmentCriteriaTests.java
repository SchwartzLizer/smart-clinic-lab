package com.project.back_end;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.util.Map;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import com.project.back_end.controllers.DoctorController;
import com.project.back_end.controllers.PrescriptionController;
import com.project.back_end.repo.PatientRepository;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.DoctorService;
import com.project.back_end.services.TokenService;
class AssignmentCriteriaTests {
    @Test void q5DoctorAvailabilityEndpointExists() throws Exception {
        var method=DoctorController.class.getMethod("getDoctorAvailability",String.class,Long.class,LocalDate.class,String.class);
        assertNotNull(method.getAnnotation(GetMapping.class));assertEquals(ResponseEntity.class,method.getReturnType());
    }
    @Test void q6AppointmentMethodsExist() throws Exception {
        assertEquals(int.class,AppointmentService.class.getMethod("bookAppointment",com.project.back_end.models.Appointment.class).getReturnType());
        assertEquals(Map.class,AppointmentService.class.getMethod("getAppointment",String.class,LocalDate.class,String.class).getReturnType());
        assertEquals(java.util.List.class,
                AppointmentService.class.getMethod("getAppointmentsForDoctorOnDate",Long.class,LocalDate.class).getReturnType());
    }
    @Test void q7PrescriptionPostExists() throws Exception {
        var method=PrescriptionController.class.getMethod("savePrescription",String.class,com.project.back_end.models.Prescription.class);
        assertNotNull(method.getAnnotation(PostMapping.class));assertEquals(ResponseEntity.class,method.getReturnType());
    }
    @Test void q8PatientQueriesExist() throws Exception {
        assertNotNull(PatientRepository.class.getMethod("findByEmail",String.class));
        assertNotNull(PatientRepository.class.getMethod("findByEmailOrPhone",String.class,String.class));
    }
    @Test void q9TokenMethodsExist() throws Exception {
        assertEquals(String.class,TokenService.class.getMethod("generateToken",String.class).getReturnType());
        assertEquals(SecretKey.class,TokenService.class.getMethod("getSigningKey").getReturnType());
    }
    @Test void q10DoctorAvailabilityAndLoginExist() throws Exception {
        assertNotNull(DoctorService.class.getMethod("getDoctorAvailability",Long.class,LocalDate.class));
        assertEquals(ResponseEntity.class,DoctorService.class.getMethod("validateDoctor",com.project.back_end.DTO.Login.class).getReturnType());
    }
}
