package com.project.back_end.DTO.prescription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PrescriptionCreateRequest(@NotBlank @Size(min = 3, max = 100) String patientName,
        @NotNull Long appointmentId,
        @NotBlank @Size(min = 3, max = 100) String medication,
        @NotBlank @Size(min = 3, max = 20) String dosage,
        @Size(max = 200) String doctorNotes) {
}
