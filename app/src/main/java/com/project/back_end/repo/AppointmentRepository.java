package com.project.back_end.repo;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.project.back_end.models.Appointment;
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment,Long> {
    @Query("SELECT DISTINCT a FROM Appointment a JOIN FETCH a.doctor d LEFT JOIN FETCH d.availableTimes JOIN FETCH a.patient WHERE d.id=:doctorId AND a.appointmentTime BETWEEN :start AND :end")
    List<Appointment> findByDoctorIdAndAppointmentTimeBetween(@Param("doctorId") Long doctorId,@Param("start") LocalDateTime start,@Param("end") LocalDateTime end);
    @Query("SELECT DISTINCT a FROM Appointment a JOIN FETCH a.doctor d LEFT JOIN FETCH d.availableTimes JOIN FETCH a.patient p WHERE d.id=:doctorId AND LOWER(p.name) LIKE LOWER(CONCAT('%',:patientName,'%')) AND a.appointmentTime BETWEEN :start AND :end")
    List<Appointment> findByDoctorIdAndPatientNameAndAppointmentTimeBetween(@Param("doctorId") Long doctorId,@Param("patientName") String patientName,@Param("start") LocalDateTime start,@Param("end") LocalDateTime end);
    @Modifying @Transactional @Query("DELETE FROM Appointment a WHERE a.doctor.id=:doctorId")
    void deleteAllByDoctorId(@Param("doctorId") Long doctorId);
    List<Appointment> findByPatientId(Long patientId);
    List<Appointment> findByPatientIdAndStatusOrderByAppointmentTimeAsc(Long patientId,int status);
    @Query("SELECT a FROM Appointment a JOIN FETCH a.doctor d JOIN FETCH a.patient p WHERE LOWER(d.name) LIKE LOWER(CONCAT('%',:doctorName,'%')) AND p.id=:patientId")
    List<Appointment> filterByDoctorNameAndPatientId(@Param("doctorName") String doctorName,@Param("patientId") Long patientId);
    @Query("SELECT a FROM Appointment a JOIN FETCH a.doctor d JOIN FETCH a.patient p WHERE LOWER(d.name) LIKE LOWER(CONCAT('%',:doctorName,'%')) AND p.id=:patientId AND a.status=:status")
    List<Appointment> filterByDoctorNameAndPatientIdAndStatus(@Param("doctorName") String doctorName,@Param("patientId") Long patientId,@Param("status") int status);
    @Modifying @Transactional @Query("UPDATE Appointment a SET a.status=:status WHERE a.id=:id")
    int updateStatus(@Param("status") int status,@Param("id") Long id);
}
