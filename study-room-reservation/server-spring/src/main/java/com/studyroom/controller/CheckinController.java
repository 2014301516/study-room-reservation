package com.studyroom.controller;

import com.studyroom.service.CheckinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;

    /**
     * 扫码签到
     * 前端扫描二维码后，将二维码中的 JSON 字符串作为 token 参数提交
     */
    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> scan(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        return ResponseEntity.ok(checkinService.scanCheckin(token));
    }
}
