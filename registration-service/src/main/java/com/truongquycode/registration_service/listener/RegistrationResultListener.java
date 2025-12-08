package com.truongquycode.registration_service.listener;

import java.util.Optional; // Nhớ import cái này

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

    @KafkaListener(
        topics = "registration_results", 
        groupId = "registration-group-result",
        properties = {
            "spring.json.value.default.type=com.truongquycode.common.events.RegistrationResultEvent"
        }
    )
    public void handleRegistrationResult(RegistrationResultEvent event) {
        
        long enrollmentId = Long.parseLong(event.getEnrollmentId());
        
        // --- SỬA ĐOẠN NÀY ---
        // Thay vì dùng .orElseThrow() gây lỗi, ta dùng Optional để kiểm tra trước
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findById(enrollmentId);

        if (enrollmentOpt.isEmpty()) {
            // Nếu không tìm thấy trong DB (do đã bị xóa tay), chỉ log warning và bỏ qua
            log.warn("REGISTRATION_SERVICE: Bỏ qua kết quả cho enrollment ID={} vì không tìm thấy trong DB (Có thể đã bị xóa)", enrollmentId);
            return; // Kết thúc hàm, Kafka sẽ coi như đã xử lý xong message này
        }

        Enrollment enrollment = enrollmentOpt.get();
        // --------------------

        try {
            // Cập nhật trạng thái
            enrollment.setStatus(event.getStatus());
            enrollmentRepository.save(enrollment);

            log.info("REGISTRATION_SERVICE: Đã cập nhật trạng thái cho enrollment [ID={}] thành {}", 
                enrollmentId, event.getStatus());

        } catch (Exception e) {
            log.error("REGISTRATION_SERVICE: Lỗi khi lưu xuống DB enrollment [ID={}]", enrollmentId, e);
        }
    }
}