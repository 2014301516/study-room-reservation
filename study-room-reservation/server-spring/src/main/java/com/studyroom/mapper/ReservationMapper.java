package com.studyroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.Reservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {

    /**
     * 查询时间段冲突的预约
     */
    @Select("SELECT * FROM reservations WHERE seat_id = #{seatId} AND date = #{date} " +
            "AND status NOT IN ('cancelled','completed','absent') " +
            "AND start_time < #{endTime} AND end_time > #{startTime}")
    List<Reservation> findConflicts(@Param("seatId") Long seatId, @Param("date") LocalDate date,
                                    @Param("endTime") String endTime, @Param("startTime") String startTime);

    /**
     * 按日期之后的预约数分组
     */
    @Select("SELECT date, COUNT(*) AS cnt FROM reservations WHERE date >= #{date} GROUP BY date ORDER BY date")
    List<Map<String, Object>> countByDateAfter(@Param("date") LocalDate date);

    /**
     * 按日期和时段分组统计
     */
    @Select("SELECT start_time, COUNT(*) AS cnt FROM reservations WHERE date = #{date} GROUP BY start_time ORDER BY start_time")
    List<Map<String, Object>> countByDateGroupByStartTime(@Param("date") LocalDate date);
}
