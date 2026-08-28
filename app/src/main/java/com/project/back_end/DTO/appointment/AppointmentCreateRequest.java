package com.project.back_end.DTO.appointment;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

public record AppointmentCreateRequest(@NotNull Long doctorId, @NotNull @Future LocalDateTime appointmentTime) {
}
