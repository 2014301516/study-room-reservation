package com.studyroom.controller;

import com.studyroom.entity.Notice;
import com.studyroom.entity.Seat;
import com.studyroom.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // 数据看板
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(Map.of("code", 200, "data", adminService.getDashboard()));
    }

    // 座位管理
    @GetMapping("/seats")
    public ResponseEntity<Map<String, Object>> seats(@RequestParam(required = false) Long area_id) {
        return ResponseEntity.ok(Map.of("code", 200, "data", adminService.getAllSeats(area_id)));
    }

    @PostMapping("/seats")
    public ResponseEntity<Map<String, Object>> addSeat(@RequestBody Map<String, Object> body) {
        Seat seat = adminService.addSeat(body);
        return ResponseEntity.ok(Map.of("code", 200, "message", "座位添加成功", "data", seat));
    }

    @PutMapping("/seats/{id}")
    public ResponseEntity<Map<String, Object>> updateSeat(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Seat seat = adminService.updateSeat(id, body);
        if (seat == null) return ResponseEntity.ok(Map.of("code", 404, "message", "座位不存在"));
        return ResponseEntity.ok(Map.of("code", 200, "message", "更新成功"));
    }

    @DeleteMapping("/seats/{id}")
    public ResponseEntity<Map<String, Object>> deleteSeat(@PathVariable Long id) {
        adminService.deleteSeat(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "座位已删除"));
    }

    // 预约管理
    @GetMapping("/reservations")
    public ResponseEntity<Map<String, Object>> reservations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int pageSize) {
        LocalDate d = date != null ? LocalDate.parse(date) : null;
        return ResponseEntity.ok(Map.of("code", 200, "data",
                adminService.getReservations(status, d, page, pageSize)));
    }

    // 用户管理
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> users(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int pageSize) {
        return ResponseEntity.ok(Map.of("code", 200, "data",
                adminService.getUsers(role, page, pageSize)));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(adminService.updateUser(id, body));
    }

    // 违规管理
    @GetMapping("/violations")
    public ResponseEntity<Map<String, Object>> violations() {
        return ResponseEntity.ok(Map.of("code", 200, "data", adminService.getViolations()));
    }

    @PostMapping("/violations")
    public ResponseEntity<Map<String, Object>> addViolation(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(adminService.addViolation(body));
    }

    @PutMapping("/violations/{id}")
    public ResponseEntity<Map<String, Object>> updateViolation(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(adminService.updateViolation(id, body));
    }

    // 公告管理
    @GetMapping("/notices")
    public ResponseEntity<Map<String, Object>> notices() {
        return ResponseEntity.ok(Map.of("code", 200, "data", adminService.getNotices()));
    }

    @PostMapping("/notices")
    public ResponseEntity<Map<String, Object>> addNotice(@RequestBody Map<String, Object> body) {
        Notice notice = adminService.addNotice(body);
        return ResponseEntity.ok(Map.of("code", 200, "message", "公告发布成功", "data", notice));
    }

    @DeleteMapping("/notices/{id}")
    public ResponseEntity<Map<String, Object>> deleteNotice(@PathVariable Long id) {
        adminService.deleteNotice(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "公告已删除"));
    }
}
