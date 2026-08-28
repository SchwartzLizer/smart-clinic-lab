package com.project.back_end.services;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.project.back_end.DTO.Login;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
@Service
public class DoctorService {
    private final DoctorRepository doctors;private final AppointmentRepository appointments;private final TokenService tokens;
    public DoctorService(DoctorRepository doctors,AppointmentRepository appointments,TokenService tokens){this.doctors=doctors;this.appointments=appointments;this.tokens=tokens;}
    @Transactional(readOnly=true)
    public List<String> getDoctorAvailability(Long doctorId,LocalDate date){
        Doctor doctor=doctors.findById(doctorId).orElse(null);if(doctor==null||doctor.getAvailableTimes()==null)return List.of();
        List<Appointment> booked=appointments.findByDoctorIdAndAppointmentTimeBetween(doctorId,date.atStartOfDay(),date.plusDays(1).atStartOfDay().minusNanos(1));
        List<LocalTime> times=booked.stream().map(a->a.getAppointmentTime().toLocalTime()).toList();
        return doctor.getAvailableTimes().stream().filter(slot->!times.contains(slotStart(slot))).toList();
    }
    private LocalTime slotStart(String slot){return LocalTime.parse(slot.split("-")[0].trim());}
    @Transactional public int saveDoctor(Doctor d){try{if(doctors.findByEmail(d.getEmail()).isPresent())return -1;doctors.save(d);return 1;}catch(Exception e){return 0;}}
    @Transactional public int updateDoctor(Doctor d){try{if(d.getId()==null||!doctors.existsById(d.getId()))return -1;doctors.save(d);return 1;}catch(Exception e){return 0;}}
    @Transactional(readOnly=true) public List<Doctor> getDoctors(){return doctors.findAll();}
    @Transactional public int deleteDoctor(long id){try{if(!doctors.existsById(id))return -1;appointments.deleteAllByDoctorId(id);doctors.deleteById(id);return 1;}catch(Exception e){return 0;}}
    public ResponseEntity<Map<String,String>> validateDoctor(Login login){
        Doctor d=doctors.findByEmail(login.getIdentifier()).orElse(null);
        if(d==null||!d.getPassword().equals(login.getPassword()))return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","Invalid credentials"));
        return ResponseEntity.ok(Map.of("token",tokens.generateToken(d.getEmail()),"message","Login successful","id",d.getId().toString()));
    }
    @Transactional(readOnly=true) public Map<String,Object> findDoctorByName(String name){return Map.of("doctors",doctors.findByNameLike(name));}
    @Transactional(readOnly=true) public Map<String,Object> filterDoctors(String name,String time,String specialty){
        List<Doctor> result;boolean n=present(name),s=present(specialty);
        if(n&&s)result=doctors.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(name,specialty);else if(n)result=doctors.findByNameLike(name);else if(s)result=doctors.findBySpecialtyIgnoreCase(specialty);else result=doctors.findAll();
        if(present(time))result=filterDoctorByTime(result,time);Map<String,Object> body=new LinkedHashMap<>();body.put("doctors",result);return body;
    }
    public List<Doctor> filterDoctorByTime(List<Doctor> source,String amOrPm){boolean pm="PM".equalsIgnoreCase(amOrPm);List<Doctor> result=new ArrayList<>();for(Doctor d:source)if(d.getAvailableTimes()!=null&&d.getAvailableTimes().stream().anyMatch(x->(slotStart(x).getHour()>=12)==pm))result.add(d);return result;}
    private boolean present(String value){return value!=null&&!value.isBlank()&&!"null".equalsIgnoreCase(value)&&!"all".equalsIgnoreCase(value);}
}
