package com.truongquycode.common.events;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Delta event produced by CourseService Streams (or RegistrationProcessor).
 * key = sectionId_shard
 * value.delta = +1 (register) or -1 (cancel)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SectionSlotChangeEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private String sectionId;
    private int delta;              // +1 or -1 (or other integer delta)
    private String enrollmentId;    // correlation id (UUID string)
    private String studentId;
    private long timestamp;
}
