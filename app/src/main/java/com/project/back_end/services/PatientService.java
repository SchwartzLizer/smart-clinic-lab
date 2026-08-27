package com.project.back_end.services;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.PatientRepository;
@Service
public class PatientService {
    private final PatientRepository patients;private final AppointmentRepository appointments;private final TokenService tokens;
    public PatientService(PatientRepository patients,AppointmentRepository appointments,TokenService tokens){this.patients=patients;this.appointments=appointments;this.tokens=tokens;}
    @Transactional public int createPatient(Patient patient){try{patients.save(patient);return 1;}catch(Exception e){return 0;}}
    @Transactional(readOnly=true) public Map<String,Object> getPatientAppointment(Long id){return appointmentBody(appointments.findByPatientId(id));}
    @Transactional(readOnly=true) public Map<String,Object> filterByCondition(Long id,String condition){return appointmentBody(appointments.findByPatientIdAndStatusOrderByAppointmentTimeAsc(id,status(condition)));}
    @Transactional(readOnly=true) public Map<String,Object> filterByDoctor(Long id,String name){return appointmentBody(appointments.filterByDoctorNameAndPatientId(name,id));}
    @Transactional(readOnly=true) public Map<String,Object> filterByDoctorAndCondition(Long id,String name,String condition){return appointmentBody(appointments.filterByDoctorNameAndPatientIdAndStatus(name,id,status(condition)));}
    public ResponseEntity<Map<String,Object>> getPatientDetails(String token){
        Patient patient=patients.findByEmail(tokens.extractIdentifier(token));Map<String,Object> body=new LinkedHashMap<>();body.put("patient",patient);return ResponseEntity.ok(body);
    }
    private int status(String condition){return "past".equalsIgnoreCase(condition)||"completed".equalsIgnoreCase(condition)?1:0;}
    private Map<String,Object> appointmentBody(List<Appointment> list){Map<String,Object> body=new LinkedHashMap<>();body.put("appointments",list.stream().map(AppointmentDTO::new).toList());return body;}
}
