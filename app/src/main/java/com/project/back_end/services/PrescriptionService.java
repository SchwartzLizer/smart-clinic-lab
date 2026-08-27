package com.project.back_end.services;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.project.back_end.models.Prescription;
import com.project.back_end.repo.PrescriptionRepository;
@Service
public class PrescriptionService {
    private final PrescriptionRepository prescriptions;
    public PrescriptionService(PrescriptionRepository prescriptions){this.prescriptions=prescriptions;}
    public ResponseEntity<Map<String,String>> savePrescription(Prescription prescription){
        try{if(!prescriptions.findByAppointmentId(prescription.getAppointmentId()).isEmpty())return ResponseEntity.badRequest().body(Map.of("message","Prescription already exists"));
            prescriptions.save(prescription);return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message","Prescription saved"));
        }catch(Exception e){return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message","Unable to save prescription"));}
    }
    public ResponseEntity<Map<String,Object>> getPrescription(Long appointmentId){
        try{List<Prescription> found=prescriptions.findByAppointmentId(appointmentId);Map<String,Object> body=new LinkedHashMap<>();body.put("prescription",found);return ResponseEntity.ok(body);
        }catch(Exception e){return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message","Unable to retrieve prescription"));}
    }
}
