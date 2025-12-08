package com.truongquycode.study_plan_service.dto;

import lombok.Data;

@Data
public class ScheduleEntry {
    private Long id;
    private Integer dayOfWeek; // Thứ
    private String periods;    // Tiết
    private String room;       // Phòng
    private String weeks;      // Tuần học
}