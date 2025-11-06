package com.truongquycode.course_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    // Topic lưu trạng thái của các lớp học (State Store)
    public static final String COURSE_SECTIONS_STATE_TOPIC = "course_sections_state";
    
    // Topic lắng nghe từ registration-service
    public static final String STUDENT_VALIDATED_TOPIC = "student_validated";
    
    // Topic gửi kết quả về
    public static final String REGISTRATION_RESULTS_TOPIC = "registration_results";

    @Bean
    public NewTopic courseSectionsStateTopic() {
        return TopicBuilder.name(COURSE_SECTIONS_STATE_TOPIC)
                .partitions(10) // Nên bằng số partition của topic input
                .replicas(1)
                // Áp dụng kiến thức CHƯƠG 4: Compacted Topics [cite: 555-557]
                // Kafka sẽ chỉ giữ lại BẢN GHI MỚI NHẤT cho mỗi key (sectionId)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT)
                .build();
    }
    
    // (Định nghĩa các topic khác nếu registration-service chưa tạo)
    @Bean
    public NewTopic studentValidatedTopic() {
        return TopicBuilder.name(STUDENT_VALIDATED_TOPIC)
                .partitions(10)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic registrationResultsTopic() {
        return TopicBuilder.name(REGISTRATION_RESULTS_TOPIC)
                .partitions(10)
                .replicas(1)
                .build();
    }
    
}