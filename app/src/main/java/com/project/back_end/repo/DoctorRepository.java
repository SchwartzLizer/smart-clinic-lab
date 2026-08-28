package com.project.back_end.repo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.project.back_end.models.Doctor;
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByEmail(String email);
    @Query("SELECT DISTINCT d FROM Doctor d LEFT JOIN FETCH d.availableTimes WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Doctor> findByNameLike(@Param("name") String name);
    @Query("SELECT DISTINCT d FROM Doctor d LEFT JOIN FETCH d.availableTimes WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%')) AND LOWER(d.specialty)=LOWER(:specialty)")
    List<Doctor> findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(@Param("name") String name,@Param("specialty") String specialty);
    List<Doctor> findBySpecialtyIgnoreCase(String specialty);
}
