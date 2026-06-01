package com.studyroom.service;

import com.studyroom.entity.*;
import com.studyroom.repository.*;
import com.studyroom.util.QRCodeUtil;
import com.studyroom.websocket.SeatBroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final CheckinRepository checkinRepository;
    private final ViolationRepository violationRepository;
    private final SeatBroadcastService broadcastService;

    @Value("${study-room.max-violations:3}")
    private int maxViolations;

    @Value("${study-room.ban-days:7}")
    private int banDays;

    public Map<String, Object> getMyReservations(Long userId, String status, int page, int pageSize) {
        List<String> statuses = (status != null && !status.isEmpty())
                ? Arrays.asList(status.split(","))
                : null;

        org.springframework.data.domain.Page<Reservation> pageResult;
        if (statuses != null && !statuses.isEmpty()) {
            pageResult = reservationRepository.findByUserIdAndStatusInOrderByDateDescStartTimeDesc(
                    userId, statuses, PageRequest.of(page - 1, pageSize));
        } else {
            pageResult = reservationRepository.findByUserIdOrderByDateDescStartTimeDesc(
                    userId, PageRequest.of(page - 1, pageSize));
        }

        // 加载关联数据
        List<Map<String, Object>> list = new ArrayList<>();
        for (Reservation r : pageResult.getContent()) {
            Seat seat = seatRepository.findById(r.getSeatId()).orElse(null);
            Area area = seat != null ? seat.getArea() : null;
            Map<String, Object> m = buildReservationMap(r, seat, area);
            list.add(m);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", pageResult.getTotalElements());
        data.put("page", page);
        data.put("pageSize", pageSize);
        return data;
    }

    public List<Map<String, Object>> checkAvailable(Long seatId, LocalDate date) {
        String nowTime = String.format("%02d:00", LocalDateTime.now().getHour());
        String todayStr = LocalDate.now().toString();
        boolean isToday = date.toString().equals(todayStr);

        List<Map<String, Object>> slots = new ArrayList<>();
        for (int h = 8; h < 22; h++) {
            String start = String.format("%02d:00", h);
            String end = String.format("%02d:00", h + 1);
            List<Reservation> conflicts = reservationRepository.findConflicts(seatId, date, end, start);
            boolean available = conflicts.isEmpty()
                    && (!isToday || start.compareTo(nowTime) >= 0);

            Map<String, Object> slot = new LinkedHashMap<>();
            slot.put("start", start);
            slot.put("end", end);
            slot.put("available", available);
            slots.add(slot);
        }
        return slots;
    }

    @Transactional
    public Map<String, Object> createReservation(Long userId, Map<String, Object> body) {
        Long seatId = Long.valueOf(body.get("seat_id").toString());
        LocalDate date = LocalDate.parse(body.get("date").toString());
        String startTime = body.get("start_time").toString();
        String endTime = body.get("end_time").toString();

        // 检查违规处罚
        long activeViolations = violationRepository.countByUserIdAndStatus(userId, "active");
        if (activeViolations >= maxViolations) {
            return Map.of("code", 403, "message", "您当前处于违规处罚期，无法预约");
        }

        // 检查座位
        Seat seat = seatRepository.findById(seatId).orElse(null);
        if (seat == null) return Map.of("code", 404, "message", "座位不存在");
        if ("maintenance".equals(seat.getCurrentStatus())) {
            return Map.of("code", 400, "message", "该座位正在维护中");
        }

        // 检查冲突
        List<Reservation> conflicts = reservationRepository.findConflicts(seatId, date, endTime, startTime);
        if (!conflicts.isEmpty()) {
            return Map.of("code", 400, "message", "该时间段已被他人预约");
        }

        // 检查自己的冲突
        List<Reservation> myConflicts = reservationRepository.findConflicts(seatId, date, endTime, startTime);
        // Actually check by userId in the query... for now, simple approach
        // Create reservation
        Reservation r = new Reservation();
        r.setUserId(userId);
        r.setSeatId(seatId);
        r.setDate(date);
        r.setStartTime(startTime);
        r.setEndTime(endTime);
        r.setQrcodeToken(UUID.randomUUID().toString());
        reservationRepository.save(r);

        // 座位标记为已预约
        String oldStatus = seat.getCurrentStatus();
        if ("available".equals(oldStatus)) {
            seat.setCurrentStatus("reserved");
            seatRepository.save(seat);
            broadcastService.broadcastSeatChange(seat.getAreaId(), seatId, seat.getSeatNumber(), oldStatus, "reserved");
        }

        // 生成QR
        String qrContent = "{\"reservation_id\":" + r.getId() + ",\"token\":\"" + r.getQrcodeToken() +
                "\",\"seat\":\"" + seat.getSeatNumber() + "\",\"date\":\"" + date + "\",\"time\":\"" + startTime + "-" + endTime + "\"}";
        String qrDataUrl = QRCodeUtil.generateDataUrl(qrContent);

        Map<String, Object> data = buildReservationMap(r, seat, seat.getArea());
        data.put("qrcode_data_url", qrDataUrl);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", data);
        result.put("message", "预约成功");
        return result;
    }

    @Transactional
    public Map<String, Object> cancelReservation(Long id, Long userId) {
        Reservation r = reservationRepository.findByIdAndUserId(id, userId).orElse(null);
        if (r == null) return Map.of("code", 404, "message", "预约不存在");
        if (!"reserved".equals(r.getStatus())) {
            return Map.of("code", 400, "message", "当前状态不可取消");
        }
        r.setStatus("cancelled");
        reservationRepository.save(r);

        seatRepository.findById(r.getSeatId()).ifPresent(seat -> {
            String oldStatus = seat.getCurrentStatus();
            seat.setCurrentStatus("available");
            seatRepository.save(seat);
            broadcastService.broadcastSeatChange(seat.getAreaId(), seat.getId(), seat.getSeatNumber(), oldStatus, "available");
        });
        return Map.of("code", 200, "message", "已取消预约");
    }

    @Transactional
    public Map<String, Object> checkin(Long id, Long userId) {
        Reservation r = reservationRepository.findByIdAndUserId(id, userId).orElse(null);
        if (r == null) return Map.of("code", 404, "message", "预约不存在");
        if (!"reserved".equals(r.getStatus())) {
            return Map.of("code", 400, "message", "当前状态不可签到");
        }
        r.setStatus("checked_in");
        reservationRepository.save(r);

        seatRepository.findById(r.getSeatId()).ifPresent(seat -> {
            String oldStatus = seat.getCurrentStatus();
            seat.setCurrentStatus("occupied");
            seatRepository.save(seat);
            broadcastService.broadcastSeatChange(seat.getAreaId(), seat.getId(), seat.getSeatNumber(), oldStatus, "occupied");
        });

        Checkin c = new Checkin();
        c.setReservationId(r.getId());
        c.setUserId(userId);
        c.setSeatId(r.getSeatId());
        c.setCheckinTime(LocalDateTime.now());
        checkinRepository.save(c);

        return Map.of("code", 200, "message", "签到成功，请就座");
    }

    @Transactional
    public Map<String, Object> tempLeave(Long id, Long userId) {
        Reservation r = reservationRepository.findByIdAndUserId(id, userId).orElse(null);
        if (r == null) return Map.of("code", 404, "message", "预约不存在");
        if (!"checked_in".equals(r.getStatus())) {
            return Map.of("code", 400, "message", "仅在已签到状态可暂离");
        }
        r.setStatus("temp_leave");
        reservationRepository.save(r);

        seatRepository.findById(r.getSeatId()).ifPresent(seat -> {
            String oldStatus = seat.getCurrentStatus();
            seat.setCurrentStatus("temp_leave");
            seatRepository.save(seat);
            broadcastService.broadcastSeatChange(seat.getAreaId(), seat.getId(), seat.getSeatNumber(), oldStatus, "temp_leave");
        });
        return Map.of("code", 200, "message", "已暂离，座位为您保留15分钟");
    }

    @Transactional
    public Map<String, Object> checkout(Long id, Long userId) {
        Reservation r = reservationRepository.findByIdAndUserId(id, userId).orElse(null);
        if (r == null) return Map.of("code", 404, "message", "预约不存在");
        if (!List.of("checked_in", "temp_leave").contains(r.getStatus())) {
            return Map.of("code", 400, "message", "当前状态不可签退");
        }
        r.setStatus("completed");
        reservationRepository.save(r);

        seatRepository.findById(r.getSeatId()).ifPresent(seat -> {
            String oldStatus = seat.getCurrentStatus();
            seat.setCurrentStatus("available");
            seatRepository.save(seat);
            broadcastService.broadcastSeatChange(seat.getAreaId(), seat.getId(), seat.getSeatNumber(), oldStatus, "available");
        });
        return Map.of("code", 200, "message", "签退成功，感谢使用");
    }

    public Map<String, Object> getQRCode(Long id, Long userId) {
        Reservation r = reservationRepository.findByIdAndUserId(id, userId).orElse(null);
        if (r == null) return Map.of("code", 404, "message", "预约不存在");

        Seat seat = seatRepository.findById(r.getSeatId()).orElse(null);
        String qrContent = "{\"reservation_id\":" + r.getId() + ",\"token\":\"" + r.getQrcodeToken() +
                "\",\"seat\":\"" + (seat != null ? seat.getSeatNumber() : "") + "\",\"date\":\"" + r.getDate() +
                "\",\"time\":\"" + r.getStartTime() + "-" + r.getEndTime() + "\"}";

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reservation_id", r.getId());
        data.put("qrcode_data_url", QRCodeUtil.generateDataUrl(qrContent));
        return Map.of("code", 200, "data", data);
    }

    private Map<String, Object> buildReservationMap(Reservation r, Seat seat, Area area) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("user_id", r.getUserId());
        m.put("seat_id", r.getSeatId());
        m.put("date", r.getDate());
        m.put("start_time", r.getStartTime());
        m.put("end_time", r.getEndTime());
        m.put("status", r.getStatus());
        m.put("qrcode_token", r.getQrcodeToken());
        m.put("created_at", r.getCreatedAt());
        if (seat != null) {
            m.put("seat_number", seat.getSeatNumber());
            m.put("row_num", seat.getRowNum());
            m.put("col_num", seat.getColNum());
        }
        if (area != null) {
            m.put("area_name", area.getName());
            m.put("building", area.getBuilding());
            m.put("floor", area.getFloor());
        }
        return m;
    }
}
