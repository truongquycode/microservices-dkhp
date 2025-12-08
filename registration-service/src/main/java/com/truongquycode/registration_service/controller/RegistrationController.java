package com.truongquycode.registration_service.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
            Map.of("message", "Yêu cầu đang được xử lý", "eventId", eventId)
        );
    }

    @PostMapping("/cancel")
    public ResponseEntity<Map<String, String>> cancelRegistration(
            @RequestBody RegistrationRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {

        String studentId = jwt.getSubject();
        String eventId = registrationService.cancelRegistration(dto, studentId);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
            Map.of("message", "Đang xử lý hủy học phần...", "eventId", eventId)
        );
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Enrollment> getRegistrationStatus(@PathVariable Long id) {
        Enrollment enrollment = registrationService.getRegistrationStatus(id);
        return ResponseEntity.ok(enrollment);
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<Enrollment> getRegistrationStatusByEventId(@PathVariable String eventId) {
        Enrollment enrollment = registrationService.getRegistrationStatusByEventId(eventId);
        return ResponseEntity.ok(enrollment);
    }

    // --- MỚI: API lấy danh sách môn đã đăng ký của tôi ---
    @GetMapping("/my-enrollments")
    public ResponseEntity<List<Enrollment>> getMyEnrollments(@AuthenticationPrincipal Jwt jwt) {
        String studentId = jwt.getSubject();
        List<Enrollment> enrollments = registrationService.getMyEnrollments(studentId);
        return ResponseEntity.ok(enrollments);
    }
}