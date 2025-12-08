package com.truongquycode.identity_service.dto;

import lombok.Data;

@Data
public class PasswordResetRequest {
    private String type; // "password"
    private String value;
    private boolean temporary;
}