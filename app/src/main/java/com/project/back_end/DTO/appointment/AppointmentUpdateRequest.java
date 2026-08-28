package com.project.back_end.DTO.appointment;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

public record AppointmentUpdateRequest(@NotNull @Future LocalDateTime appointmentTime) {
}
