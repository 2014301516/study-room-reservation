package com.studyroom.repository;

import com.studyroom.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByAreaIdOrderByRowNumAscColNumAsc(Long areaId);

    @Query("SELECT s.currentStatus, COUNT(s) FROM Seat s GROUP BY s.currentStatus")
    List<Object[]> countByStatus();

    long countByAreaId(Long areaId);

    @Query("SELECT COUNT(s) FROM Seat s WHERE s.currentStatus IN ('reserved','occupied','temp_leave')")
    long countUsed();
}
