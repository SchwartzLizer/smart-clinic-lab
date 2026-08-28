package com.project.back_end.DTO.doctor;

import java.util.List;

public record DoctorResponse(Long id, String name, String specialty, String email, String phone,
        List<String> availableTimes) {
}
