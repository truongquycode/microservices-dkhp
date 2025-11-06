package com.truongquycode.registration_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    // Không cần hằng số ở đây nữa, hoặc dùng file chung
    // public static final String REGISTRATION_REQUEST_TOPIC = "registration_requests_topic";

    @Bean
    public NewTopic registrationRequestTopic() {
        return TopicBuilder.name(TopicConstants.REGISTRATION_REQUESTS) // <-- SỬ DỤNG TÊN CHUNG
                .partitions(10)
                .replicas(1)
                .build();
    }
    
    // Bạn cũng nên định nghĩa các topic khác ở đây
    
    @Bean
    public NewTopic studentValidatedTopic() {
        return TopicBuilder.name(TopicConstants.STUDENT_VALIDATED)
                .partitions(10)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic registrationResultsTopic() {
        return TopicBuilder.name(TopicConstants.REGISTRATION_RESULTS)
                .partitions(10)
                .replicas(1)
                .build();
    }
}