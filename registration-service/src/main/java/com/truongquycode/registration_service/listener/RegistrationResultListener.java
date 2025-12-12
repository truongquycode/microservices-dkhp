package com.truongquycode.registration_service.listener;

import java.util.Optional;

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

        String eventId = event.getEnrollmentId();

        // Find by eventId (UUID string), not by DB id
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByEventId(eventId);

        if (enrollmentOpt.isEmpty()) {
            log.warn("REGISTRATION_SERVICE: Skip result for eventId={} because not found in DB (maybe deleted).", eventId);
            return;
        }

        Enrollment enrollment = enrollmentOpt.get();

        try {
            enrollment.setStatus(event.getStatus());
            enrollmentRepository.save(enrollment);

            log.info("REGISTRATION_SERVICE: Updated enrollment (eventId={}) -> {}", eventId, event.getStatus());

        } catch (Exception e) {
            log.error("REGISTRATION_SERVICE: Error saving enrollment (eventId={})", eventId, e);
        }
    }
}
