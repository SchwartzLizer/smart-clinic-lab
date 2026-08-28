package com.project.back_end.services;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.project.back_end.DTO.common.PageResponse;
import com.project.back_end.DTO.doctor.DoctorCreateRequest;
import com.project.back_end.DTO.doctor.DoctorResponse;
import com.project.back_end.DTO.doctor.DoctorUpdateRequest;
import com.project.back_end.DTO.Login;
import com.project.back_end.exceptions.ResourceConflictException;
import com.project.back_end.exceptions.ResourceNotFoundException;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
@Service
public class DoctorService {
    private final DoctorRepository doctors;private final AppointmentRepository appointments;private final TokenService tokens;private final PasswordEncoder passwordEncoder;
    public DoctorService(DoctorRepository doctors,AppointmentRepository appointments,TokenService tokens,PasswordEncoder passwordEncoder){this.doctors=doctors;this.appointments=appointments;this.tokens=tokens;this.passwordEncoder=passwordEncoder;}
    @Transactional(readOnly=true)
    public List<String> getDoctorAvailability(Long doctorId,LocalDate date){
        Doctor doctor=doctors.findById(doctorId).orElse(null);if(doctor==null||doctor.getAvailableTimes()==null)return List.of();
        List<Appointment> booked=appointments.findByDoctorIdAndAppointmentTimeBetween(doctorId,date.atStartOfDay(),date.plusDays(1).atStartOfDay().minusNanos(1));
        List<LocalTime> times=booked.stream().map(a->a.getAppointmentTime().toLocalTime()).toList();
        return doctor.getAvailableTimes().stream().filter(slot->!times.contains(slotStart(slot))).toList();
    }
    private LocalTime slotStart(String slot){return LocalTime.parse(slot.split("-")[0].trim());}
    @Transactional public int saveDoctor(Doctor d){try{if(doctors.findByEmail(d.getEmail()).isPresent())return -1;d.setPassword(passwordEncoder.encode(d.getPassword()));doctors.save(d);return 1;}catch(Exception e){return 0;}}
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

    @Transactional
    public DoctorResponse createDoctor(DoctorCreateRequest request) {
        if (doctors.findByEmail(request.email()).isPresent() || doctors.existsByPhone(request.phone())) {
            throw new ResourceConflictException("Doctor email or phone already exists");
        }
        Doctor doctor = new Doctor();
        doctor.setName(request.name());
        doctor.setSpecialty(request.specialty());
        doctor.setEmail(request.email());
        doctor.setPassword(passwordEncoder.encode(request.password()));
        doctor.setPhone(request.phone());
        doctor.setAvailableTimes(request.availableTimes() == null ? List.of() : List.copyOf(request.availableTimes()));
        return new com.project.back_end.mappers.DoctorMapper().toResponse(doctors.save(doctor));
    }

    @Transactional
    public DoctorResponse updateDoctor(Long id, DoctorUpdateRequest request) {
        Doctor doctor = doctors.findById(id).orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        Optional<Doctor> byEmail = doctors.findByEmail(request.email());
        if (byEmail.isPresent() && !id.equals(byEmail.get().getId())) {
            throw new ResourceConflictException("Doctor email already exists");
        }
        doctor.setName(request.name());
        doctor.setSpecialty(request.specialty());
        doctor.setEmail(request.email());
        doctor.setPassword(passwordEncoder.encode(request.password()));
        doctor.setPhone(request.phone());
        doctor.setAvailableTimes(request.availableTimes() == null ? List.of() : List.copyOf(request.availableTimes()));
        return new com.project.back_end.mappers.DoctorMapper().toResponse(doctors.save(doctor));
    }

    @Transactional
    public void deleteDoctor(Long id) {
        if (!doctors.existsById(id)) {
            throw new ResourceNotFoundException("Doctor not found");
        }
        appointments.deleteAllByDoctorId(id);
        doctors.deleteById(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<DoctorResponse> listDoctors(String name, String specialty, String period, Pageable pageable) {
        Pageable safe = safePageable(pageable);
        List<Doctor> filtered = doctors.findAll(safe.getSort()).stream()
                .filter(doctor -> matches(doctor.getName(), name))
                .filter(doctor -> matches(doctor.getSpecialty(), specialty))
                .filter(doctor -> matchesPeriod(doctor, period))
                .toList();
        int from = Math.min((int) safe.getOffset(), filtered.size());
        int to = Math.min(from + safe.getPageSize(), filtered.size());
        List<DoctorResponse> content = filtered.subList(from, to).stream()
                .map(doctor -> new com.project.back_end.mappers.DoctorMapper().toResponse(doctor)).toList();
        int totalPages = filtered.isEmpty() ? 0 : (int) Math.ceil((double) filtered.size() / safe.getPageSize());
        return new PageResponse<>(content, safe.getPageNumber(), safe.getPageSize(), filtered.size(), totalPages);
    }

    private Pageable safePageable(Pageable pageable) {
        int page = pageable == null ? 0 : Math.max(0, pageable.getPageNumber());
        int size = pageable == null ? 20 : Math.min(Math.max(1, pageable.getPageSize()), 100);
        Sort.Order order = pageable == null ? null : pageable.getSort().stream().findFirst().orElse(null);
        String property = order != null && SetOfAllowedSorts.contains(order.getProperty()) ? order.getProperty() : "name";
        Sort.Direction direction = order == null ? Sort.Direction.ASC : order.getDirection();
        return PageRequest.of(page, size, Sort.by(direction, property));
    }

    private boolean matches(String value, String query) {
        return !present(query) || (value != null && value.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)));
    }

    private boolean matchesPeriod(Doctor doctor, String period) {
        if (!present(period)) return true;
        boolean pm = "PM".equalsIgnoreCase(period);
        return doctor.getAvailableTimes() != null && doctor.getAvailableTimes().stream().anyMatch(slot -> {
            try { return (LocalTime.parse(slot.split("-")[0].trim()).getHour() >= 12) == pm; }
            catch (RuntimeException exception) { return false; }
        });
    }

    private static final java.util.Set<String> SetOfAllowedSorts = java.util.Set.of("id", "name", "specialty");
}
