package com.studyroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("violations")
public class Violation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long reservationId;
    private String type;
    private String description;
    private LocalDate penaltyEnd;
    private String status = "active";

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // 关联对象（非数据库字段，Service 层手动填充）
    @TableField(exist = false)
    private User user;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getPenaltyEnd() { return penaltyEnd; }
    public void setPenaltyEnd(LocalDate penaltyEnd) { this.penaltyEnd = penaltyEnd; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
