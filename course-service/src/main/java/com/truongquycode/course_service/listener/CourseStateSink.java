package com.truongquycode.course_service.listener;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.truongquycode.course_service.config.KafkaTopicConfig;
import com.truongquycode.course_service.model.CourseSection;
import com.truongquycode.course_service.repository.CourseSectionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseStateSink {

    private final CourseSectionRepository sectionRepository;

    @KafkaListener(
        topics = KafkaTopicConfig.COURSE_SECTIONS_UPDATES_TOPIC, // LISTEN to UPDATES (not the original source topic)
        groupId = "course-state-sink-group",
//        concurrency = "5",
        properties = {
            "spring.json.value.default.type=com.truongquycode.course_service.model.CourseSection"
        }
    )
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void handleCourseStateUpdate(ConsumerRecord<String, CourseSection> record) {
        String sectionId = record.key();
        CourseSection incoming = record.value();
        long incomingTs = record.timestamp();

        if (incoming == null) {
            sectionRepository.deleteById(sectionId);
            log.info("SINK: Deleted section {}", sectionId);
            return;
        }

        // Use repository's "update if newer" semantics to avoid regressions
        int updatedRows = sectionRepository.updateIfNewer(sectionId, incoming.getRegisteredSlots(), incomingTs);

        if (updatedRows > 0) {
//            log.info("SINK: Synced DB section {} -> slots: {} (ts={})", sectionId, incoming.getRegisteredSlots(), incomingTs);
        } else {
            log.debug("SINK: Skip update for section {} (incoming ts {} not newer)", sectionId, incomingTs);
        }
    }
}
