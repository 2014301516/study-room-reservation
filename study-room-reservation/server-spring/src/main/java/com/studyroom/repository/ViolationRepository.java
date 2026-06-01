package com.studyroom.repository;

import com.studyroom.entity.Violation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ViolationRepository extends JpaRepository<Violation, Long> {
    long countByUserIdAndStatus(Long userId, String status);
    long countByStatus(String status);
    List<Violation> findByUserIdOrderByCreatedAtDesc(Long userId);
}
