package com.truongquycode.study_plan_service.dto;

import java.util.List;

import lombok.Data;

// Lớp này là bản sao DTO của CourseSection.java bên course-service
@Data
public class CourseSection {
    private String sectionId;
    private String groupName;
    private String instructorName;
    private int totalSlots;
    private int registeredSlots;
    private String schedule; 
    private List<ScheduleEntry> scheduleEntries;
}