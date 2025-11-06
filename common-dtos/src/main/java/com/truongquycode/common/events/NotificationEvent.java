package com.truongquycode.common.events;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String studentId;
    private String message;
    private String enrollmentId; // optional, để frontend/backend liên kết
    private long timestamp;
}