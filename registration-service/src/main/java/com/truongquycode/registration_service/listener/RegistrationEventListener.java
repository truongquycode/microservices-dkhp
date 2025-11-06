//package com.truongquycode.registration_service.listener;
//
//import java.time.LocalDateTime;
//import java.util.List; // SỬA: Dùng List
//import java.util.Optional;
//
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional; // THÊM IMPORT
//
//import com.truongquycode.registration_service.config.TopicConstants; 
//import com.truongquycode.common.events.EnrollmentStatus;
//import com.truongquycode.common.events.RegistrationRequestEvent;
//import com.truongquycode.common.events.StudentValidatedEvent; 
//import com.truongquycode.registration_service.model.Enrollment;
//import com.truongquycode.registration_service.repository.EnrollmentRepository;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class RegistrationEventListener {
//
//    private final EnrollmentRepository enrollmentRepository;
//    private final KafkaTemplate<String, Object> kafkaTemplate; 
//    
//    private static final String KAFKA_GROUP_ID = "registration-group"; 
//
//    @KafkaListener(
//        topics = TopicConstants.REGISTRATION_REQUESTS, 
//        groupId = KAFKA_GROUP_ID,
//        properties = {
//            "spring.json.value.default.type=com.truongquycode.common.events.RegistrationRequestEvent"
//        }
//    )
//    @Transactional // <-- BẮT BUỘC: Thêm @Transactional để @Lock hoạt động
//    public void handleRegistrationRequest(RegistrationRequestEvent event) {
//        
//        log.info("REGISTRATION_SERVICE: Bắt đầu xử lý [EventID={}]", event.getEnrollmentId());
//
//        try {
//            // SỬA: Dùng phương thức findAndLock... mới
//            // Thread 1 sẽ lock. Thread 2 (cùng student/course) sẽ chờ ở đây.
//            List<Enrollment> existingList = enrollmentRepository
//                 .findAndLockByStudentIdAndCourseSectionId(event.getStudentId(), event.getCourseSectionId());
//
//            Enrollment enrollment = new Enrollment();
//            enrollment.setEventId(event.getEnrollmentId());
//            enrollment.setStudentId(event.getStudentId());
//            enrollment.setCourseSectionId(event.getCourseSectionId());
//            enrollment.setCreatedAt(LocalDateTime.now());
//            
//            // SỬA: Lọc danh sách (vì list có thể chứa các bản ghi FAILED cũ)
//            boolean alreadyProcessed = existingList.stream()
//                    .anyMatch(e -> e.getStatus() == EnrollmentStatus.PENDING || e.getStatus() == EnrollmentStatus.CONFIRMED);
//
//            if (alreadyProcessed) {
//                // Thread 1 đã chạy xong. Thread 2 sẽ đi vào đây.
//                log.warn("Đã tồn tại đăng ký PENDING/CONFIRMED. Đánh dấu EventID={} là FAILED.", event.getEnrollmentId());
//                
//                enrollment.setStatus(EnrollmentStatus.FAILED);
//                enrollment.setReason("Bạn đã đăng ký lớp học này rồi.");
//                
//                enrollmentRepository.save(enrollment); // Lưu bản ghi FAILED
//                return; // Commit và nhả lock
//            }
//
//            // --- TRƯỜNG HỢP 2: ĐĂNG KÝ MỚI (Thread 1) ---
//            log.info("Đăng ký mới. Đánh dấu EventID={} là PENDING.", event.getEnrollmentId());
//            enrollment.setStatus(EnrollmentStatus.PENDING);
//            Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
//
//            StudentValidatedEvent validatedEvent = new StudentValidatedEvent(
//                savedEnrollment.getId(), 
//                savedEnrollment.getStudentId(),
//                savedEnrollment.getCourseSectionId()
//            );
//
//            kafkaTemplate.send(TopicConstants.STUDENT_VALIDATED, event.getCourseSectionId(), validatedEvent);
//            log.info("REGISTRATION_SERVICE: Đã gửi StudentValidatedEvent cho enrollment [DB_ID={}]", savedEnrollment.getId());
//            // Commit và nhả lock
//
//        } catch (Exception e) {
//            log.error("REGISTRATION_SERVICE: Lỗi khi xử lý RegistrationRequestEvent [EventID={}]", event.getEnrollmentId(), e);
//            
//            // Cố gắng lưu bản ghi FAILED nếu có lỗi
//            try {
//                Enrollment errorRecord = new Enrollment();
//                errorRecord.setEventId(event.getEnrollmentId());
//                errorRecord.setStudentId(event.getStudentId());
//                errorRecord.setCourseSectionId(event.getCourseSectionId());
//                errorRecord.setCreatedAt(LocalDateTime.now());
//                errorRecord.setStatus(EnrollmentStatus.FAILED);
//                errorRecord.setReason(e.getMessage());
//                enrollmentRepository.save(errorRecord);
//            } catch (Exception saveE) {
//                log.error("KHÔNG THỂ LƯU TRẠNG THÁI FAILED CHO EventID={}!", event.getEnrollmentId(), saveE);
//            }
//        }
//    }
//}


