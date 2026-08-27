package com.project.back_end.controllers;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.project.back_end.models.Appointment;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.Service;
import jakarta.validation.Valid;
@RestController
@RequestMapping("/appointments")
public class AppointmentController {
    private final AppointmentService appointments;private final Service service;
    public AppointmentController(AppointmentService appointments,Service service){this.appointments=appointments;this.service=service;}
    @GetMapping("/{date}/{patientName}/{token}") public ResponseEntity<Map<String,Object>> getAppointments(@PathVariable @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,@PathVariable String patientName,@PathVariable String token){
        ResponseEntity<Map<String,String>> invalid=service.validateToken(token,"doctor");if(invalid!=null)return ResponseEntity.status(invalid.getStatusCode()).body(new LinkedHashMap<>(invalid.getBody()));
        return ResponseEntity.ok(appointments.getAppointment(patientName,date,token));
    }
    @PostMapping("/{token}") public ResponseEntity<Map<String,String>> bookAppointment(@Valid @RequestBody Appointment appointment,@PathVariable String token){
        ResponseEntity<Map<String,String>> invalid=service.validateToken(token,"patient");if(invalid!=null)return invalid;int valid=service.validateAppointment(appointment);
        if(valid==-1)return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message","Doctor not found"));if(valid==0)return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message","Appointment time unavailable"));
        return appointments.bookAppointment(appointment)==1?ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message","Appointment booked")):ResponseEntity.internalServerError().body(Map.of("message","Unable to book appointment"));
    }
    @PutMapping("/{token}") public ResponseEntity<Map<String,String>> updateAppointment(@Valid @RequestBody Appointment appointment,@PathVariable String token){ResponseEntity<Map<String,String>> invalid=service.validateToken(token,"patient");return invalid!=null?invalid:appointments.updateAppointment(appointment,token);}
    @DeleteMapping("/{id}/{token}") public ResponseEntity<Map<String,String>> cancelAppointment(@PathVariable long id,@PathVariable String token){ResponseEntity<Map<String,String>> invalid=service.validateToken(token,"patient");return invalid!=null?invalid:appointments.cancelAppointment(id,token);}
}
