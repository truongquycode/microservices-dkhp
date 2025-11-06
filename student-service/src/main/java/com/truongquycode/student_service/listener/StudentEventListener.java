//package com.truongquycode.student_service.listener;
//
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Service;
//
//import com.truongquycode.common.events.RegistrationRequestEvent;
//import com.truongquycode.common.events.RegistrationResultEvent;
//import com.truongquycode.common.events.StudentValidatedEvent;
//import com.truongquycode.student_service.model.Student;
//import com.truongquycode.student_service.repository.StudentRepository;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class StudentEventListener {
//
//    private final StudentRepository studentRepository;
//    private final KafkaTemplate<String, Object> kafkaTemplate;
//
//    // Lắng nghe yêu cầu đăng ký
//    @KafkaListener(topics = "registration_requests", groupId = "student-group") // Đặt groupId
//    public void handleRegistrationRequest(RegistrationRequestEvent event) {
//        log.info("STUDENT_SERVICE: Nhận request cho studentId: {}", event.getStudentId());
//
//        // (Bạn nên dùng .findById(event.getStudentId()) thay vì .findById(event.getStudentId()).orElse(null))
//        Student student = studentRepository.findById(event.getStudentId()).orElse(null);
//
//        if (student == null || !student.isActive()) {
//            // THẤT BẠI: Gửi event thất bại
//            log.warn("Student không hợp lệ: {}", event.getStudentId());
//            RegistrationResultEvent failedEvent = new RegistrationResultEvent(
//                    event.getEnrollmentId(), 
//                    event.getStudentId(), // <-- Thêm studentId
//                    "FAILED", 
//                    "Student not found or inactive"
//            );
//            kafkaTemplate.send("registration_results", failedEvent);
//        } else {
//            // THÀNH CÔNG: Chuyển tiếp sự kiện
//            log.info("Student hợp lệ, chuyển tiếp tới course_service...");
//            
//            StudentValidatedEvent nextEvent = new StudentValidatedEvent(
//                    event.getEnrollmentId(), 
//                    event.getStudentId(), 
//                    event.getCourseSectionId()
//            );
//            kafkaTemplate.send("student_validated", nextEvent);
//        }
//    }
//}
