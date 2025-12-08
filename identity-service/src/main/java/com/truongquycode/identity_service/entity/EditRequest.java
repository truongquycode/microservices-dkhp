package com.truongquycode.identity_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "edit_requests")
public class EditRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String studentId; // MSSV người gửi
    
    @Column(columnDefinition = "TEXT")
    private String requestDetails; // Nội dung yêu cầu

    private LocalDateTime createdAt;
    
    private String status; // PENDING, COMPLETED

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "PENDING";
    }
}