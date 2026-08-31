package com.project.back_end.mappers;

import org.springframework.stereotype.Component;

import com.project.back_end.DTO.appointment.AppointmentResponse;
import com.project.back_end.models.Appointment;

@Component
public class AppointmentMapper {
    public AppointmentResponse toResponse(Appointment appointment) {
        var doctor = appointment.getDoctor();
        var patient = appointment.getPatient();
        return new AppointmentResponse(appointment.getId(),
                doctor == null ? null : new AppointmentResponse.DoctorSummary(doctor.getId(), doctor.getName(),
                        doctor.getSpecialty()),
                patient == null ? null : new AppointmentResponse.PatientSummary(patient.getId(), patient.getName()),
                appointment.getAppointmentTime(), appointment.getStatus());
    }
}
