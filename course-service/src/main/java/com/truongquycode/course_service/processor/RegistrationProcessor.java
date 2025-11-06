package com.truongquycode.course_service.processor;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable; 
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
// --- 1. THÊM IMPORT NÀY ---
import org.apache.kafka.streams.state.ValueAndTimestamp; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

import com.truongquycode.common.events.EnrollmentStatus;
import com.truongquycode.common.events.RegistrationResultEvent;
import com.truongquycode.common.events.StudentValidatedEvent;
import com.truongquycode.course_service.config.KafkaTopicConfig;
import com.truongquycode.course_service.model.CourseSection;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RegistrationProcessor {

    public static final String SECTIONS_STORE_NAME = "course-sections-store";

    private record ProcessingResult(RegistrationResultEvent resultEvent, CourseSection updatedSection) {}

    @Autowired
    public void processRegistrations(StreamsBuilder builder) {
        
        Serde<String> stringSerde = Serdes.String();
        JsonSerde<StudentValidatedEvent> validatedEventSerde = new JsonSerde<>(StudentValidatedEvent.class);
        JsonSerde<CourseSection> sectionSerde = new JsonSerde<>(CourseSection.class);
        JsonSerde<RegistrationResultEvent> resultEventSerde = new JsonSerde<>(RegistrationResultEvent.class);
        JsonSerde<ProcessingResult> processingResultSerde = new JsonSerde<>(ProcessingResult.class);

        KTable<String, CourseSection> sectionsTable = builder.table(
            KafkaTopicConfig.COURSE_SECTIONS_STATE_TOPIC,
            Consumed.with(stringSerde, sectionSerde),
            Materialized.as(SECTIONS_STORE_NAME)
        );

        KStream<String, StudentValidatedEvent> validationStream = builder.stream(
            KafkaTopicConfig.STUDENT_VALIDATED_TOPIC,
            Consumed.with(stringSerde, validatedEventSerde)
        );

        KStream<String, ProcessingResult> processingStream = validationStream.process(
            () -> new StatefulRegistrationProcessor(), 
            SECTIONS_STORE_NAME
        );

        // --- Phần 5 (Split) giữ nguyên ---
        processingStream
            .mapValues(ProcessingResult::resultEvent) 
            .to(
                KafkaTopicConfig.REGISTRATION_RESULTS_TOPIC,
                Produced.with(stringSerde, resultEventSerde)
            );

        processingStream
            .filter((key, value) -> value.updatedSection() != null) 
            .mapValues(ProcessingResult::updatedSection) 
            .to(
                KafkaTopicConfig.COURSE_SECTIONS_STATE_TOPIC,
                Produced.with(stringSerde, sectionSerde)
            );
    }

    private RegistrationResultEvent createResult(StudentValidatedEvent event, EnrollmentStatus status, String reason) {
        return new RegistrationResultEvent(
            String.valueOf(event.getEnrollmentId()), 
            event.getStudentId(),
            status,
            reason,
            System.currentTimeMillis()
        );
    }

    // --- SỬA LỖI TRONG LỚP NỘI BỘ NÀY ---
 // --- SỬA LỖI TRONG LỚP NỘI BỘ NÀY ---
    private class StatefulRegistrationProcessor implements Processor<String, StudentValidatedEvent, String, ProcessingResult> {

        private KeyValueStore<String, ValueAndTimestamp<CourseSection>> sectionStore;
        private ProcessorContext<String, ProcessingResult> context;

        @Override
        public void init(ProcessorContext<String, ProcessingResult> context) {
            this.context = context;
            this.sectionStore = context.getStateStore(SECTIONS_STORE_NAME);
        }

        @Override
        public void process(Record<String, StudentValidatedEvent> record) {
            String sectionId = record.value().getCourseSectionId();
            StudentValidatedEvent event = record.value();

            // --- Bắt đầu xác định timestamp ---
            long recordTs = record.timestamp();
            long streamTs = this.context.currentStreamTimeMs();
            long newTimestamp = Math.max(recordTs, streamTs);

            ValueAndTimestamp<CourseSection> timestampedSection = sectionStore.get(sectionId);
            CourseSection section = (timestampedSection != null) ? timestampedSection.value() : null;

            ProcessingResult result;

            if (section == null) {
                log.warn("Không tìm thấy lớp học (state store) với ID: {}", sectionId);
                result = new ProcessingResult(
                    createResult(event, EnrollmentStatus.FAILED, "Không tìm thấy mã lớp học."),
                    null
                );

            } else if (section.getRegisteredSlots() >= section.getTotalSlots()) {
                log.warn("Lớp học (state store) [{}] đã hết chỗ. ({} / {})",
                    section.getSectionId(), section.getRegisteredSlots(), section.getTotalSlots());

                result = new ProcessingResult(
                    createResult(event, EnrollmentStatus.FAILED, "Lớp học đã hết chỗ."),
                    section
                );

            } else {
                // --- BẮT ĐẦU ĐẢM BẢO TIMESTAMP ĐƠN ĐIỆU ---
                if (timestampedSection != null && newTimestamp <= timestampedSection.timestamp()) {
                    // Nếu timestamp mới <= timestamp cũ => tăng thêm 1ms để giữ thứ tự
                    newTimestamp = timestampedSection.timestamp() + 1;
                }

                // --- CẬP NHẬT STATE ---
                section.setRegisteredSlots(section.getRegisteredSlots() + 1);
                log.info("Đăng ký thành công (state store) cho section: {}. Số chỗ mới: {}/{}",
                    section.getSectionId(), section.getRegisteredSlots(), section.getTotalSlots());

                sectionStore.put(sectionId, ValueAndTimestamp.make(section, newTimestamp));

                // --- TẠO KẾT QUẢ ---
                result = new ProcessingResult(
                    createResult(event, EnrollmentStatus.CONFIRMED, "Đăng ký thành công."),
                    section
                );
            }

            // --- FORWARD RA NGOÀI VỚI TIMESTAMP MỚI ---
            context.forward(record.withValue(result).withTimestamp(newTimestamp));
        }


        @Override
        public void close() {
            // Không cần làm gì
        }
    }
}