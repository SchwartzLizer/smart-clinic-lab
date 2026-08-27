package com.project.back_end.services;
import java.time.LocalTime;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.project.back_end.DTO.Login;
import com.project.back_end.models.Admin;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
@org.springframework.stereotype.Service
public class Service {
    private final TokenService tokens;private final AdminRepository admins;private final DoctorRepository doctors;private final PatientRepository patients;private final DoctorService doctorService;private final PatientService patientService;
    public Service(TokenService tokens,AdminRepository admins,DoctorRepository doctors,PatientRepository patients,DoctorService doctorService,PatientService patientService){this.tokens=tokens;this.admins=admins;this.doctors=doctors;this.patients=patients;this.doctorService=doctorService;this.patientService=patientService;}
    public ResponseEntity<Map<String,String>> validateToken(String token,String user){return tokens.validateToken(token,user)?null:ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","Invalid or expired token"));}
    public ResponseEntity<Map<String,String>> validateAdmin(Admin login){
        Admin admin=admins.findByUsername(login.getUsername());if(admin==null||!admin.getPassword().equals(login.getPassword()))return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","Invalid credentials"));
        return ResponseEntity.ok(Map.of("token",tokens.generateToken(admin.getUsername()),"message","Login successful"));
    }
    public Map<String,Object> filterDoctor(String name,String time,String specialty){return doctorService.filterDoctors(name,time,specialty);}
    public int validateAppointment(Appointment appointment){
        if(appointment.getDoctor()==null||appointment.getDoctor().getId()==null||!doctors.existsById(appointment.getDoctor().getId()))return -1;
        LocalTime requested=appointment.getAppointmentTime().toLocalTime();
        return doctorService.getDoctorAvailability(appointment.getDoctor().getId(),appointment.getAppointmentTime().toLocalDate()).stream().map(s->LocalTime.parse(s.split("-")[0].trim())).anyMatch(requested::equals)?1:0;
    }
    public boolean validatePatient(Patient patient){return patients.findByEmailOrPhone(patient.getEmail(),patient.getPhone())==null;}
    public ResponseEntity<Map<String,String>> validatePatientLogin(Login login){
        Patient patient=patients.findByEmail(login.getIdentifier());if(patient==null||!patient.getPassword().equals(login.getPassword()))return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","Invalid credentials"));
        return ResponseEntity.ok(Map.of("token",tokens.generateToken(patient.getEmail()),"message","Login successful","id",patient.getId().toString()));
    }
    public Map<String,Object> filterPatient(String condition,String name,String token){
        Patient patient=patients.findByEmail(tokens.extractIdentifier(token));if(patient==null)return Map.of("appointments",java.util.List.of());
        boolean c=condition!=null&&!condition.isBlank()&&!"all".equalsIgnoreCase(condition),n=name!=null&&!name.isBlank()&&!"all".equalsIgnoreCase(name);
        if(c&&n)return patientService.filterByDoctorAndCondition(patient.getId(),name,condition);if(c)return patientService.filterByCondition(patient.getId(),condition);if(n)return patientService.filterByDoctor(patient.getId(),name);return patientService.getPatientAppointment(patient.getId());
    }
}
