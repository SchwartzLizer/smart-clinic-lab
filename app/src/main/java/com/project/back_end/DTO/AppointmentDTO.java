package com.project.back_end.DTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import com.project.back_end.models.Appointment;
public class AppointmentDTO {
    private final Long id, doctorId, patientId;
    private final String doctorName, patientName, patientEmail, patientPhone, patientAddress;
    private final LocalDateTime appointmentTime, endTime;
    private final int status;
    private final LocalDate appointmentDate;
    private final LocalTime appointmentTimeOnly;
    public AppointmentDTO(Appointment a) {
        this(a.getId(), a.getDoctor().getId(), a.getDoctor().getName(), a.getPatient().getId(),
                a.getPatient().getName(), a.getPatient().getEmail(), a.getPatient().getPhone(),
                a.getPatient().getAddress(), a.getAppointmentTime(), a.getStatus());
    }
    public AppointmentDTO(Long id, Long doctorId, String doctorName, Long patientId, String patientName,
            String patientEmail, String patientPhone, String patientAddress, LocalDateTime appointmentTime, int status) {
        this.id=id; this.doctorId=doctorId; this.doctorName=doctorName; this.patientId=patientId;
        this.patientName=patientName; this.patientEmail=patientEmail; this.patientPhone=patientPhone;
        this.patientAddress=patientAddress; this.appointmentTime=appointmentTime; this.status=status;
        this.appointmentDate=appointmentTime.toLocalDate(); this.appointmentTimeOnly=appointmentTime.toLocalTime();
        this.endTime=appointmentTime.plusHours(1);
    }
    public Long getId(){return id;} public Long getDoctorId(){return doctorId;}
    public String getDoctorName(){return doctorName;} public Long getPatientId(){return patientId;}
    public String getPatientName(){return patientName;} public String getPatientEmail(){return patientEmail;}
    public String getPatientPhone(){return patientPhone;} public String getPatientAddress(){return patientAddress;}
    public LocalDateTime getAppointmentTime(){return appointmentTime;} public int getStatus(){return status;}
    public LocalDate getAppointmentDate(){return appointmentDate;} public LocalTime getAppointmentTimeOnly(){return appointmentTimeOnly;}
    public LocalDateTime getEndTime(){return endTime;}
}
