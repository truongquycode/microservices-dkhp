package com.truongquycode.registration_service.dto;

import lombok.Data;

@Data
public class RegistrationRequestDto {
    // Client chỉ cần gửi lên mã lớp học phần
    private String courseSectionId;
}