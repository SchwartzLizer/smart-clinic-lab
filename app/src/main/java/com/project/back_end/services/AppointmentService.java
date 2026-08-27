package com.project.back_end.services;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.repo.AppointmentRepository;
@Service
public class AppointmentService {
    private final AppointmentRepository appointments;private final TokenService tokens;private final DoctorService doctors;
    public AppointmentService(AppointmentRepository appointments,TokenService tokens,DoctorService doctors){this.appointments=appointments;this.tokens=tokens;this.doctors=doctors;}
    @Transactional public int bookAppointment(Appointment a){try{appointments.save(a);return 1;}catch(Exception e){return 0;}}
    @Transactional public ResponseEntity<Map<String,String>> updateAppointment(Appointment a,String token){
        if(a.getId()==null)return ResponseEntity.badRequest().body(Map.of("message","Appointment id is required"));
        Appointment old=appointments.findById(a.getId()).orElse(null);if(old==null)return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message","Appointment not found"));
        if(!owns(old,token))return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message","Patient does not own appointment"));
        appointments.save(a);return ResponseEntity.ok(Map.of("message","Appointment updated"));
    }
    @Transactional public ResponseEntity<Map<String,String>> cancelAppointment(long id,String token){
        Appointment a=appointments.findById(id).orElse(null);if(a==null)return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message","Appointment not found"));
        if(!owns(a,token))return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message","Patient does not own appointment"));
        appointments.delete(a);return ResponseEntity.ok(Map.of("message","Appointment cancelled"));
    }
    private boolean owns(Appointment a,String token){return a.getPatient().getEmail().equals(tokens.extractIdentifier(token));}
    @Transactional(readOnly=true) public Map<String,Object> getAppointment(String patientName,LocalDate date,String token){
        String email=tokens.extractIdentifier(token);Long doctorId=appointments.findAll().stream().filter(a->a.getDoctor().getEmail().equals(email)).map(a->a.getDoctor().getId()).findFirst().orElse(null);
        List<Appointment> found=doctorId==null?List.of():(patientName==null||patientName.isBlank()||"all".equalsIgnoreCase(patientName)?appointments.findByDoctorIdAndAppointmentTimeBetween(doctorId,date.atStartOfDay(),date.plusDays(1).atStartOfDay().minusNanos(1)):appointments.findByDoctorIdAndPatientNameAndAppointmentTimeBetween(doctorId,patientName,date.atStartOfDay(),date.plusDays(1).atStartOfDay().minusNanos(1)));
        Map<String,Object> body=new LinkedHashMap<>();body.put("appointments",found.stream().map(AppointmentDTO::new).toList());return body;
    }
    @Transactional public void changeStatus(Long id,int status){appointments.updateStatus(status,id);}
}
