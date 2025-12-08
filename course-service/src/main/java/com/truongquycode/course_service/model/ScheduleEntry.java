package com.truongquycode.course_service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "schedule_entries")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int dayOfWeek;
    private String periods;
    private String weeks;
    private String room;

    @ManyToOne
    @JoinColumn(name = "section_id")
    @JsonIgnore // Tránh lặp vô hạn khi serialize
    private CourseSection courseSection;
}
