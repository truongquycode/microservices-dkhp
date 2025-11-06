package com.truongquycode.student_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "students")
@Data
public class Student {
    @Id
    private String studentId; // Dùng ID sinh viên làm khóa chính
    private String name;
    private String email;
    private boolean active; // Ví dụ: sinh viên còn học hay không
}