package com.truongquycode.course_service.processor;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Repartitioned;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.ValueAndTimestamp;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

import com.truongquycode.common.events.EnrollmentStatus;
import com.truongquycode.common.events.RegistrationRequestEvent;
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
        JsonSerde<RegistrationRequestEvent> requestSerde = new JsonSerde<>(RegistrationRequestEvent.class);
        JsonSerde<CourseSection> sectionSerde = new JsonSerde<>(CourseSection.class);
        JsonSerde<RegistrationResultEvent> resultEventSerde = new JsonSerde<>(RegistrationResultEvent.class);

        // 1. Read existing sections into a KTable state store (this is read-only for Streams app)
        KTable<String, CourseSection> sectionsTable = builder.table(
            KafkaTopicConfig.COURSE_SECTIONS_STATE_TOPIC,
            Consumed.with(stringSerde, sectionSerde),
            Materialized.<String, CourseSection, KeyValueStore<org.apache.kafka.common.utils.Bytes, byte[]>>as(SECTIONS_STORE_NAME)
                .withKeySerde(stringSerde)
                .withValueSerde(sectionSerde)
        );

        // 2. Student validated stream (registration finalization)
        KStream<String, StudentValidatedEvent> validationStream = builder.stream(
            KafkaTopicConfig.STUDENT_VALIDATED_TOPIC,
            Consumed.with(stringSerde, validatedEventSerde)
        )
        // ensure key = sectionId
        .selectKey((key, value) -> value.getCourseSectionId())
        // make sure partitioning matches store partitions
        .repartition(Repartitioned.<String, StudentValidatedEvent>as("repartition-validations")
            .withKeySerde(stringSerde)
            .withValueSerde(validatedEventSerde)
            .withNumberOfPartitions(KafkaTopicConfig.PARTITIONS)
        );

        // 3. Cancel stream (from registration service)
        KStream<String, RegistrationRequestEvent> cancelStream = builder.stream(
            KafkaTopicConfig.REGISTRATION_CANCELLED_TOPIC,
            Consumed.with(stringSerde, requestSerde)
        )
        .selectKey((k, v) -> v.getCourseSectionId())
        .repartition(Repartitioned.<String, RegistrationRequestEvent>as("repartition-cancels")
            .withKeySerde(stringSerde)
            .withValueSerde(requestSerde)
            .withNumberOfPartitions(KafkaTopicConfig.PARTITIONS)
        );

        // 4. Process streams using processors that access the same state store.
        KStream<String, ProcessingResult> registrationOutputStream = validationStream.process(
            () -> new StatefulRegistrationProcessor(),
            SECTIONS_STORE_NAME
        );

        KStream<String, ProcessingResult> cancelOutputStream = cancelStream.process(
            () -> new StatefulCancellationProcessor(),
            SECTIONS_STORE_NAME
        );

        // 5. Merge results and write outputs:
        KStream<String, ProcessingResult> merged = registrationOutputStream.merge(cancelOutputStream);

        // a) results events (to registration_results)
        merged
            .mapValues(ProcessingResult::resultEvent)
            .to(KafkaTopicConfig.REGISTRATION_RESULTS_TOPIC, Produced.with(stringSerde, resultEventSerde));

        // b) section updates -> write to a separate updates topic (avoid writing back into the KTable source topic)
        merged
            .filter((k, v) -> v.updatedSection() != null)
            .mapValues(ProcessingResult::updatedSection)
            .to(KafkaTopicConfig.COURSE_SECTIONS_UPDATES_TOPIC, Produced.with(stringSerde, sectionSerde));
    }

    private RegistrationResultEvent createResult(String enrollmentId, String studentId, EnrollmentStatus status, String reason) {
        return new RegistrationResultEvent(
            enrollmentId,
            studentId,
            status,
            reason,
            System.currentTimeMillis()
        );
    }

    // --- Processor for registration confirmations ---
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

            long newTimestamp = Math.max(record.timestamp(), context.currentStreamTimeMs());
            ValueAndTimestamp<CourseSection> vt = sectionStore.get(sectionId);
            if (vt != null && newTimestamp <= vt.timestamp()) {
                newTimestamp = vt.timestamp() + 1;
            }

            CourseSection section = (vt != null) ? vt.value() : null;
            ProcessingResult result;

            if (section == null) {
                result = new ProcessingResult(createResult(event.getEnrollmentId(), event.getStudentId(), EnrollmentStatus.FAILED, "Không tìm thấy lớp (Reg)."), null);
            } else if (section.getRegisteredSlots() >= section.getTotalSlots()) {
                result = new ProcessingResult(createResult(event.getEnrollmentId(), event.getStudentId(), EnrollmentStatus.FAILED, "Lớp đã hết chỗ."), section);
            } else {
                // safe because all events for this section go to the same partition/task
                int newSlots = section.getRegisteredSlots() + 1;
                section.setRegisteredSlots(newSlots);
                section.setLastUpdatedAt(newTimestamp); // if CourseSection has such field
                sectionStore.put(sectionId, ValueAndTimestamp.make(section, newTimestamp));
//                log.info("REGISTRATION: Section={} slots {}/{}", sectionId, newSlots, section.getTotalSlots());
                result = new ProcessingResult(createResult(event.getEnrollmentId(), event.getStudentId(), EnrollmentStatus.CONFIRMED, "Đăng ký thành công."), section);
            }

            context.forward(record.withValue(result).withTimestamp(newTimestamp));
        }

        @Override
        public void close() {}
    }

    // --- Processor for cancellations ---
    private class StatefulCancellationProcessor implements Processor<String, RegistrationRequestEvent, String, ProcessingResult> {
        private KeyValueStore<String, ValueAndTimestamp<CourseSection>> sectionStore;
        private ProcessorContext<String, ProcessingResult> context;

        @Override
        public void init(ProcessorContext<String, ProcessingResult> context) {
            this.context = context;
            this.sectionStore = context.getStateStore(SECTIONS_STORE_NAME);
        }

        @Override
        public void process(Record<String, RegistrationRequestEvent> record) {
            String sectionId = record.value().getCourseSectionId();
            RegistrationRequestEvent event = record.value();

            long newTimestamp = Math.max(record.timestamp(), context.currentStreamTimeMs());
            ValueAndTimestamp<CourseSection> vt = sectionStore.get(sectionId);
            if (vt != null && newTimestamp <= vt.timestamp()) {
                newTimestamp = vt.timestamp() + 1;
            }

            CourseSection section = (vt != null) ? vt.value() : null;
            ProcessingResult result;

            if (section == null) {
                log.error("CANCEL: Không tìm thấy lớp {} trong store (Partition ID={}).", sectionId, context.taskId().partition());
                result = new ProcessingResult(createResult(event.getEnrollmentId(), event.getStudentId(), EnrollmentStatus.FAILED, "Không tìm thấy lớp để hủy."), null);
            } else {
                if (section.getRegisteredSlots() > 0) {
                    section.setRegisteredSlots(section.getRegisteredSlots() - 1);
                    log.info("CANCEL: Section={} slots {}/{}", sectionId, section.getRegisteredSlots(), section.getTotalSlots());
                } else {
                    log.warn("CANCEL: Section {} slots already 0.", sectionId);
                }
                section.setLastUpdatedAt(newTimestamp); // if CourseSection has such field
                sectionStore.put(sectionId, ValueAndTimestamp.make(section, newTimestamp));
                result = new ProcessingResult(createResult(event.getEnrollmentId(), event.getStudentId(), EnrollmentStatus.CANCELLED, "Hủy thành công."), section);
            }

            context.forward(record.withValue(result).withTimestamp(newTimestamp));
        }

        @Override
        public void close() {}
    }
}
