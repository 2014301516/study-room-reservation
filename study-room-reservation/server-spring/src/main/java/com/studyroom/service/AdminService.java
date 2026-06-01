package com.studyroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.studyroom.entity.*;
import com.studyroom.mapper.*;
import com.studyroom.websocket.SeatBroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ReservationMapper reservationMapper;
    private final SeatMapper seatMapper;
    private final UserMapper userMapper;
    private final ViolationMapper violationMapper;
    private final NoticeMapper noticeMapper;
    private final AreaMapper areaMapper;
    private final SeatBroadcastService broadcastService;

    @Value("${study-room.max-violations:3}")
    private int maxViolations;
    @Value("${study-room.ban-days:7}")
    private int banDays;

    public Map<String, Object> getDashboard() {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(6);

        // 今日统计
        long totalReservations = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>().eq(Reservation::getDate, today));
        long activeUsers = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getDate, today)
                        .in(Reservation::getStatus, List.of("checked_in", "using", "temp_leave")));
        long noShows = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getDate, today)
                        .eq(Reservation::getStatus, "absent"));

        // 利用率
        long totalSeats = seatMapper.selectCount(null);
        List<String> usedStatuses = List.of("reserved", "occupied", "temp_leave");
        long usedSeats = seatMapper.selectCount(
                new LambdaQueryWrapper<Seat>().in(Seat::getCurrentStatus, usedStatuses));
        long utilization = totalSeats > 0 ? Math.round(usedSeats * 100.0 / totalSeats) : 0;

        // 区域使用
        List<Area> areas = areaMapper.selectList(
                new LambdaQueryWrapper<Area>().eq(Area::getStatus, "active").orderByAsc(Area::getSortOrder));
        List<Map<String, Object>> areaUsage = new ArrayList<>();
        for (Area a : areas) {
            long areaTotal = seatMapper.selectCount(new LambdaQueryWrapper<Seat>().eq(Seat::getAreaId, a.getId()));
            long areaUsed = seatMapper.selectCount(new LambdaQueryWrapper<Seat>()
                    .eq(Seat::getAreaId, a.getId())
                    .in(Seat::getCurrentStatus, usedStatuses));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", a.getName());
            m.put("building", a.getBuilding());
            m.put("floor", a.getFloor());
            m.put("total", areaTotal);
            m.put("used", areaUsed);
            areaUsage.add(m);
        }

        // 趋势
        List<Map<String, Object>> trendRaw = reservationMapper.countByDateAfter(weekAgo);
        List<Map<String, Object>> trend = new ArrayList<>();
        for (Map<String, Object> row : trendRaw) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", row.get("date").toString());
            m.put("cnt", ((Number) row.get("cnt")).longValue());
            trend.add(m);
        }

        // 时段分布
        List<Map<String, Object>> hourlyRaw = reservationMapper.countByDateGroupByStartTime(today);
        List<Map<String, Object>> hourly = new ArrayList<>();
        for (Map<String, Object> row : hourlyRaw) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("start_time", row.get("start_time").toString());
            m.put("cnt", ((Number) row.get("cnt")).longValue());
            hourly.add(m);
        }

        // 违规
        long totalViolations = violationMapper.selectCount(null);
        long activeViolations = violationMapper.selectCount(
                new LambdaQueryWrapper<Violation>().eq(Violation::getStatus, "active"));

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
            return seatMapper.selectList(new LambdaQueryWrapper<Seat>()
                    .eq(Seat::getAreaId, areaId)
                    .orderByAsc(Seat::getRowNum, Seat::getColNum));
        }
        return seatMapper.selectList(null);
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
        seatMapper.insert(seat);
        return seat;
    }

    public Seat updateSeat(Long id, Map<String, Object> body) {
        Seat seat = seatMapper.selectById(id);
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
            seatMapper.updateById(seat);
            broadcastService.broadcastSeatChange(seat.getAreaId(), seat.getId(), seat.getSeatNumber(), oldStatus, newStatus);
            return seat;
        }
        seatMapper.updateById(seat);
        return seat;
    }

    public void deleteSeat(Long id) {
        seatMapper.deleteById(id);
    }

    // ====== 预约管理 ======
    public Map<String, Object> getReservations(String status, LocalDate date, int page, int pageSize) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Reservation::getStatus, status);
        }
        if (date != null) {
            wrapper.eq(Reservation::getDate, date);
        }
        wrapper.orderByDesc(Reservation::getDate).orderByDesc(Reservation::getStartTime);

        Page<Reservation> pageResult = reservationMapper.selectPage(new Page<>(page, pageSize), wrapper);

        List<Map<String, Object>> list = new ArrayList<>();
        for (Reservation r : pageResult.getRecords()) {
            Seat seat = seatMapper.selectById(r.getSeatId());
            User user = userMapper.selectById(r.getUserId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("real_name", user != null ? user.getRealName() : "");
            m.put("student_id", user != null ? user.getStudentId() : "");
            m.put("seat_number", seat != null ? seat.getSeatNumber() : "");
            if (seat != null && seat.getAreaId() != null) {
                Area area = areaMapper.selectById(seat.getAreaId());
                if (area != null) {
                    m.put("area_name", area.getName());
                    m.put("building", area.getBuilding());
                    m.put("floor", area.getFloor());
                }
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
        data.put("total", pageResult.getTotal());
        data.put("page", page);
        data.put("pageSize", pageSize);
        return data;
    }

    // ====== 用户管理 ======
    public Map<String, Object> getUsers(String role, int page, int pageSize) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (role != null && !role.isEmpty()) {
            wrapper.eq(User::getRole, role);
        }
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> pageResult = userMapper.selectPage(new Page<>(page, pageSize), wrapper);

        List<Map<String, Object>> list = new ArrayList<>();
        for (User u : pageResult.getRecords()) {
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
        data.put("total", pageResult.getTotal());
        data.put("page", page);
        data.put("pageSize", pageSize);
        return data;
    }

    public Map<String, Object> updateUser(Long id, Map<String, String> body) {
        User user = userMapper.selectById(id);
        if (user == null) return Map.of("code", 404, "message", "用户不存在");
        if (body.containsKey("status")) user.setStatus(body.get("status"));
        userMapper.updateById(user);
        return Map.of("code", 200, "message", "更新成功");
    }

    // ====== 违规管理 ======
    public List<Violation> getViolations() {
        return violationMapper.selectList(null);
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
        violationMapper.insert(v);

        // 自动封禁检查
        long cnt = violationMapper.selectCount(
                new LambdaQueryWrapper<Violation>().eq(Violation::getUserId, userId).eq(Violation::getStatus, "active"));
        if (cnt >= maxViolations) {
            User user = userMapper.selectById(userId);
            if (user != null) {
                user.setStatus("banned");
                userMapper.updateById(user);
            }
        }
        return Map.of("code", 200, "message", "违规记录已添加");
    }

    public Map<String, Object> updateViolation(Long id, Map<String, String> body) {
        Violation v = violationMapper.selectById(id);
        if (v == null) return Map.of("code", 404, "message", "违规不存在");
        if (body.containsKey("status")) v.setStatus(body.get("status"));
        violationMapper.updateById(v);
        return Map.of("code", 200, "message", "更新成功");
    }

    // ====== 公告管理 ======
    public List<Notice> getNotices() {
        return noticeMapper.selectList(
                new LambdaQueryWrapper<Notice>().orderByDesc(Notice::getIsTop).orderByDesc(Notice::getCreatedAt));
    }

    public Notice addNotice(Map<String, Object> body) {
        Notice n = new Notice();
        n.setTitle(body.get("title").toString());
        n.setContent(body.getOrDefault("content", "").toString());
        n.setPublisher("管理员");
        n.setIsTop(body.containsKey("is_top") ? Integer.valueOf(body.get("is_top").toString()) : 0);
        noticeMapper.insert(n);
        return n;
    }

    public void deleteNotice(Long id) {
        noticeMapper.deleteById(id);
    }
}
