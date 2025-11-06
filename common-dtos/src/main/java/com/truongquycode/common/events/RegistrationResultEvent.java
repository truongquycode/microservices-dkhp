package com.truongquycode.common.events;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResultEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String enrollmentId;       // id của Enrollment
    private String studentId;
    private EnrollmentStatus status;   // dùng enum để tránh spelling mismatch
    private String reason;             // message mô tả lý do (vd: "Hết chỗ")
    private long timestamp;            // epoch millis, optional
}
