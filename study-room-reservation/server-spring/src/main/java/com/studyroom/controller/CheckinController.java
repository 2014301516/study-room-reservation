package com.studyroom.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/checkin")
public class CheckinController {

    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> scan(@RequestBody Map<String, String> body) {
        // 简化版：前端已通过 reservations/:id/checkin 接口直接签到
        return ResponseEntity.ok(Map.of("code", 200, "message", "签到接口请使用 /api/reservations/{id}/checkin"));
    }
}
