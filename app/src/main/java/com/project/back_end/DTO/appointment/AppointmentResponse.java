package com.project.back_end.DTO.appointment;

import java.time.LocalDateTime;

public record AppointmentResponse(Long id, DoctorSummary doctor, PatientSummary patient,
        LocalDateTime appointmentTime, int status) {

    public record DoctorSummary(Long id, String name, String specialty) {
    }

    public record PatientSummary(Long id, String name) {
    }
}
