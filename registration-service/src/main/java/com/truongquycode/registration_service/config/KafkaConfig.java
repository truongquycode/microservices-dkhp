package com.truongquycode.registration_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> template) {

        // Thử lại 2 lần (tổng cộng 3 lần), mỗi lần cách nhau 1 giây
        var backOff = new FixedBackOff(1000L, 2L);

        // Nếu thất bại cả 3 lần, ném event vào topic DLT
        var recoverer = new DeadLetterPublishingRecoverer(template);

        return new DefaultErrorHandler(recoverer, backOff);
    }
}
