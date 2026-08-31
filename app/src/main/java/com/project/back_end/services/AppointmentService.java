package com.project.back_end.services;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.DTO.appointment.AppointmentCreateRequest;
import com.project.back_end.DTO.appointment.AppointmentResponse;
import com.project.back_end.DTO.appointment.AppointmentUpdateRequest;
import com.project.back_end.exceptions.ForbiddenOperationException;
import com.project.back_end.exceptions.ResourceConflictException;
import com.project.back_end.exceptions.ResourceNotFoundException;
import com.project.back_end.mappers.AppointmentMapper;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import com.project.back_end.security.AuthenticatedUser;
import com.project.back_end.security.Role;

@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;
    private final DoctorRepository doctors;
    private final PatientRepository patients;
    private final AppointmentMapper mapper;
    private final Clock clock;

    /** Compatibility constructor for course-era callers. */
    public AppointmentService(AppointmentRepository appointmentRepository, TokenService tokenService,
            DoctorService ignoredDoctorService) {
        this(appointmentRepository, tokenService, null, null, new AppointmentMapper(), Clock.system(ZoneId.of("Asia/Bangkok")));
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AppointmentService(AppointmentRepository appointmentRepository, TokenService tokenService,
            DoctorRepository doctors, PatientRepository patients, AppointmentMapper mapper, Clock clinicClock) {
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
        this.doctors = doctors;
        this.patients = patients;
        this.mapper = mapper;
        this.clock = clinicClock;
    }

    public AppointmentService(AppointmentRepository appointmentRepository, TokenService tokenService,
            DoctorRepository doctors, PatientRepository patients, Clock clock) {
        this(appointmentRepository, tokenService, doctors, patients, new AppointmentMapper(), clock);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> listAppointments(AuthenticatedUser principal, String status, LocalDate date,
            String patientName) {
        requireRole(principal, Role.PATIENT, Role.DOCTOR);
        List<Appointment> found;
        if (principal.role() == Role.DOCTOR) {
            found = date == null
                    ? appointmentRepository.findByDoctorIdOrderByAppointmentTimeAsc(principal.accountId())
                    : appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(principal.accountId(),
                            date.atStartOfDay(), date.plusDays(1).atStartOfDay().minusNanos(1));
        } else {
            found = appointmentRepository.findByPatientId(principal.accountId());
        }
        return found.stream()
                .filter(appointment -> date == null || date.equals(appointment.getAppointmentDate()))
                .filter(appointment -> matchesStatus(appointment, status))
                .filter(appointment -> matchesPatientName(appointment, patientName))
                .sorted(Comparator.comparing(Appointment::getAppointmentTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public AppointmentResponse createAppointment(AuthenticatedUser principal, AppointmentCreateRequest request) {
        requireRole(principal, Role.PATIENT);
        if (patients == null || doctors == null) {
            throw new IllegalStateException("Appointment workflow repositories are not configured");
        }
        Patient patient = patients.findById(principal.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        Doctor doctor = doctors.findById(request.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        validateSlot(doctor, request.appointmentTime());
        ensureSlotFree(doctor.getId(), request.appointmentTime(), null);
        Appointment appointment = new Appointment();
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setAppointmentTime(request.appointmentTime());
        appointment.setStatus(0);
        return mapper.toResponse(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse updateAppointment(AuthenticatedUser principal, Long appointmentId,
            AppointmentUpdateRequest request) {
        requireRole(principal, Role.PATIENT);
        Appointment appointment = ownedAppointment(principal, appointmentId);
        validateSlot(appointment.getDoctor(), request.appointmentTime());
        ensureSlotFree(appointment.getDoctor().getId(), request.appointmentTime(), appointmentId);
        appointment.setAppointmentTime(request.appointmentTime());
        return mapper.toResponse(appointmentRepository.save(appointment));
    }

    @Transactional
    public void deleteAppointment(AuthenticatedUser principal, Long appointmentId) {
        requireRole(principal, Role.PATIENT);
        appointmentRepository.delete(ownedAppointment(principal, appointmentId));
    }

    private Appointment ownedAppointment(AuthenticatedUser principal, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        if (appointment.getPatient() == null || !principal.accountId().equals(appointment.getPatient().getId())) {
            throw new ForbiddenOperationException("Patient does not own appointment");
        }
        return appointment;
    }

    private void validateSlot(Doctor doctor, LocalDateTime requested) {
        if (requested == null || !requested.isAfter(LocalDateTime.now(clock))) {
            throw new ResourceConflictException("Appointment time must be in the future");
        }
        boolean available = doctor != null && doctor.getAvailableTimes() != null
                && doctor.getAvailableTimes().stream().anyMatch(slot -> slotStart(slot).equals(requested.toLocalTime()));
        if (!available) {
            throw new ResourceConflictException("Appointment time unavailable");
        }
    }

    private void ensureSlotFree(Long doctorId, LocalDateTime requested, Long ignoreAppointmentId) {
        LocalDate date = requested.toLocalDate();
        boolean booked = appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(doctorId, date.atStartOfDay(),
                date.plusDays(1).atStartOfDay().minusNanos(1)).stream()
                .anyMatch(existing -> !existing.getId().equals(ignoreAppointmentId)
                        && requested.equals(existing.getAppointmentTime()));
        if (booked) {
            throw new ResourceConflictException("Appointment time unavailable");
        }
    }

    private LocalTime slotStart(String slot) {
        try {
            return LocalTime.parse(slot.split("-", 2)[0].trim());
        } catch (RuntimeException exception) {
            return LocalTime.MIN;
        }
    }

    private boolean matchesStatus(Appointment appointment, String status) {
        if (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) return true;
        if ("upcoming".equalsIgnoreCase(status) || "booked".equalsIgnoreCase(status)) return appointment.getStatus() == 0;
        if ("past".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status)) return appointment.getStatus() == 1;
        throw new ResourceConflictException("Unsupported appointment status filter");
    }

    private boolean matchesPatientName(Appointment appointment, String patientName) {
        return patientName == null || patientName.isBlank() || "all".equalsIgnoreCase(patientName)
                || (appointment.getPatient() != null && appointment.getPatient().getName() != null
                        && appointment.getPatient().getName().toLowerCase().contains(patientName.toLowerCase()));
    }

    private void requireRole(AuthenticatedUser principal, Role... allowed) {
        if (principal == null || java.util.Arrays.stream(allowed).noneMatch(role -> role == principal.role())) {
            throw new ForbiddenOperationException("Role cannot perform this appointment operation");
        }
    }

    // Course-era APIs retained until assignment evidence is migrated.
    @Transactional
    public int bookAppointment(Appointment appointment) {
        try { appointmentRepository.save(appointment); return 1; }
        catch (RuntimeException exception) { return 0; }
    }

    @Transactional(readOnly = true)
    public List<Appointment> getAppointmentsForDoctorOnDate(Long doctorId, LocalDate date) {
        return appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(doctorId, date.atStartOfDay(),
                date.plusDays(1).atStartOfDay().minusNanos(1));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAppointment(String patientName, LocalDate date, String token) {
        String doctorEmail = tokenService.extractIdentifier(token);
        Long doctorId = appointmentRepository.findAll().stream()
                .filter(appointment -> appointment.getDoctor() != null
                        && appointment.getDoctor().getEmail().equals(doctorEmail))
                .map(appointment -> appointment.getDoctor().getId()).findFirst().orElse(null);
        List<Appointment> found = doctorId == null ? List.of()
                : (patientName == null || patientName.isBlank() || "all".equalsIgnoreCase(patientName)
                        ? getAppointmentsForDoctorOnDate(doctorId, date)
                        : appointmentRepository.findByDoctorIdAndPatientNameAndAppointmentTimeBetween(doctorId,
                                patientName, date.atStartOfDay(), date.plusDays(1).atStartOfDay().minusNanos(1)));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("appointments", found.stream().map(AppointmentDTO::new).toList());
        return body;
    }

    @Transactional
    public ResponseEntity<Map<String, String>> updateAppointment(Appointment appointment, String token) {
        if (appointment.getId() == null) return ResponseEntity.badRequest().body(Map.of("message", "Appointment id is required"));
        Appointment existing = appointmentRepository.findById(appointment.getId()).orElse(null);
        if (existing == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Appointment not found"));
        if (!owns(existing, token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Patient does not own appointment"));
        appointmentRepository.save(appointment);
        return ResponseEntity.ok(Map.of("message", "Appointment updated"));
    }

    @Transactional
    public ResponseEntity<Map<String, String>> cancelAppointment(long id, String token) {
        Appointment appointment = appointmentRepository.findById(id).orElse(null);
        if (appointment == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Appointment not found"));
        if (!owns(appointment, token)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Patient does not own appointment"));
        appointmentRepository.delete(appointment);
        return ResponseEntity.ok(Map.of("message", "Appointment cancelled"));
    }

    private boolean owns(Appointment appointment, String token) {
        return appointment.getPatient() != null && appointment.getPatient().getEmail().equals(tokenService.extractIdentifier(token));
    }

    @Transactional
    public void changeStatus(Long id, int status) {
        appointmentRepository.updateStatus(status, id);
    }
}
