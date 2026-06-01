package com.studyroom.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "seats")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Seat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "area_id")
    private Long areaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", insertable = false, updatable = false)
    private Area area;

    private String seatNumber;
    private Integer rowNum;
    private Integer colNum;
    private Integer hasOutlet = 0;
    private Integer hasLamp = 0;
    private Integer isWindow = 0;
    private String currentStatus = "available";
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAreaId() { return areaId; }
    public void setAreaId(Long areaId) { this.areaId = areaId; }
    public Area getArea() { return area; }
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
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
