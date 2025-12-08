package com.truongquycode.registration_service.service;

import java.util.List;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.truongquycode.common.events.RegistrationRequestEvent;
import com.truongquycode.registration_service.config.TopicConstants;
import com.truongquycode.registration_service.dto.RegistrationRequestDto;
import com.truongquycode.registration_service.model.Enrollment;
import com.truongquycode.registration_service.repository.EnrollmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EnrollmentRepository enrollmentRepository;

    public String requestRegistration(RegistrationRequestDto dto, String studentId) {
        log.info("Tiếp nhận yêu cầu đăng ký: SV={} Lớp={}", studentId, dto.getCourseSectionId());

        String enrollmentId = UUID.randomUUID().toString();

        RegistrationRequestEvent event = new RegistrationRequestEvent(
                enrollmentId,
                studentId,
                dto.getCourseSectionId(),
                System.currentTimeMillis()
        );

        kafkaTemplate.send(TopicConstants.REGISTRATION_REQUESTS, event.getCourseSectionId(), event);
        return enrollmentId;
    }
    
    public String cancelRegistration(RegistrationRequestDto dto, String studentId) {
        log.info("Tiếp nhận yêu cầu HỦY: SV={} Lớp={}", studentId, dto.getCourseSectionId());

        String enrollmentId = UUID.randomUUID().toString();

        RegistrationRequestEvent event = new RegistrationRequestEvent(
                enrollmentId,
                studentId,
                dto.getCourseSectionId(),
                System.currentTimeMillis()
        );

        // Gửi vào topic request hủy
        kafkaTemplate.send("registration_cancels", event.getCourseSectionId(), event);
        
        return enrollmentId;
    }
    
    public Enrollment getRegistrationStatus(Long enrollmentId) {
        return enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đăng ký với ID: " + enrollmentId));
    }
    
    public Enrollment getRegistrationStatusByEventId(String eventId) {
         return enrollmentRepository.findByEventId(eventId)
                 .orElseThrow(() -> new RuntimeException("Không tìm thấy đăng ký với EventID: " + eventId));
    }

    // --- MỚI: Lấy danh sách lớp đã đăng ký của User ---
    public List<Enrollment> getMyEnrollments(String studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }
}