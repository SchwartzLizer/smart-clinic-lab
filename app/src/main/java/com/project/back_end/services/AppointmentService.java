package com.project.back_end.services;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.repo.AppointmentRepository;

@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    public AppointmentService(AppointmentRepository appointmentRepository, TokenService tokenService,
            DoctorService doctorService) {
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    /**
     * Saves a new appointment.
     *
     * @param appointment appointment requested by a patient
     * @return 1 when saved, otherwise 0
     */
    @Transactional
    public int bookAppointment(Appointment appointment) {
        try {
            appointmentRepository.save(appointment);
            return 1;
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    /**
     * Retrieves every appointment for one doctor on one calendar date.
     *
     * @param doctorId doctor whose schedule is requested
     * @param date selected calendar date
     * @return matching appointments
     */
    @Transactional(readOnly = true)
    public List<Appointment> getAppointmentsForDoctorOnDate(Long doctorId, LocalDate date) {
        return appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                doctorId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay().minusNanos(1));
    }

    /**
     * Returns a doctor's daily appointments, optionally filtered by patient name.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAppointment(String patientName, LocalDate date, String token) {
        String doctorEmail = tokenService.extractIdentifier(token);
        Long doctorId = appointmentRepository.findAll().stream()
                .filter(appointment -> appointment.getDoctor().getEmail().equals(doctorEmail))
                .map(appointment -> appointment.getDoctor().getId())
                .findFirst()
                .orElse(null);

        List<Appointment> found;
        if (doctorId == null) {
            found = List.of();
        } else if (patientName == null || patientName.isBlank() || "all".equalsIgnoreCase(patientName)) {
            found = getAppointmentsForDoctorOnDate(doctorId, date);
        } else {
            found = appointmentRepository.findByDoctorIdAndPatientNameAndAppointmentTimeBetween(
                    doctorId,
                    patientName,
                    date.atStartOfDay(),
                    date.plusDays(1).atStartOfDay().minusNanos(1));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("appointments", found.stream().map(AppointmentDTO::new).toList());
        return body;
    }

    /** Updates an appointment only when it exists and belongs to the token owner. */
    @Transactional
    public ResponseEntity<Map<String, String>> updateAppointment(Appointment appointment, String token) {
        if (appointment.getId() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Appointment id is required"));
        }
        Appointment existing = appointmentRepository.findById(appointment.getId()).orElse(null);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Appointment not found"));
        }
        if (!owns(existing, token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Patient does not own appointment"));
        }
        appointmentRepository.save(appointment);
        return ResponseEntity.ok(Map.of("message", "Appointment updated"));
    }

    /** Cancels an appointment only when it belongs to the authenticated patient. */
    @Transactional
    public ResponseEntity<Map<String, String>> cancelAppointment(long id, String token) {
        Appointment appointment = appointmentRepository.findById(id).orElse(null);
        if (appointment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Appointment not found"));
        }
        if (!owns(appointment, token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Patient does not own appointment"));
        }
        appointmentRepository.delete(appointment);
        return ResponseEntity.ok(Map.of("message", "Appointment cancelled"));
    }

    private boolean owns(Appointment appointment, String token) {
        return appointment.getPatient().getEmail().equals(tokenService.extractIdentifier(token));
    }

    /** Marks the appointment status after a doctor saves a prescription. */
    @Transactional
    public void changeStatus(Long id, int status) {
        appointmentRepository.updateStatus(status, id);
    }
}
