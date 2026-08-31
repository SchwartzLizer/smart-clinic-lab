package com.project.back_end.mappers;

import org.springframework.stereotype.Component;

import com.project.back_end.DTO.doctor.DoctorResponse;
import com.project.back_end.models.Doctor;

@Component
public class DoctorMapper {
    public DoctorResponse toResponse(Doctor doctor) {
        return new DoctorResponse(doctor.getId(), doctor.getName(), doctor.getSpecialty(), doctor.getEmail(),
                doctor.getPhone(), doctor.getAvailableTimes() == null ? java.util.List.of()
                        : java.util.List.copyOf(doctor.getAvailableTimes()));
    }
}
