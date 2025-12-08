package com.truongquycode.registration_service.listener;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
        properties = {"spring.json.value.default.type=com.truongquycode.common.events.RegistrationRequestEvent"}
    )
    @Transactional
    public void handleRegistrationRequest(RegistrationRequestEvent event) {
        log.info("REGISTRATION: Xử lý đăng ký [EventID={}]", event.getEnrollmentId());

        try {
            List<Enrollment> existingList = enrollmentRepository
                 .findAndLockByStudentIdAndCourseSectionId(event.getStudentId(), event.getCourseSectionId());

            boolean alreadyProcessed = existingList.stream()
                    .anyMatch(e -> e.getStatus() == EnrollmentStatus.PENDING || e.getStatus() == EnrollmentStatus.CONFIRMED);

            Enrollment enrollment = new Enrollment();
            enrollment.setEventId(event.getEnrollmentId());
            enrollment.setStudentId(event.getStudentId());
            enrollment.setCourseSectionId(event.getCourseSectionId());
            enrollment.setCreatedAt(LocalDateTime.now());

//            if (alreadyProcessed) {
//                log.warn("Đã đăng ký rồi. EventID={} -> FAILED", event.getEnrollmentId());
//                enrollment.setStatus(EnrollmentStatus.FAILED);
//                enrollment.setReason("Đã đăng ký rồi.");
//                enrollmentRepository.save(enrollment); 
//                return; 
//            }

            enrollment.setStatus(EnrollmentStatus.PENDING);
            Enrollment savedEnrollment = enrollmentRepository.save(enrollment);

            StudentValidatedEvent validatedEvent = new StudentValidatedEvent(
                savedEnrollment.getId(), 
                savedEnrollment.getStudentId(),
                savedEnrollment.getCourseSectionId()
            );

            kafkaTemplate.send(TopicConstants.STUDENT_VALIDATED, event.getCourseSectionId(), validatedEvent);

        } catch (Exception e) {
            log.error("Lỗi xử lý đăng ký", e);
            saveError(event, e.getMessage());
        }
    }

    @KafkaListener(
        topics = "registration_cancels", 
        groupId = KAFKA_GROUP_ID,
        properties = {"spring.json.value.default.type=com.truongquycode.common.events.RegistrationRequestEvent"}
    )
    @Transactional
    public void handleCancelRequest(RegistrationRequestEvent event) {
        log.info("CANCEL: Xử lý hủy [EventID={}]", event.getEnrollmentId());
        try {
            List<Enrollment> list = enrollmentRepository
                 .findAndLockByStudentIdAndCourseSectionId(event.getStudentId(), event.getCourseSectionId());
            
            boolean found = false;
            for (Enrollment en : list) {
                if (en.getStatus() == EnrollmentStatus.CONFIRMED || en.getStatus() == EnrollmentStatus.PENDING) {
                    en.setStatus(EnrollmentStatus.CANCELLED);
                    en.setReason("Sinh viên hủy");
                    enrollmentRepository.save(en);
                    found = true;
                }
            }

            Enrollment result = new Enrollment();
            result.setEventId(event.getEnrollmentId());
            result.setStudentId(event.getStudentId());
            result.setCourseSectionId(event.getCourseSectionId());
            result.setCreatedAt(LocalDateTime.now());
            result.setStatus(found ? EnrollmentStatus.CANCELLED : EnrollmentStatus.FAILED); 
            result.setReason(found ? "Đã hủy thành công" : "Không tìm thấy lớp để hủy");
            enrollmentRepository.save(result);

            if (found) {

                log.info("Gửi sự kiện registration_cancelled để CourseService giảm sỉ số");
                kafkaTemplate.send("registration_cancelled", event.getCourseSectionId(), event);
            }

        } catch (Exception e) {
            log.error("Lỗi xử lý hủy", e);
            saveError(event, e.getMessage());
        }
    }

    private void saveError(RegistrationRequestEvent event, String reason) {
        try {
            Enrollment err = new Enrollment();
            err.setEventId(event.getEnrollmentId());
            err.setStudentId(event.getStudentId());
            err.setCourseSectionId(event.getCourseSectionId());
            err.setCreatedAt(LocalDateTime.now());
            err.setStatus(EnrollmentStatus.FAILED);
            err.setReason(reason);
            enrollmentRepository.save(err);
        } catch(Exception e) {}
    }
}