package com.project.back_end.repo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.project.back_end.models.Admin;
@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> { Optional<Admin> findByUsername(String username); }
