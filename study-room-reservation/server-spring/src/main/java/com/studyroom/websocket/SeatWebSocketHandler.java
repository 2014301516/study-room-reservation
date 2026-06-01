package com.studyroom.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * WebSocket 消息处理
 * 对应 Express 版 socket/index.js 的功能：
 * - 客户端加入/离开区域监控房间
 * - 座位状态变更广播
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class SeatWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 客户端请求监控某个区域的座位变化
     * 前端发送: { areaId: 1 }
     * 服务端将其订阅到 /topic/area/{areaId}
     */
    @MessageMapping("/watch-area")
    public void watchArea(Map<String, Object> payload) {
        // STOMP 客户端自行订阅 /topic/area/{areaId}
        // 此方法仅用于日志记录
        log.debug("用户订阅区域监控: {}", payload.get("areaId"));
    }

    /**
     * 座位状态变更广播
     * 前端发送: { area_id, seat_id, seat_number, old_status, new_status }
     * 广播到对应区域房间 + 全局
     */
    @MessageMapping("/seat-change")
    public void seatChange(Map<String, Object> data) {
        Object areaId = data.get("area_id");
        if (areaId != null) {
            messagingTemplate.convertAndSend("/topic/area/" + areaId, data);
        }
        messagingTemplate.convertAndSend("/topic/global", data);
        log.debug("座位状态变更: {} -> {}", data.get("seat_number"), data.get("new_status"));
    }
}
