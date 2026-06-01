package com.studyroom.controller;

import com.studyroom.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMy(
            Principal principal,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = Long.parseLong(principal.getName());
        return ResponseEntity.ok(Map.of("code", 200, "data",
                reservationService.getMyReservations(userId, status, page, pageSize)));
    }

    @GetMapping("/check-available/{seatId}")
    public ResponseEntity<Map<String, Object>> checkAvailable(
            @PathVariable Long seatId,
            @RequestParam(required = false) String date) {
        LocalDate d = date != null ? LocalDate.parse(date) : LocalDate.now();
        return ResponseEntity.ok(Map.of("code", 200, "data",
                reservationService.checkAvailable(seatId, d)));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(Principal principal, @RequestBody Map<String, Object> body) {
        Long userId = Long.parseLong(principal.getName());
        return ResponseEntity.ok(reservationService.createReservation(userId, body));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancel(@PathVariable Long id, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        return ResponseEntity.ok(reservationService.cancelReservation(id, userId));
    }

    @PostMapping("/{id}/checkin")
    public ResponseEntity<Map<String, Object>> checkin(@PathVariable Long id, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        return ResponseEntity.ok(reservationService.checkin(id, userId));
    }

    @PutMapping("/{id}/leave")
    public ResponseEntity<Map<String, Object>> leave(@PathVariable Long id, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        return ResponseEntity.ok(reservationService.tempLeave(id, userId));
    }

    @PostMapping("/{id}/checkout")
    public ResponseEntity<Map<String, Object>> checkout(@PathVariable Long id, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        return ResponseEntity.ok(reservationService.checkout(id, userId));
    }

    @GetMapping("/{id}/qrcode")
    public ResponseEntity<Map<String, Object>> qrcode(@PathVariable Long id, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        return ResponseEntity.ok(reservationService.getQRCode(id, userId));
    }
}
