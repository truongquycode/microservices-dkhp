package com.truongquycode.study_plan_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "plan_entries", 
    uniqueConstraints = {
        // Ràng buộc UNIQUE mới: Chỉ cho phép 1 môn / 1 sinh viên
        @UniqueConstraint(
            name = "UK_student_course",
            columnNames = {"student_id", "course_id"} // Chỉ 2 cột
        )
    },
    indexes = {
        @Index(name = "idx_plan_student", columnList = "student_id"),
        @Index(name = "idx_plan_semester", columnList = "student_id, academic_year, semester")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private String studentId;

    @Column(name = "course_id", nullable = false, updatable = false)
    private String courseId;

    @Column(nullable = false)
    private int credits;

    @Column(name = "academic_year", nullable = false)
    private int academicYear;

    @Column(nullable = false)
    private int semester;
}