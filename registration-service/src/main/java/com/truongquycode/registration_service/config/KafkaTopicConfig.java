package com.truongquycode.registration_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    
    @Bean
    public NewTopic registrationRequestTopic() {
        return TopicBuilder.name(TopicConstants.REGISTRATION_REQUESTS)
                .partitions(50)
                .replicas(1)
                .build();
    }
    
    @Bean
    public NewTopic studentValidatedTopic() {
        return TopicBuilder.name(TopicConstants.STUDENT_VALIDATED)
                .partitions(50)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic registrationResultsTopic() {
        return TopicBuilder.name(TopicConstants.REGISTRATION_RESULTS)
                .partitions(50)
                .replicas(1)
                .build();
    }

    // --- MỚI: Bean tạo topic Hủy ---
    @Bean
    public NewTopic registrationCancelsTopic() {
        return TopicBuilder.name(TopicConstants.REGISTRATION_CANCELS)
                .partitions(3) // Hủy ít hơn đăng ký nên để 3 partition là đủ
                .replicas(1)
                .build();
    }

    // --- MỚI: Bean tạo topic Đã Hủy (để giảm sỉ số) ---
    @Bean
    public NewTopic registrationCancelledTopic() {
        return TopicBuilder.name(TopicConstants.REGISTRATION_CANCELLED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}