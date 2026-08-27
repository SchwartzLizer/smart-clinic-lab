package com.project.back_end.controllers;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.project.back_end.models.Prescription;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.PrescriptionService;
import com.project.back_end.services.Service;
import jakarta.validation.Valid;
@RestController
@RequestMapping("${api.path}prescription")
public class PrescriptionController {
    private final PrescriptionService prescriptions;private final Service service;private final AppointmentService appointments;
    public PrescriptionController(PrescriptionService prescriptions,Service service,AppointmentService appointments){this.prescriptions=prescriptions;this.service=service;this.appointments=appointments;}
    @PostMapping("/{token}") public ResponseEntity<Map<String,String>> savePrescription(@PathVariable String token,@Valid @RequestBody Prescription prescription){ResponseEntity<Map<String,String>> invalid=service.validateToken(token,"doctor");if(invalid!=null)return invalid;ResponseEntity<Map<String,String>> response=prescriptions.savePrescription(prescription);if(response.getStatusCode().is2xxSuccessful())appointments.changeStatus(prescription.getAppointmentId(),1);return response;}
    @GetMapping("/{appointmentId}/{token}") public ResponseEntity<Map<String,Object>> getPrescription(@PathVariable Long appointmentId,@PathVariable String token){ResponseEntity<Map<String,String>> invalid=service.validateToken(token,"doctor");return invalid!=null?ResponseEntity.status(invalid.getStatusCode()).body(new LinkedHashMap<>(invalid.getBody())):prescriptions.getPrescription(appointmentId);}
}
