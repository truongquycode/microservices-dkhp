package com.truongquycode.study_plan_service.dto;

import lombok.Data;

@Data
public class AddCoursePlanDto {
    private String courseId;
    private int academicYear;
    private int semester;
}