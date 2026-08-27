package com.project.back_end.controllers;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.project.back_end.DTO.Login;
import com.project.back_end.models.Doctor;
import com.project.back_end.services.DoctorService;
import com.project.back_end.services.Service;
import jakarta.validation.Valid;
@RestController
@RequestMapping("${api.path}doctor")
public class DoctorController {
    private final DoctorService doctors;private final Service service;
    public DoctorController(DoctorService doctors,Service service){this.doctors=doctors;this.service=service;}
    @GetMapping("/availability/{user}/{doctorId}/{date}/{token}")
    public ResponseEntity<Map<String,Object>> getDoctorAvailability(@PathVariable String user,@PathVariable Long doctorId,@PathVariable @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,@PathVariable String token){
        ResponseEntity<Map<String,String>> invalid=service.validateToken(token,user);if(invalid!=null)return ResponseEntity.status(invalid.getStatusCode()).body(new LinkedHashMap<>(invalid.getBody()));
        return ResponseEntity.ok(Map.of("availability",doctors.getDoctorAvailability(doctorId,date)));
    }
    @GetMapping public ResponseEntity<Map<String,Object>> getDoctor(){return ResponseEntity.ok(Map.of("doctors",doctors.getDoctors()));}
    @PostMapping("/{token}") public ResponseEntity<Map<String,String>> saveDoctor(@Valid @RequestBody Doctor doctor,@PathVariable String token){
        ResponseEntity<Map<String,String>> invalid=service.validateToken(token,"admin");if(invalid!=null)return invalid;int result=doctors.saveDoctor(doctor);
        return result==1?ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message","Doctor added to db")):result==-1?ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message","Doctor already exists")):ResponseEntity.internalServerError().body(Map.of("message","Some internal error occurred"));
    }
    @PostMapping("/login") public ResponseEntity<Map<String,String>> doctorLogin(@Valid @RequestBody Login login){return doctors.validateDoctor(login);}
    @PutMapping("/{token}") public ResponseEntity<Map<String,String>> updateDoctor(@Valid @RequestBody Doctor doctor,@PathVariable String token){
        ResponseEntity<Map<String,String>> invalid=service.validateToken(token,"admin");if(invalid!=null)return invalid;int result=doctors.updateDoctor(doctor);
        return result==1?ResponseEntity.ok(Map.of("message","Doctor updated")):result==-1?ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message","Doctor not found")):ResponseEntity.internalServerError().body(Map.of("message","Some internal error occurred"));
    }
    @DeleteMapping("/{id}/{token}") public ResponseEntity<Map<String,String>> deleteDoctor(@PathVariable long id,@PathVariable String token){
        ResponseEntity<Map<String,String>> invalid=service.validateToken(token,"admin");if(invalid!=null)return invalid;int result=doctors.deleteDoctor(id);
        return result==1?ResponseEntity.ok(Map.of("message","Doctor deleted successfully")):result==-1?ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message","Doctor not found with id")):ResponseEntity.internalServerError().body(Map.of("message","Some internal error occurred"));
    }
    @GetMapping("/filter/{name}/{time}/{speciality}") public ResponseEntity<Map<String,Object>> filter(@PathVariable String name,@PathVariable String time,@PathVariable String speciality){return ResponseEntity.ok(service.filterDoctor(name,time,speciality));}
}
