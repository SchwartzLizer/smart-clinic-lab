package com.project.back_end.services;

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.back_end.DTO.doctor.DoctorCreateRequest;
import com.project.back_end.config.properties.BootstrapProperties;
import com.project.back_end.models.Admin;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@Service
@Profile("cloud")
public class CloudIdentityBootstrapService {

    private static final Pattern ADMIN_USERNAME = Pattern.compile("[a-z0-9._-]{3,100}");
    private static final Pattern PHONE = Pattern.compile("[0-9]{10}");
    private static final Pattern SLOT = Pattern.compile("(?:[01][0-9]|2[0-3]):[0-5][0-9]-(?:[01][0-9]|2[0-3]):[0-5][0-9]");

    private final AdminRepository admins;
    private final DoctorRepository doctors;
    private final DoctorService doctorService;
    private final PasswordEncoder passwords;
    private final Validator validator;

    public CloudIdentityBootstrapService(AdminRepository admins, DoctorRepository doctors,
            DoctorService doctorService, PasswordEncoder passwords, Validator validator) {
        this.admins = admins;
        this.doctors = doctors;
        this.doctorService = doctorService;
        this.passwords = passwords;
        this.validator = validator;
    }

    @Transactional
    public BootstrapResult bootstrap(BootstrapProperties properties) {
        BootstrapInput input = BootstrapInput.from(properties);
        DoctorCreateRequest request = input.doctorCreateRequest();
        validate(request);
        AdminState adminState = inspectAdmin(input);
        DoctorState doctorState = inspectDoctor(input);
        try {
            boolean wroteIdentity = false;
            if (adminState == AdminState.MISSING) {
                Admin admin = new Admin();
                admin.setUsername(input.adminUsername());
                admin.setPassword(passwords.encode(input.adminPassword()));
                admins.save(admin);
                wroteIdentity = true;
            }
            if (doctorState == DoctorState.MISSING) {
                doctorService.createDoctor(request);
                wroteIdentity = true;
            }
            if (wroteIdentity) {
                admins.flush();
                doctors.flush();
            }
        } catch (DataAccessException exception) {
            throw new BootstrapException("Cloud identity bootstrap failed: database conflict");
        } catch (RuntimeException exception) {
            if (exception instanceof BootstrapException) {
                throw exception;
            }
            throw new BootstrapException("Cloud identity bootstrap failed: identity creation");
        }
        return new BootstrapResult(adminState.statusAfterBootstrap(), doctorState.statusAfterBootstrap());
    }

    private AdminState inspectAdmin(BootstrapInput input) {
        List<Admin> existing = admins.findAll();
        if (existing.isEmpty()) {
            return AdminState.MISSING;
        }
        if (existing.size() != 1) {
            throw new BootstrapException("Cloud identity bootstrap failed: admin state");
        }
        Admin admin = existing.get(0);
        if (!input.adminUsername().equals(admin.getUsername()) || !matches(input.adminPassword(), admin.getPassword())) {
            throw new BootstrapException("Cloud identity bootstrap failed: admin credentials");
        }
        return AdminState.EXISTING;
    }

    private DoctorState inspectDoctor(BootstrapInput input) {
        List<Doctor> existing = doctors.findAll();
        if (existing.isEmpty()) {
            return DoctorState.MISSING;
        }
        if (existing.size() != 1) {
            throw new BootstrapException("Cloud identity bootstrap failed: doctor state");
        }
        Doctor doctor = existing.get(0);
        if (!input.doctorName().equals(doctor.getName())
                || !input.doctorSpecialty().equals(doctor.getSpecialty())
                || !input.doctorEmail().equals(doctor.getEmail())
                || !input.doctorPhone().equals(doctor.getPhone())
                || !input.availableTimes().equals(normalizeAvailability(doctor.getAvailableTimes()))
                || !matches(input.doctorPassword(), doctor.getPassword())) {
            throw new BootstrapException("Cloud identity bootstrap failed: doctor state");
        }
        return DoctorState.EXISTING;
    }

    private boolean matches(String rawPassword, String storedHash) {
        try {
            return storedHash != null && passwords.matches(rawPassword, storedHash);
        } catch (RuntimeException exception) {
            throw new BootstrapException("Cloud identity bootstrap failed: stored credential");
        }
    }

    private void validate(DoctorCreateRequest request) {
        List<String> fields = validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .sorted()
                .toList();
        if (!fields.isEmpty()) {
            throw new BootstrapException("Cloud identity bootstrap failed: invalid doctor fields "
                    + String.join(", ", fields));
        }
    }

    public record BootstrapResult(String admin, String doctor) {
    }

    public static class BootstrapException extends IllegalStateException {
        BootstrapException(String message) {
            super(message);
        }
    }

    private enum AdminState {
        MISSING("created"), EXISTING("existing");

        private final String status;

        AdminState(String status) {
            this.status = status;
        }

        String statusAfterBootstrap() {
            return status;
        }
    }

