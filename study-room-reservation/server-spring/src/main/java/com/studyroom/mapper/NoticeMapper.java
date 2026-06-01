package com.studyroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyroom.entity.Notice;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {
}
