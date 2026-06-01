package com.studyroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyroom.entity.Area;
import com.studyroom.entity.Seat;
import com.studyroom.mapper.AreaMapper;
import com.studyroom.mapper.SeatMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final AreaMapper areaMapper;
    private final SeatMapper seatMapper;

    public List<Area> getActiveAreas() {
        return areaMapper.selectList(
                new LambdaQueryWrapper<Area>()
                        .eq(Area::getStatus, "active")
                        .orderByAsc(Area::getSortOrder));
    }

    public Map<String, Object> getAreaWithSeats(Long areaId) {
        Area area = areaMapper.selectById(areaId);
        if (area == null) return null;

        List<Seat> seats = seatMapper.selectList(
                new LambdaQueryWrapper<Seat>()
                        .eq(Seat::getAreaId, areaId)
                        .orderByAsc(Seat::getRowNum, Seat::getColNum));

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
        return seatMapper.selectById(id);
    }

    public Map<String, Object> getOverview() {
        long total = seatMapper.selectCount(null);

        List<Map<String, Object>> byStatus = seatMapper.countByStatus();
        List<Area> areas = areaMapper.selectList(
                new LambdaQueryWrapper<Area>().eq(Area::getStatus, "active").orderByAsc(Area::getSortOrder));

        Map<String, Long> statusMap = new LinkedHashMap<>();
        for (Map<String, Object> row : byStatus) {
            statusMap.put((String) row.get("status"), ((Number) row.get("cnt")).longValue());
        }

        // 统计已使用座位数量
        List<String> usedStatuses = Arrays.asList("reserved", "occupied", "temp_leave");
        long usedCount = 0;
        for (Map.Entry<String, Long> e : statusMap.entrySet()) {
            if (usedStatuses.contains(e.getKey())) usedCount += e.getValue();
        }

        List<Map<String, Object>> byArea = new ArrayList<>();
        for (Area area : areas) {
            long areaTotal = seatMapper.selectCount(
                    new LambdaQueryWrapper<Seat>().eq(Seat::getAreaId, area.getId()));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", area.getName());
            m.put("building", area.getBuilding());
            m.put("floor", area.getFloor());
            m.put("total", areaTotal);
            m.put("used", seatMapper.selectCount(
                    new LambdaQueryWrapper<Seat>()
                            .eq(Seat::getAreaId, area.getId())
                            .in(Seat::getCurrentStatus, usedStatuses)));
            byArea.add(m);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", total);
        data.put("byStatus", statusMap);
        data.put("byArea", byArea);
        return data;
    }
}
