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
        topics = KafkaTopicConfig.COURSE_SECTIONS_STATE_TOPIC, 
        groupId = "course-state-sink-group",
        properties = {
            "spring.json.value.default.type=com.truongquycode.course_service.model.CourseSection"
        }
    )
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void handleCourseStateUpdate(ConsumerRecord<String, CourseSection> record) {
        String sectionId = record.key();
        CourseSection incoming = record.value();
        long incomingTs = record.timestamp();

        if (incoming == null) {
            sectionRepository.deleteById(sectionId);
            log.info("SINK: Đã xóa lớp {}", sectionId);
            return;
        }

        // Cập nhật vào MySQL nếu timestamp mới hơn
        int updatedRows = sectionRepository.updateIfNewer(sectionId, incoming.getRegisteredSlots(), incomingTs);
        
        if (updatedRows > 0) {
            log.info("SINK: Đã đồng bộ DB lớp {} -> Slots: {}", sectionId, incoming.getRegisteredSlots());
        } else {
            log.debug("SINK: Bỏ qua update lớp {} (Dữ liệu cũ hoặc không thay đổi)", sectionId);
        }
    }
}