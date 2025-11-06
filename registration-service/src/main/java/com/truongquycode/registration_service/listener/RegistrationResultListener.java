package com.truongquycode.registration_service.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.truongquycode.common.events.RegistrationResultEvent;
import com.truongquycode.registration_service.model.Enrollment;
import com.truongquycode.registration_service.repository.EnrollmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationResultListener {

    private final EnrollmentRepository enrollmentRepository;

    /**
     * Lắng nghe kết quả cuối cùng từ topic 'registration_results'
     * (do course-service hoặc các service khác gửi về)
     */
 // <-- THÊM THUỘC TÍNH 'properties' VÀO ĐÂY
    @KafkaListener(
        topics = "registration_results", 
        groupId = "registration-group-result",
        properties = {
            "spring.json.value.default.type=com.truongquycode.common.events.RegistrationResultEvent"
        }
    )
    public void handleRegistrationResult(RegistrationResultEvent event) {
        
        long enrollmentId = Long.parseLong(event.getEnrollmentId());
        log.info("REGISTRATION_SERVICE: Nhận được kết quả cho enrollment [ID={}] -> {}, Lý do: {}", 
            enrollmentId, event.getStatus(), event.getReason());

        try {
            Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy enrollment với ID: " + enrollmentId));

            // Cập nhật trạng thái cuối cùng (CONFIRMED hoặc FAILED) từ event
            enrollment.setStatus(event.getStatus());
            enrollmentRepository.save(enrollment);

            log.info("REGISTRATION_SERVICE: Đã cập nhật trạng thái cho enrollment [ID={}] thành {}", 
                enrollmentId, event.getStatus());

        } catch (Exception e) {
            log.error("REGISTRATION_SERVICE: Lỗi khi cập nhật kết quả cho enrollment [ID={}]", enrollmentId, e);
        }
    }
}
