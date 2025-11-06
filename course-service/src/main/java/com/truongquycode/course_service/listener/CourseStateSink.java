package com.truongquycode.course_service.listener;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.truongquycode.course_service.config.KafkaTopicConfig;
import com.truongquycode.course_service.model.CourseSection;
import com.truongquycode.course_service.repository.CourseSectionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Đây là "Sink" (Đầu ra) của kiến trúc.
 * Nhiệm vụ duy nhất: Lắng nghe Nguồn Chân Lý (Kafka)
 * và cập nhật bản sao lưu (MySQL).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CourseStateSink {

    private final CourseSectionRepository sectionRepository;

    @KafkaListener(
        topics = KafkaTopicConfig.COURSE_SECTIONS_STATE_TOPIC, 
        groupId = "course-state-sink-group", // Một group ID riêng biệt
        properties = {
            "spring.json.value.default.type=com.truongquycode.course_service.model.CourseSection"
        }
    )
    @Transactional
    public void handleCourseStateUpdate(ConsumerRecord<String, CourseSection> record) {
        String sectionId = record.key();
        CourseSection incoming = record.value();
        long incomingTs = record.timestamp(); // timestamp của bản ghi trên topic

        if (incoming == null) {
            sectionRepository.deleteById(sectionId);
            return;
        }

        // Try conditional update
        int updatedRows = sectionRepository.updateIfNewer(sectionId, incoming.getRegisteredSlots(), incomingTs);
        if (updatedRows == 0) {
            // Không cập nhật vì DB có bản mới hơn; log debug
            log.debug("SINK: Bản cập nhật của {} bị bỏ qua vì cũ hơn timestamp hiện tại (incomingTs={})", sectionId, incomingTs);
        } else {
            log.info("SINK: Cập nhật DB cho {} -> registeredSlots={}, ts={}", sectionId, incoming.getRegisteredSlots(), incomingTs);
        }
    }

}