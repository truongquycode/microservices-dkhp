package com.truongquycode.common.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentValidatedEvent {
    private String enrollmentId;   // <-- CHUYỂN TỪ Long -> String
    private String studentId;
    private String courseSectionId;
}
