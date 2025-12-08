package com.truongquycode.common.events;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequestEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private String enrollmentId;
    private String studentId;
    private String courseSectionId;
    private long timestamp;
}