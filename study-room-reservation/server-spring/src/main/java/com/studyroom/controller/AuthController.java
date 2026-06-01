package com.studyroom.controller;

import com.studyroom.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.register(body));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.login(body));
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> profile(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        return ResponseEntity.ok(authService.getProfile(userId));
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(Principal principal, @RequestBody Map<String, String> body) {
        Long userId = Long.parseLong(principal.getName());
        return ResponseEntity.ok(authService.updateProfile(userId, body));
    }
}
