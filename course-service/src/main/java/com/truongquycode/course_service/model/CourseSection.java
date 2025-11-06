package com.truongquycode.course_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

//Thêm 2 annotation này của Lombok
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "course_sections")
@Getter
@Setter
@NoArgsConstructor // <-- THÊM VÀO
@AllArgsConstructor // <-- THÊM VÀO
public class CourseSection {
    @Id
    private String sectionId;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    private String schedule;
    private int totalSlots;
    private int registeredSlots;
    private Long lastUpdatedAt; // lưu timestamp của bản cập nhật
}