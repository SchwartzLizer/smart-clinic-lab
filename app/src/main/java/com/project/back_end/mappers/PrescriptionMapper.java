package com.project.back_end.mappers;

import org.springframework.stereotype.Component;

import com.project.back_end.DTO.prescription.PrescriptionResponse;
import com.project.back_end.models.Prescription;

@Component
public class PrescriptionMapper {
    public PrescriptionResponse toResponse(Prescription prescription) {
        return new PrescriptionResponse(prescription.getId(), prescription.getAppointmentId(),
                prescription.getPatientName(), prescription.getMedication(), prescription.getDosage(),
                prescription.getDoctorNotes());
    }
}
