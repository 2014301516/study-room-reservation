package com.studyroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("seats")
public class Seat {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long areaId;
    private String seatNumber;
    private Integer rowNum;
    private Integer colNum;
    private Integer hasOutlet = 0;
    private Integer hasLamp = 0;
    private Integer isWindow = 0;
    private String currentStatus = "available";

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // 关联对象（非数据库字段，Service 层手动填充）
    @TableField(exist = false)
    private Area area;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAreaId() { return areaId; }
    public void setAreaId(Long areaId) { this.areaId = areaId; }
    public Area getArea() { return area; }
    public void setArea(Area area) { this.area = area; }
    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
    public Integer getRowNum() { return rowNum; }
    public void setRowNum(Integer rowNum) { this.rowNum = rowNum; }
    public Integer getColNum() { return colNum; }
    public void setColNum(Integer colNum) { this.colNum = colNum; }
    public Integer getHasOutlet() { return hasOutlet; }
    public void setHasOutlet(Integer hasOutlet) { this.hasOutlet = hasOutlet; }
    public Integer getHasLamp() { return hasLamp; }
    public void setHasLamp(Integer hasLamp) { this.hasLamp = hasLamp; }
    public Integer getIsWindow() { return isWindow; }
    public void setIsWindow(Integer isWindow) { this.isWindow = isWindow; }
    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
