package com.truongquycode.notification_service.listener;

import com.truongquycode.common.events.RegistrationResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
// (Giả sử bạn có một service để gọi Student-Service)
// import com.truongquycode.notification_service.service.StudentServiceClient; 

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    // (Bạn có thể inject một service để lấy email từ studentId)
    // private final StudentServiceClient studentServiceClient;

    @KafkaListener(topics = "registration_results", groupId = "notification-group")
    public void handleRegistrationResult(RegistrationResultEvent event) {
        log.info(
            "NOTIFICATION-SERVICE: Nhận được kết quả cho Sinh viên [{}], Enrollment ID: {}. Trạng thái: {}. Lý do: {}", 
            event.getStudentId(),
            event.getEnrollmentId(), 
            event.getStatus(),
            event.getReason()
        );

        // --- Logic gửi thông báo ---
        try {
            // 1. (Tùy chọn) Lấy email từ studentId
            // Giả lập: String email = studentServiceClient.getEmailFromStudentId(event.getStudentId());
            String email = event.getStudentId() + "@student.ctu.edu.vn"; // Giả lập

            // 2. Gửi email
            if ("CONFIRMED".equals(event.getStatus())) {
                log.info("Đang gửi email THÀNH CÔNG đến [{}]: {}", email, event.getReason());
                // TODO: Gọi service gửi email thật
                // emailService.sendEmail(email, "Kết quả đăng ký học phần", "Chúc mừng! " + event.getReason());
            } else { // FAILED
                log.info("Đang gửi email THẤT BẠI đến [{}]: {}", email, event.getReason());
                // TODO: Gọi service gửi email thật
                // emailService.sendEmail(email, "Kết quả đăng ký học phần", "Rất tiếc! " + event.getReason());
            }

        } catch (Exception e) {
            log.error("NOTIFICATION-SERVICE: Gửi thông báo thất bại cho Enrollment ID: {}", event.getEnrollmentId(), e);
            // (Không ném lỗi, vì đây là consumer)
        }
    }
}
