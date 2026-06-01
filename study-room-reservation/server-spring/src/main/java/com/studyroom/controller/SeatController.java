package com.studyroom.controller;

import com.studyroom.entity.Seat;
import com.studyroom.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping("/areas")
    public ResponseEntity<Map<String, Object>> getAreas() {
        return ResponseEntity.ok(Map.of("code", 200, "data", seatService.getActiveAreas()));
    }

    @GetMapping("/area/{areaId}")
    public ResponseEntity<Map<String, Object>> getAreaSeats(@PathVariable Long areaId) {
        Map<String, Object> data = seatService.getAreaWithSeats(areaId);
        if (data == null) return ResponseEntity.ok(Map.of("code", 404, "message", "区域不存在"));
        return ResponseEntity.ok(Map.of("code", 200, "data", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getSeat(@PathVariable Long id) {
        Seat seat = seatService.getSeatById(id);
        if (seat == null) return ResponseEntity.ok(Map.of("code", 404, "message", "座位不存在"));
        return ResponseEntity.ok(Map.of("code", 200, "data", seat));
    }

    @GetMapping("/stats/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        return ResponseEntity.ok(Map.of("code", 200, "data", seatService.getOverview()));
    }
}
