//package com.truongquycode.course_service.config;
//
//import org.apache.kafka.streams.KafkaStreams;
//import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.kafka.config.KafkaStreamsCustomizer;
//
//import lombok.extern.slf4j.Slf4j;
//
//@Configuration
//@Slf4j
//public class KafkaStreamsConfig {
//
//    @Bean
//    public KafkaStreamsCustomizer customizer() {
//        return new KafkaStreamsCustomizer() {
//            @Override
//            public void customize(KafkaStreams kafkaStreams) {
//
//                // Log state transitions
//                kafkaStreams.setStateListener((newState, oldState) -> {
//                    log.info("Kafka Streams state changed: {} -> {}", oldState, newState);
//                });
//
//                // Auto recover thread on uncaught exceptions
//                kafkaStreams.setUncaughtExceptionHandler((thread, ex) -> {
//                    log.error("UNCAUGHT STREAM THREAD ERROR {}: {}", thread.getName(), ex.getMessage(), ex);
//                });
//
//            }
//        };
//    }
//
//}
