package com.studyroom.repository;

import com.studyroom.entity.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Page<Reservation> findByUserIdOrderByDateDescStartTimeDesc(Long userId, Pageable pageable);

    Page<Reservation> findByUserIdAndStatusInOrderByDateDescStartTimeDesc(
            Long userId, List<String> statuses, Pageable pageable);

    @Query("SELECT r FROM Reservation r WHERE r.seatId = ?1 AND r.date = ?2 " +
           "AND r.status NOT IN ('cancelled','completed','absent') " +
           "AND r.startTime < ?3 AND r.endTime > ?4")
    List<Reservation> findConflicts(Long seatId, LocalDate date, String endTime, String startTime);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.seat s JOIN FETCH s.area " +
           "WHERE r.userId = ?1 ORDER BY r.date DESC, r.startTime DESC")
    List<Reservation> findMyReservations(Long userId);

    Optional<Reservation> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT r.date, COUNT(r) FROM Reservation r " +
           "WHERE r.date >= ?1 GROUP BY r.date ORDER BY r.date")
    List<Object[]> countByDateAfter(LocalDate date);

    @Query("SELECT r.startTime, COUNT(r) FROM Reservation r " +
           "WHERE r.date = ?1 GROUP BY r.startTime ORDER BY r.startTime")
    List<Object[]> countByDateGroupByStartTime(LocalDate date);

    long countByDateAndStatusIn(LocalDate date, List<String> statuses);

    long countByDateAndStatus(LocalDate date, String status);

    long countByDate(LocalDate date);

    Page<Reservation> findByStatus(String status, Pageable pageable);

    Page<Reservation> findByDate(LocalDate date, Pageable pageable);

    @Query("SELECT r FROM Reservation r WHERE (:status IS NULL OR r.status = :status) AND (:date IS NULL OR r.date = :date)")
    Page<Reservation> findByStatusAndDate(@Param("status") String status, @Param("date") LocalDate date, Pageable pageable);
}
