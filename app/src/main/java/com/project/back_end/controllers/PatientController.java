package com.project.back_end.controllers;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.project.back_end.DTO.Login;
import com.project.back_end.models.Patient;
import com.project.back_end.services.PatientService;
import com.project.back_end.services.Service;
import jakarta.validation.Valid;
@RestController
@RequestMapping("/patient")
public class PatientController {
    private final PatientService patients;private final Service service;
    public PatientController(PatientService patients,Service service){this.patients=patients;this.service=service;}
    @GetMapping("/{token}") public ResponseEntity<Map<String,Object>> getPatient(@PathVariable String token){ResponseEntity<Map<String,String>> invalid=service.validateToken(token,"patient");return invalid!=null?ResponseEntity.status(invalid.getStatusCode()).body(new LinkedHashMap<>(invalid.getBody())):patients.getPatientDetails(token);}
    @PostMapping public ResponseEntity<Map<String,String>> createPatient(@Valid @RequestBody Patient patient){if(!service.validatePatient(patient))return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message","Patient with email id or phone no already exists"));return patients.createPatient(patient)==1?ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message","Signup successful")):ResponseEntity.internalServerError().body(Map.of("message","Internal server error"));}
    @PostMapping("/login") public ResponseEntity<Map<String,String>> login(@Valid @RequestBody Login login){return service.validatePatientLogin(login);}
    @GetMapping("/{id}/{user}/{token}") public ResponseEntity<Map<String,Object>> getPatientAppointment(@PathVariable Long id,@PathVariable String user,@PathVariable String token){ResponseEntity<Map<String,String>> invalid=service.validateToken(token,user);return invalid!=null?ResponseEntity.status(invalid.getStatusCode()).body(new LinkedHashMap<>(invalid.getBody())):ResponseEntity.ok(patients.getPatientAppointment(id));}
    @GetMapping("/filter/{condition}/{name}/{token}") public ResponseEntity<Map<String,Object>> filterPatientAppointment(@PathVariable String condition,@PathVariable String name,@PathVariable String token){ResponseEntity<Map<String,String>> invalid=service.validateToken(token,"patient");return invalid!=null?ResponseEntity.status(invalid.getStatusCode()).body(new LinkedHashMap<>(invalid.getBody())):ResponseEntity.ok(service.filterPatient(condition,name,token));}
}
