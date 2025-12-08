package com.truongquycode.identity_service.dto;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class UserUpdateRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String gender;
    // Map chứa các custom attributes (lớp, ngành, khoa...)
    private Map<String, List<String>> attributes;
}