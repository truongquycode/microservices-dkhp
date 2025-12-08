package com.truongquycode.course_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Course {
    @Id
    private String courseId;
    private String name;
    private int credits;
    // Thêm quan hệ ngược lại để quản lý tốt hơn
    // và dùng @JsonIgnore để tránh lỗi lặp vô hạn khi chuyển sang JSON
    @OneToMany(mappedBy = "course")
    private List<CourseSection> courseSections;
}