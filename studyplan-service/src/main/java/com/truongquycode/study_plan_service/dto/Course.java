package com.truongquycode.study_plan_service.dto;

import lombok.Data;
import java.util.List;

@Data
public class Course {
    private String courseId;
    private String name;
    private int credits;
    private List<CourseSection> courseSections;
}