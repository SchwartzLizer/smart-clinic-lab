package com.project.back_end.DTO.patient;

public record PatientResponse(Long id, String name, String email, String phone, String address) {
}