package com.truongquycode.registration_service.listener;

import java.time.LocalDateTime;
import java.util.List; 
import java.util.Optional;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
// import org.springframework.transaction.annotation.Transactional; // Bỏ @Transactional vì không cần @Lock

import com.truongquycode.registration_service.config.TopicConstants; 
import com.truongquycode.common.events.EnrollmentStatus;
import com.truongquycode.common.events.RegistrationRequestEvent;
import com.truongquycode.common.events.StudentValidatedEvent; 
import com.truongquycode.registration_service.model.Enrollment;
import com.truongquycode.registration_service.repository.EnrollmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationEventListener {

    private final EnrollmentRepository enrollmentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate; 
    
    private static final String KAFKA_GROUP_ID = "registration-group"; 

    @KafkaListener(
        topics = TopicConstants.REGISTRATION_REQUESTS, 
        groupId = KAFKA_GROUP_ID,
        properties = {
            "spring.json.value.default.type=com.truongquycode.common.events.RegistrationRequestEvent"
        }
    )
    // @Transactional // Bỏ @Transactional vì không dùng @Lock
    public void handleRegistrationRequest(RegistrationRequestEvent event) {
        
        log.info("REGISTRATION_SERVICE (TEST HIỆU NĂNG): Bắt đầu xử lý [EventID={}]", event.getEnrollmentId());

        try {
            // =====================================================================
            // VÔ HIỆU HÓA LOGIC KIỂM TRA TRÙNG LẶP ĐỂ TEST HIỆU NĂNG
            // =====================================================================
            /*
            log.info("Đang kiểm tra trùng lặp (BỊ VÔ HIỆU HÓA)...");
            List<Enrollment> existingList = enrollmentRepository
                 .findAndLockByStudentIdAndCourseSectionId(event.getStudentId(), event.getCourseSectionId());
            */
            
            Enrollment enrollment = new Enrollment();
            enrollment.setEventId(event.getEnrollmentId());
            enrollment.setStudentId(event.getStudentId());
            enrollment.setCourseSectionId(event.getCourseSectionId());
            enrollment.setCreatedAt(LocalDateTime.now());
            
            /*
            boolean alreadyProcessed = existingList.stream()
                    .anyMatch(e -> e.getStatus() == EnrollmentStatus.PENDING || e.getStatus() == EnrollmentStatus.CONFIRMED);

            if (alreadyProcessed) {
                log.warn("TEST HIỆU NĂNG: Bỏ qua kiểm tra trùng lặp cho EventID={}", event.getEnrollmentId());
                
                // enrollment.setStatus(EnrollmentStatus.FAILED);
                // enrollment.setReason("BỎ QUA KIỂM TRA TRÙNG LẶP");
                // enrollmentRepository.save(enrollment); 
                // return; 
            }
            */
            // =====================================================================
            // KẾT THÚC VÔ HIỆU HÓA
            // =====================================================================


            // --- LUỒNG TEST: MỌI REQUEST ĐỀU LÀ ĐĂNG KÝ MỚI ---
            log.info("TEST HIỆU NĂNG: Xử lý EventID={} như đăng ký mới.", event.getEnrollmentId());
            enrollment.setStatus(EnrollmentStatus.PENDING);
            Enrollment savedEnrollment = enrollmentRepository.save(enrollment);

            StudentValidatedEvent validatedEvent = new StudentValidatedEvent(
                savedEnrollment.getId(), 
                savedEnrollment.getStudentId(),
                savedEnrollment.getCourseSectionId()
            );

            kafkaTemplate.send(TopicConstants.STUDENT_VALIDATED, event.getCourseSectionId(), validatedEvent);
            log.info("REGISTRATION_SERVICE (TEST HIỆU NĂNG): Đã gửi StudentValidatedEvent cho [DB_ID={}]", savedEnrollment.getId());

        } catch (Exception e) {
            log.error("REGISTRATION_SERVICE (TEST HIỆU NĂNG): Lỗi khi xử lý [EventID={}]", event.getEnrollmentId(), e);
            
            // Cố gắng lưu bản ghi FAILED nếu có lỗi (ví dụ: DB sập)
            try {
                Enrollment errorRecord = new Enrollment();
                errorRecord.setEventId(event.getEnrollmentId());
                errorRecord.setStudentId(event.getStudentId());
                errorRecord.setCourseSectionId(event.getCourseSectionId());
                errorRecord.setCreatedAt(LocalDateTime.now());
                errorRecord.setStatus(EnrollmentStatus.FAILED);
                errorRecord.setReason(e.getMessage());
                enrollmentRepository.save(errorRecord);
            } catch (Exception saveE) {
                log.error("KHÔNG THỂ LƯU TRẠNG THÁI FAILED CHO EventID={}!", event.getEnrollmentId(), saveE);
            }
        }
    }
}