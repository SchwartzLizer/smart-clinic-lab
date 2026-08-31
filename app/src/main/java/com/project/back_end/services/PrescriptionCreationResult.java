package com.project.back_end.services;

import org.springframework.http.HttpStatus;

import com.project.back_end.DTO.prescription.PrescriptionResponse;

public record PrescriptionCreationResult(PrescriptionResponse response, HttpStatus status) {
}
