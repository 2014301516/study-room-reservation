package com.studyroom.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 供 Service 层调用的 WebSocket 广播工具
 * 当座位状态在业务逻辑中变更时，调用此服务推送实时更新
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeatBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 广播座位状态变更
     *
     * @param areaId     区域 ID
     * @param seatId     座位 ID
     * @param seatNumber 座位编号
     * @param oldStatus  旧状态
     * @param newStatus  新状态
     */
    public void broadcastSeatChange(Long areaId, Long seatId, String seatNumber,
                                    String oldStatus, String newStatus) {
        Map<String, Object> data = Map.of(
                "area_id", areaId,
                "seat_id", seatId,
                "seat_number", seatNumber,
                "old_status", oldStatus,
                "new_status", newStatus
        );
        messagingTemplate.convertAndSend("/topic/area/" + areaId, data);
        messagingTemplate.convertAndSend("/topic/global", data);
        log.debug("广播座位变更: area={}, seat={}, {} -> {}", areaId, seatNumber, oldStatus, newStatus);
    }
}