    private enum DoctorState {
        MISSING("created"), EXISTING("existing");

        private final String status;

        DoctorState(String status) {
            this.status = status;
        }

        String statusAfterBootstrap() {
            return status;
        }
    }

    private record BootstrapInput(String adminUsername, String adminPassword, String doctorName,
            String doctorSpecialty, String doctorEmail, String doctorPassword, String doctorPhone,
            List<String> availableTimes) {

        static BootstrapInput from(BootstrapProperties properties) {
            if (properties == null || !properties.enabled()) {
                throw new BootstrapException("Cloud identity bootstrap failed: disabled configuration");
            }
            String adminUsername = lowerTrim(properties.adminUsername(), "admin username");
            if (!ADMIN_USERNAME.matcher(adminUsername).matches()) {
                throw new BootstrapException("Cloud identity bootstrap failed: invalid admin username");
            }
            String doctorName = trim(properties.doctorName(), "doctor name");
            String doctorSpecialty = trim(properties.doctorSpecialty(), "doctor specialty");
            String doctorEmail = lowerTrim(properties.doctorEmail(), "doctor email");
            String doctorPhone = trim(properties.doctorPhone(), "doctor phone");
            if (doctorName.length() < 3 || doctorName.length() > 100) {
                throw new BootstrapException("Cloud identity bootstrap failed: invalid doctor name");
            }
            if (doctorSpecialty.length() < 3 || doctorSpecialty.length() > 50) {
                throw new BootstrapException("Cloud identity bootstrap failed: invalid doctor specialty");
            }
            if (doctorEmail.length() > 255 || !doctorEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new BootstrapException("Cloud identity bootstrap failed: invalid doctor email");
            }
            if (!PHONE.matcher(doctorPhone).matches()) {
                throw new BootstrapException("Cloud identity bootstrap failed: invalid doctor phone");
            }
            String adminPassword = password(properties.adminPassword(), "admin password");
            String doctorPassword = password(properties.doctorPassword(), "doctor password");
            if (adminPassword.equals(doctorPassword)) {
                throw new BootstrapException("Cloud identity bootstrap failed: passwords must differ");
            }
            return new BootstrapInput(adminUsername, adminPassword, doctorName, doctorSpecialty, doctorEmail,
                    doctorPassword, doctorPhone, normalizeAvailability(properties.doctorAvailableTimes()));
        }

        DoctorCreateRequest doctorCreateRequest() {
            return new DoctorCreateRequest(doctorName, doctorSpecialty, doctorEmail, doctorPassword, doctorPhone,
                    availableTimes);
        }

        private static String lowerTrim(String value, String field) {
            return trim(value, field).toLowerCase(Locale.ROOT);
        }

        private static String trim(String value, String field) {
            if (value == null || value.isBlank()) {
                throw new BootstrapException("Cloud identity bootstrap failed: missing " + field);
            }
            return value.trim();
        }

        private static String password(String value, String field) {
            if (value == null || value.isBlank() || value.chars().anyMatch(Character::isISOControl)) {
                throw new BootstrapException("Cloud identity bootstrap failed: invalid " + field);
            }
            int length = value.getBytes(StandardCharsets.UTF_8).length;
            if (length < 16 || length > 72) {
                throw new BootstrapException("Cloud identity bootstrap failed: invalid " + field);
            }
            return value;
        }
    }

    private static List<String> normalizeAvailability(String configured) {
        if (configured == null || configured.isBlank()) {
            throw new BootstrapException("Cloud identity bootstrap failed: missing doctor availability");
        }
        String[] rawSlots = configured.split(",", -1);
        List<AvailabilitySlot> slots = new ArrayList<>();
        for (String rawSlot : rawSlots) {
            String slot = rawSlot.trim();
            if (slot.length() > 50 || !SLOT.matcher(slot).matches()) {
                throw new BootstrapException("Cloud identity bootstrap failed: invalid doctor availability");
            }
            LocalTime start = LocalTime.parse(slot.substring(0, 5));
            LocalTime end = LocalTime.parse(slot.substring(6, 11));
            if (!start.isBefore(end)) {
                throw new BootstrapException("Cloud identity bootstrap failed: invalid doctor availability");
            }
            slots.add(new AvailabilitySlot(slot, start, end));
        }
        slots.sort(Comparator.comparing(AvailabilitySlot::start));
        for (int index = 1; index < slots.size(); index++) {
            if (slots.get(index - 1).end().isAfter(slots.get(index).start())) {
                throw new BootstrapException("Cloud identity bootstrap failed: overlapping doctor availability");
            }
        }
        return slots.stream().map(AvailabilitySlot::value).toList();
    }

    private static List<String> normalizeAvailability(List<String> stored) {
        if (stored == null || stored.isEmpty()) {
            throw new BootstrapException("Cloud identity bootstrap failed: doctor state");
        }
        return normalizeAvailability(String.join(",", stored));
    }

    private record AvailabilitySlot(String value, LocalTime start, LocalTime end) {
    }

}
