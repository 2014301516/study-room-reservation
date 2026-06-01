package com.studyroom.repository;

import com.studyroom.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByStudentId(String studentId);
    boolean existsByUsername(String username);
    boolean existsByStudentId(String studentId);
    long countByRole(String role);
}
