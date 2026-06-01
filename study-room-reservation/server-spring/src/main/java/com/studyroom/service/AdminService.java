package com.studyroom.service;

import com.studyroom.entity.*;
import com.studyroom.repository.*;
import com.studyroom.websocket.SeatBroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final ViolationRepository violationRepository;
    private final NoticeRepository noticeRepository;
    private final AreaRepository areaRepository;
    private final SeatBroadcastService broadcastService;

    @Value("${study-room.max-violations:3}")
    private int maxViolations;
    @Value("${study-room.ban-days:7}")
    private int banDays;

    public Map<String, Object> getDashboard() {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(6);

        // 今日统计
        long totalReservations = reservationRepository.countByDate(today);
        long activeUsers = reservationRepository.countByDateAndStatusIn(today,
                List.of("checked_in", "using", "temp_leave"));
        long noShows = reservationRepository.countByDateAndStatus(today, "absent");

        // 利用率
        long totalSeats = seatRepository.count();
        long usedSeats = seatRepository.countUsed();
        long utilization = totalSeats > 0 ? Math.round(usedSeats * 100.0 / totalSeats) : 0;

        // 区域使用
        List<Area> areas = areaRepository.findByStatusOrderBySortOrderAsc("active");
        List<Map<String, Object>> areaUsage = new ArrayList<>();
        for (Area a : areas) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", a.getName());
            m.put("building", a.getBuilding());
            m.put("floor", a.getFloor());
            m.put("total", seatRepository.countByAreaId(a.getId()));
            // simplified: count used in this area
            long used = seatRepository.findByAreaIdOrderByRowNumAscColNumAsc(a.getId())
                    .stream().filter(s -> List.of("reserved", "occupied", "temp_leave")
                            .contains(s.getCurrentStatus())).count();
            m.put("used", used);
            areaUsage.add(m);
        }

        // 趋势
        List<Object[]> trendRaw = reservationRepository.countByDateAfter(weekAgo);
        List<Map<String, Object>> trend = new ArrayList<>();
        for (Object[] row : trendRaw) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", row[0].toString());
            m.put("cnt", row[1]);
            trend.add(m);
        }

        // 时段分布
        List<Object[]> hourlyRaw = reservationRepository.countByDateGroupByStartTime(today);
        List<Map<String, Object>> hourly = new ArrayList<>();
        for (Object[] row : hourlyRaw) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("start_time", row[0].toString());
            m.put("cnt", row[1]);
            hourly.add(m);
        }

        // 违规
        long totalViolations = violationRepository.count();
        long activeViolations = violationRepository.countByStatus("active");

        Map<String, Object> todayStats = new LinkedHashMap<>();
        todayStats.put("total_reservations", totalReservations);
        todayStats.put("active_users", activeUsers);
        todayStats.put("no_shows", noShows);
        todayStats.put("utilization", utilization);

        Map<String, Object> violationStats = new LinkedHashMap<>();
        violationStats.put("total", totalViolations);
        violationStats.put("active", activeViolations);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("today", todayStats);
        data.put("areaUsage", areaUsage);
        data.put("trend", trend);
        data.put("hourly", hourly);
        data.put("violations", violationStats);
        return data;
    }

    // ====== 座位管理 ======
    public List<Seat> getAllSeats(Long areaId) {
        if (areaId != null) {
            return seatRepository.findByAreaIdOrderByRowNumAscColNumAsc(areaId);
        }
        return seatRepository.findAll();
    }

    public Seat addSeat(Map<String, Object> body) {
        Seat seat = new Seat();
        seat.setAreaId(Long.valueOf(body.get("area_id").toString()));
        seat.setSeatNumber(body.get("seat_number").toString());
        seat.setRowNum(body.containsKey("row_num") ? Integer.valueOf(body.get("row_num").toString()) : 0);
        seat.setColNum(body.containsKey("col_num") ? Integer.valueOf(body.get("col_num").toString()) : 0);
        seat.setHasOutlet(body.containsKey("has_outlet") ? Integer.valueOf(body.get("has_outlet").toString()) : 0);
        seat.setHasLamp(body.containsKey("has_lamp") ? Integer.valueOf(body.get("has_lamp").toString()) : 0);
        seat.setIsWindow(body.containsKey("is_window") ? Integer.valueOf(body.get("is_window").toString()) : 0);
        return seatRepository.save(seat);
    }

    public Seat updateSeat(Long id, Map<String, Object> body) {
        Seat seat = seatRepository.findById(id).orElse(null);
        if (seat == null) return null;
        if (body.containsKey("seat_number")) seat.setSeatNumber(body.get("seat_number").toString());
        if (body.containsKey("row_num")) seat.setRowNum(Integer.valueOf(body.get("row_num").toString()));
        if (body.containsKey("col_num")) seat.setColNum(Integer.valueOf(body.get("col_num").toString()));
        if (body.containsKey("has_outlet")) seat.setHasOutlet(Integer.valueOf(body.get("has_outlet").toString()));
        if (body.containsKey("has_lamp")) seat.setHasLamp(Integer.valueOf(body.get("has_lamp").toString()));
        if (body.containsKey("is_window")) seat.setIsWindow(Integer.valueOf(body.get("is_window").toString()));
        if (body.containsKey("current_status")) {
            String oldStatus = seat.getCurrentStatus();
            String newStatus = body.get("current_status").toString();
            seat.setCurrentStatus(newStatus);
            Seat saved = seatRepository.save(seat);
            broadcastService.broadcastSeatChange(saved.getAreaId(), saved.getId(), saved.getSeatNumber(), oldStatus, newStatus);
            return saved;
        }
        return seatRepository.save(seat);
    }

    public void deleteSeat(Long id) {
        seatRepository.deleteById(id);
    }

    // ====== 预约管理 ======
    public Map<String, Object> getReservations(String status, LocalDate date, int page, int pageSize) {
        org.springframework.data.domain.Page<Reservation> pageResult =
                reservationRepository.findByStatusAndDate(status, date, PageRequest.of(page - 1, pageSize));

        List<Map<String, Object>> list = new ArrayList<>();
        for (Reservation r : pageResult.getContent()) {
            Seat seat = seatRepository.findById(r.getSeatId()).orElse(null);
            User user = userRepository.findById(r.getUserId()).orElse(null);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("real_name", user != null ? user.getRealName() : "");
            m.put("student_id", user != null ? user.getStudentId() : "");
            m.put("seat_number", seat != null ? seat.getSeatNumber() : "");
            if (seat != null && seat.getArea() != null) {
                m.put("area_name", seat.getArea().getName());
                m.put("building", seat.getArea().getBuilding());
                m.put("floor", seat.getArea().getFloor());
            }
            m.put("date", r.getDate());
            m.put("start_time", r.getStartTime());
            m.put("end_time", r.getEndTime());
            m.put("status", r.getStatus());
            m.put("created_at", r.getCreatedAt());
            list.add(m);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", pageResult.getTotalElements());
        data.put("page", page);
        data.put("pageSize", pageSize);
        return data;
    }

    // ====== 用户管理 ======
    public Map<String, Object> getUsers(String role, int page, int pageSize) {
        org.springframework.data.domain.Page<User> pageResult;
        if (role != null && !role.isEmpty()) {
            pageResult = userRepository.findByRole(role, PageRequest.of(page - 1, pageSize));
        } else {
            pageResult = userRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page - 1, pageSize));
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (User u : pageResult.getContent()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("real_name", u.getRealName());
            m.put("student_id", u.getStudentId());
            m.put("phone", u.getPhone());
            m.put("email", u.getEmail());
            m.put("role", u.getRole());
            m.put("status", u.getStatus());
            m.put("created_at", u.getCreatedAt());
            list.add(m);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", pageResult.getTotalElements());
        data.put("page", page);
        data.put("pageSize", pageSize);
        return data;
    }

    public Map<String, Object> updateUser(Long id, Map<String, String> body) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return Map.of("code", 404, "message", "用户不存在");
        if (body.containsKey("status")) user.setStatus(body.get("status"));
        userRepository.save(user);
        return Map.of("code", 200, "message", "更新成功");
    }

    // ====== 违规管理 ======
    public List<Violation> getViolations() {
        return violationRepository.findAll();
    }

    @Transactional
    public Map<String, Object> addViolation(Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("user_id").toString());
        Violation v = new Violation();
        v.setUserId(userId);
        v.setReservationId(body.containsKey("reservation_id") ? Long.valueOf(body.get("reservation_id").toString()) : null);
        v.setType(body.get("type").toString());
        v.setDescription(body.getOrDefault("description", "").toString());
        if (body.containsKey("penalty_end") && body.get("penalty_end") != null) {
            v.setPenaltyEnd(LocalDate.parse(body.get("penalty_end").toString()));
        }
        violationRepository.save(v);

        // 自动封禁检查
        long cnt = violationRepository.countByUserIdAndStatus(userId, "active");
        if (cnt >= maxViolations) {
            userRepository.findById(userId).ifPresent(u -> {
                u.setStatus("banned");
                userRepository.save(u);
            });
        }
        return Map.of("code", 200, "message", "违规记录已添加");
    }

    public Map<String, Object> updateViolation(Long id, Map<String, String> body) {
        Violation v = violationRepository.findById(id).orElse(null);
        if (v == null) return Map.of("code", 404, "message", "违规不存在");
        if (body.containsKey("status")) v.setStatus(body.get("status"));
        violationRepository.save(v);
        return Map.of("code", 200, "message", "更新成功");
    }

    // ====== 公告管理 ======
    public List<Notice> getNotices() {
        return noticeRepository.findAllByOrderByIsTopDescCreatedAtDesc();
    }

    public Notice addNotice(Map<String, Object> body) {
        Notice n = new Notice();
        n.setTitle(body.get("title").toString());
        n.setContent(body.getOrDefault("content", "").toString());
        n.setPublisher("管理员");
        n.setIsTop(body.containsKey("is_top") ? Integer.valueOf(body.get("is_top").toString()) : 0);
        return noticeRepository.save(n);
    }

    public void deleteNotice(Long id) {
        noticeRepository.deleteById(id);
    }
}
