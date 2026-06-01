package com.studyroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.Seat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SeatMapper extends BaseMapper<Seat> {

    /**
     * 按状态分组统计
     */
    @Select("SELECT current_status AS status, COUNT(*) AS cnt FROM seats GROUP BY current_status")
    List<Map<String, Object>> countByStatus();
}
