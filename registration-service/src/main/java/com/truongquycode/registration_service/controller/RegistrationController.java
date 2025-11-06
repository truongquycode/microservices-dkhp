package com.truongquycode.registration_service.controller;

import java.util.Map; 

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.truongquycode.registration_service.dto.RegistrationRequestDto;
import com.truongquycode.registration_service.model.Enrollment;
import com.truongquycode.registration_service.service.RegistrationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> requestRegistration(
            
            @RequestBody RegistrationRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) { 

        String studentId = jwt.getSubject(); 

        String eventId = registrationService.requestRegistration(dto, studentId); 
        
        // Trả về 202 ACCEPTED (Đã chấp nhận)
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
            Map.of("message", "Yêu cầu đang được xử lý", "eventId", eventId) // <-- Trả về eventId (UUID)
        );
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Enrollment> getRegistrationStatus(@PathVariable Long id) {
        // Endpoint này vẫn giữ để dùng nội bộ hoặc khi biết DB ID
        Enrollment enrollment = registrationService.getRegistrationStatus(id);
        return ResponseEntity.ok(enrollment);
    }
    
    // -----------------------------------------------------------------
    // ** THÊM ENDPOINT MỚI NÀY VÀO **
    // Endpoint này để frontend hỏi thăm trạng thái bằng String (UUID)
    // -----------------------------------------------------------------
    @GetMapping("/event/{eventId}")
    public ResponseEntity<Enrollment> getRegistrationStatusByEventId(@PathVariable String eventId) {
        Enrollment enrollment = registrationService.getRegistrationStatusByEventId(eventId);
        return ResponseEntity.ok(enrollment);
    }
}