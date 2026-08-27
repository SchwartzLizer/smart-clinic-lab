package com.project.back_end.repo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.project.back_end.models.Patient;
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    /** Retrieves one patient by exact email address. */
    Patient findByEmail(String email);

    /** Retrieves one patient when either the email or phone number matches. */
    Patient findByEmailOrPhone(String email, String phone);
}
