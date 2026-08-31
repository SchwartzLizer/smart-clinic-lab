package com.project.back_end.DTO.prescription;

public record PrescriptionResponse(String id, Long appointmentId, String patientName, String medication,
        String dosage, String doctorNotes) {
}
