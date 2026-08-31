package com.project.back_end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import java.util.List;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.project.back_end.DTO.auth.AuthResponse;
import com.project.back_end.DTO.auth.UserLoginRequest;
import com.project.back_end.DTO.appointment.AppointmentCreateRequest;
import com.project.back_end.DTO.appointment.AppointmentResponse;
import com.project.back_end.DTO.appointment.AppointmentUpdateRequest;
import com.project.back_end.DTO.common.PageResponse;
import com.project.back_end.DTO.doctor.DoctorCreateRequest;
import com.project.back_end.DTO.doctor.DoctorResponse;
import com.project.back_end.DTO.prescription.PrescriptionCreateRequest;
import com.project.back_end.DTO.prescription.PrescriptionResponse;
import com.project.back_end.controllers.DoctorController;
import com.project.back_end.controllers.PrescriptionController;
import com.project.back_end.repo.PatientRepository;
import com.project.back_end.security.AuthenticatedUser;
import com.project.back_end.security.Role;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.AuthService;
import com.project.back_end.services.DoctorService;
import com.project.back_end.services.TokenService;

class AssignmentCriteriaTests {

    @Test
    void q5ModernDoctorDirectoryEndpointExists() throws Exception {
        var method = DoctorController.class.getMethod("getDoctors", String.class, String.class, String.class,
                Pageable.class);
        assertNotNull(method.getAnnotation(GetMapping.class));
        assertEquals(PageResponse.class, method.getReturnType());
    }

    @Test
    void q6ModernAppointmentMethodsExist() throws Exception {
        assertEquals(AppointmentResponse.class,
                AppointmentService.class.getMethod("createAppointment", AuthenticatedUser.class,
                        AppointmentCreateRequest.class).getReturnType());
        assertEquals(List.class,
                AppointmentService.class.getMethod("listAppointments", AuthenticatedUser.class, String.class,
                        LocalDate.class, String.class).getReturnType());
        assertEquals(AppointmentResponse.class,
                AppointmentService.class.getMethod("updateAppointment", AuthenticatedUser.class, Long.class,
                        AppointmentUpdateRequest.class).getReturnType());
    }

    @Test
    void q7ModernPrescriptionPostExists() throws Exception {
        var method = PrescriptionController.class.getMethod("createPrescription", AuthenticatedUser.class,
                PrescriptionCreateRequest.class);
        assertNotNull(method.getAnnotation(PostMapping.class));
        assertEquals(org.springframework.http.ResponseEntity.class, method.getReturnType());
        assertEquals(PrescriptionResponse.class,
                PrescriptionController.class.getMethod("getPrescription", AuthenticatedUser.class, Long.class)
                        .getReturnType());
    }

    @Test
    void q8PatientQueriesExist() throws Exception {
        assertNotNull(PatientRepository.class.getMethod("findByEmail", String.class));
        assertNotNull(PatientRepository.class.getMethod("findByEmailOrPhone", String.class, String.class));
    }

    @Test
    void q9ModernTokenMethodsExist() throws Exception {
        assertEquals(String.class,
                TokenService.class.getMethod("generateToken", Long.class, String.class, Role.class).getReturnType());
        assertEquals(SecretKey.class, TokenService.class.getMethod("getSigningKey").getReturnType());
        assertEquals(AuthenticatedUser.class, TokenService.class.getMethod("parse", String.class).getReturnType());
    }

    @Test
    void q10ModernDoctorAndLoginServicesExist() throws Exception {
        assertNotNull(DoctorService.class.getMethod("getDoctorAvailability", Long.class, LocalDate.class));
        assertEquals(DoctorResponse.class,
                DoctorService.class.getMethod("createDoctor", DoctorCreateRequest.class).getReturnType());
        assertEquals(AuthResponse.class,
                AuthService.class.getMethod("authenticateDoctor", UserLoginRequest.class).getReturnType());
    }
}
