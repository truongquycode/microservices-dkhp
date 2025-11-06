package com.truongquycode.registration_service.model;

import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import com.truongquycode.common.events.EnrollmentStatus;
import jakarta.persistence.*;
import lombok.Data;

@Entity
// 1. XÓA BỎ UNIQUECONSTRAINT KHỎI ĐÂY
@Table(name = "enrollments")
@Data
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, updatable = false)
    private String eventId; 
    
    @Column(nullable = false)
    private String studentId;
    
    @Column(nullable = false)
    private String courseSectionId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status;
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    // 2. NÊN THÊM CỘT NÀY ĐỂ LƯU LÝ DO FAILED
    private String reason;
}