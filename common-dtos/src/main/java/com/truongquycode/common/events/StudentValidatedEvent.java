package com.truongquycode.common.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentValidatedEvent {
    private Long enrollmentId;
    private String studentId;
    private String courseSectionId; // <- ĐÃ THAY ĐỔI
}
