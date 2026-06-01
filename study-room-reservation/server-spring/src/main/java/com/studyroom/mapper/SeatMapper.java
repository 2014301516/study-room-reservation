package com.studyroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.Seat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SeatMapper extends BaseMapper<Seat> {

    /**
     * 按状态分组统计
     */
    @Select("SELECT current_status, COUNT(*) FROM seats GROUP BY current_status")
    List<Object[]> countByStatus();
}
