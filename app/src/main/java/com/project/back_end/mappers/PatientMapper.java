package com.project.back_end.mappers;

import org.springframework.stereotype.Component;

import com.project.back_end.DTO.patient.PatientResponse;
import com.project.back_end.models.Patient;

@Component
public class PatientMapper {
    public PatientResponse toResponse(Patient patient) {
        return new PatientResponse(patient.getId(), patient.getName(), patient.getEmail(), patient.getPhone(),
                patient.getAddress());
    }
}
