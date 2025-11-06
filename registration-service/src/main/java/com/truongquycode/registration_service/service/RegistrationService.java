package com.truongquycode.registration_service.service;

import java.util.UUID; // Sử dụng UUID cho ID sự kiện

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.truongquycode.common.events.RegistrationRequestEvent;
import com.truongquycode.registration_service.config.TopicConstants;
import com.truongquycode.registration_service.dto.RegistrationRequestDto;
import com.truongquycode.registration_service.model.Enrollment; // Vẫn cần để lấy status
import com.truongquycode.registration_service.repository.EnrollmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    // CHỈ GIỮ LẠI KAFKA
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    // VẪN GIỮ LẠI REPOSITORY, NHƯNG CHỈ DÙNG CHO VIỆC ĐỌC (GET)
    private final EnrollmentRepository enrollmentRepository;


    /**
     * Tái cấu trúc: Phương thức này bây giờ chỉ hoạt động như một "Command Gateway".
     * Nó không ghi vào DB. Nó chỉ đẩy sự kiện vào Kafka và trả về ngay lập tức.
     * Đây là chìa khóa để xử lý hàng ngàn request/giây.
     */
    public String requestRegistration(RegistrationRequestDto dto, String studentId) {
        
        log.info("Tiếp nhận yêu cầu đăng ký cho sinh viên [{}] lớp [{}]", studentId, dto.getCourseSectionId());

        // Chúng ta không kiểm tra trùng lặp ở đây.
        // Việc đó sẽ được xử lý bất đồng bộ bởi một bộ xử lý (processor).
        // [Tham khảo Sách: Ch. 15, Xử lý stateful streams]

        // Tạo một ID duy nhất cho sự kiện này
        String enrollmentId = UUID.randomUUID().toString();

        // 2. Tạo Event để gửi đi
        RegistrationRequestEvent event = new RegistrationRequestEvent(
                enrollmentId, // ID này là UUID (String)
                studentId,
                dto.getCourseSectionId(),
                System.currentTimeMillis()
        );

        // Gửi sự kiện vào Log (Nguồn sự thật)
        kafkaTemplate.send(
        		TopicConstants.REGISTRATION_REQUESTS, 
            event.getCourseSectionId(), // Key (partition) bằng mã lớp
            event
        );

        log.info("Đã phát sự kiện (event) cho yêu cầu [ID={}]", enrollmentId);
        
        // Trả về ID sự kiện ngay lập tức.
        // Controller sẽ trả về 202 ACCEPTED.
        return enrollmentId;
    }
    
    /**
     * Phương thức này là phần "Query" của CQRS.
     * Nó đọc từ "Read Model" (database) đã được xây dựng bất đồng bộ.
     */
    public Enrollment getRegistrationStatus(Long enrollmentId) {
        log.info("Kiểm tra trạng thái (Query) cho [ID={}]", enrollmentId);
        return enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đăng ký với ID: " + enrollmentId));
    }
    
    // Phương thức kiểm tra bằng ID (String) nếu bạn muốn dùng UUID làm ID chính
    public Enrollment getRegistrationStatusByEventId(String eventId) {
         log.info("Kiểm tra trạng thái (Query) cho [EventID={}]", eventId);
         return enrollmentRepository.findByEventId(eventId) // Cần thêm phương thức này vào Repository
                 .orElseThrow(() -> new RuntimeException("Không tìm thấy đăng ký với EventID: " + eventId));
    }
}