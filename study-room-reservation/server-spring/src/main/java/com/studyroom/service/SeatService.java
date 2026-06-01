package com.studyroom.service;

import com.studyroom.entity.Area;
import com.studyroom.entity.Seat;
import com.studyroom.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final AreaRepository areaRepository;
    private final SeatRepository seatRepository;

    public List<Area> getActiveAreas() {
        return areaRepository.findByStatusOrderBySortOrderAsc("active");
    }

    public Map<String, Object> getAreaWithSeats(Long areaId) {
        Area area = areaRepository.findById(areaId).orElse(null);
        if (area == null) return null;

        List<Seat> seats = seatRepository.findByAreaIdOrderByRowNumAscColNumAsc(areaId);

        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", (long) seats.size());
        stats.put("available", seats.stream().filter(s -> "available".equals(s.getCurrentStatus())).count());
        stats.put("reserved", seats.stream().filter(s -> "reserved".equals(s.getCurrentStatus())).count());
        stats.put("occupied", seats.stream().filter(s -> "occupied".equals(s.getCurrentStatus())).count());
        stats.put("temp_leave", seats.stream().filter(s -> "temp_leave".equals(s.getCurrentStatus())).count());
        stats.put("maintenance", seats.stream().filter(s -> "maintenance".equals(s.getCurrentStatus())).count());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("area", area);
        data.put("seats", seats);
        data.put("stats", stats);
        return data;
    }

    public Seat getSeatById(Long id) {
        return seatRepository.findById(id).orElse(null);
    }

    public Map<String, Object> getOverview() {
        long total = seatRepository.count();
        List<Object[]> byStatus = seatRepository.countByStatus();
        List<Area> areas = areaRepository.findByStatusOrderBySortOrderAsc("active");

        Map<String, Long> statusMap = new LinkedHashMap<>();
        for (Object[] row : byStatus) {
            statusMap.put((String) row[0], (Long) row[1]);
        }

        List<Map<String, Object>> byArea = new ArrayList<>();
        for (Area area : areas) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", area.getName());
            m.put("building", area.getBuilding());
            m.put("floor", area.getFloor());
            m.put("total", seatRepository.countByAreaId(area.getId()));
            byArea.add(m);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", total);
        data.put("byStatus", statusMap);
        data.put("byArea", byArea);
        return data;
    }
}
