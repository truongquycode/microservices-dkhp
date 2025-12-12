package com.truongquycode.course_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    // Topic lưu trạng thái của các lớp học (State Store)
    public static final String COURSE_SECTIONS_STATE_TOPIC = "course_sections_state";
    
    // Topic lắng nghe từ registration-service
    public static final String STUDENT_VALIDATED_TOPIC = "student_validated";
    
    // Topic gửi kết quả về
    public static final String REGISTRATION_RESULTS_TOPIC = "registration_results";
    
    public static final String REGISTRATION_CANCELLED_TOPIC = "registration_cancelled";
    public static final String COURSE_SECTIONS_UPDATES_TOPIC = "course_sections_updates";
    public static final int PARTITIONS = 50;

    @Bean
    public NewTopic courseSectionsStateTopic() {
        return TopicBuilder.name(COURSE_SECTIONS_STATE_TOPIC)
                .partitions(50) // Nên bằng số partition của topic input
                .replicas(1)
                // Áp dụng kiến thức CHƯƠG 4: Compacted Topics
                // Kafka sẽ chỉ giữ lại BẢN GHI MỚI NHẤT cho mỗi key (sectionId)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT)
                .build();
    }
    
    // (Định nghĩa các topic khác nếu registration-service chưa tạo)
    @Bean
    public NewTopic studentValidatedTopic() {
        return TopicBuilder.name(STUDENT_VALIDATED_TOPIC)
                .partitions(50)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic registrationResultsTopic() {
        return TopicBuilder.name(REGISTRATION_RESULTS_TOPIC)
                .partitions(50)
                .replicas(1)
                .build();
    }
    
    @Bean
    public ApplicationRunner waitForKafkaStreams(StreamsBuilderFactoryBean factoryBean) {
        return args -> {
            while (factoryBean.getKafkaStreams() == null ||
                   !factoryBean.getKafkaStreams().state().isRunningOrRebalancing()) {
                System.out.println("Kafka Streams chua san sang, cho 1s...");
                Thread.sleep(1000);
            }
            System.out.println("Kafka Streams READY.");
        };
    }
    
}