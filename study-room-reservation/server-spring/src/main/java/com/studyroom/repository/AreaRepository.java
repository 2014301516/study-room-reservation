package com.studyroom.repository;

import com.studyroom.entity.Area;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AreaRepository extends JpaRepository<Area, Long> {
    List<Area> findByStatusOrderBySortOrderAsc(String status);
}
