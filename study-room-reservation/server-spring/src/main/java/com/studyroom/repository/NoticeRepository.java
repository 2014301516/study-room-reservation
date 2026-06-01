package com.studyroom.repository;

import com.studyroom.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByOrderByIsTopDescCreatedAtDesc();
}
