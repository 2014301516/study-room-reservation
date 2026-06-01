package com.studyroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyroom.entity.*;
import com.studyroom.mapper.*;
import com.studyroom.websocket.SeatBroadcastService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CheckinService {

    private final ReservationMapper reservationMapper;
    private final SeatMapper seatMapper;
    private final CheckinMapper checkinMapper;
    private final ViolationMapper violationMapper;
    private final SeatBroadcastService broadcastService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 扫码签到
     * 对应 Express checkin.js 的 POST /api/checkin/scan
     *
     * @param qrToken 二维码内容（JSON字符串，含 reservation_id 和 token）
     * @return 签到结果
     */
    @Transactional
    public Map<String, Object> scanCheckin(String qrToken) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (qrToken == null || qrToken.isBlank()) {
            result.put("code", 400);
            result.put("message", "缺少二维码信息");
            return result;
        }

        // 解析二维码 JSON
        Long reservationId;
        String token;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> qrData = objectMapper.readValue(qrToken, Map.class);
            reservationId = Long.valueOf(qrData.get("reservation_id").toString());
            token = (String) qrData.get("token");
        } catch (Exception e) {
            result.put("code", 400);
            result.put("message", "二维码格式无效");
            return result;
        }

        // 查找预约
        Reservation r = reservationMapper.selectById(reservationId);
        if (r == null || !token.equals(r.getQrcodeToken())) {
            result.put("code", 404);
            result.put("message", "无效的二维码");
            return result;
        }

        // 检查状态
        String status = r.getStatus();
        if (!"reserved".equals(status)) {
            String msg = switch (status) {
                case "completed" -> "该预约已完成";
                case "cancelled" -> "该预约已取消";
                case "checked_in", "using", "temp_leave" -> "已签到，请勿重复操作";
                default -> "当前状态不可签到";
            };
            result.put("code", 400);
            result.put("message", msg);
            return result;
        }

        // 检查签到时间（预约开始前30分钟到开始后30分钟为合理范围）
        boolean isLate = false;
        LocalDate date = r.getDate();
        String[] parts = r.getStartTime().split(":");
        LocalTime startTime = LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        LocalDateTime slotStart = LocalDateTime.of(date, startTime);
        long diffMin = java.time.Duration.between(slotStart, LocalDateTime.now()).toMinutes();

        if (diffMin > 30) {
            isLate = true;
        }

        // 更新预约状态
        r.setStatus("checked_in");
        reservationMapper.updateById(r);

        // 更新座位状态
        Seat seat = seatMapper.selectById(r.getSeatId());
        if (seat != null) {
            String oldStatus = seat.getCurrentStatus();
            seat.setCurrentStatus("occupied");
            seatMapper.updateById(seat);
            broadcastService.broadcastSeatChange(
                    seat.getAreaId(), seat.getId(), seat.getSeatNumber(), oldStatus, "occupied");
        }

        // 插入签到记录
        Checkin checkin = new Checkin();
        checkin.setReservationId(r.getId());
        checkin.setUserId(r.getUserId());
        checkin.setSeatId(r.getSeatId());
        checkin.setCheckinTime(LocalDateTime.now());
        checkinMapper.insert(checkin);

        // 迟到记录违规
        if (isLate) {
            Violation v = new Violation();
            v.setUserId(r.getUserId());
            v.setReservationId(r.getId());
            v.setType("late");
            v.setDescription(String.format("预约 %s %s 迟到签到", r.getDate(), r.getStartTime()));
            violationMapper.insert(v);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("seat_number", seat != null ? seat.getSeatNumber() : "");
        data.put("is_late", isLate);

        result.put("code", 200);
        result.put("message", "签到成功，请对号入座" + (isLate ? "（迟到已记录）" : ""));
        result.put("data", data);
        return result;
    }
}
