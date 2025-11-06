package com.truongquycode.course_service.controller;

import com.truongquycode.course_service.model.Course;
import com.truongquycode.course_service.model.CourseSection;
import com.truongquycode.course_service.repository.CourseRepository;
// 1. IMPORT LẠI CourseSectionRepository
import com.truongquycode.course_service.repository.CourseSectionRepository;
import com.truongquycode.course_service.config.KafkaTopicConfig;
import com.truongquycode.course_service.processor.RegistrationProcessor; 

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.ArrayList;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
// 2. VẪN CẦN KafkaTemplate (cho CourseDataLoader)
import org.springframework.kafka.core.KafkaTemplate; 

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor // 3. SỬA LẠI CONSTRUCTOR
public class CourseController {

    private final CourseRepository courseRepository;
    // 4. THÊM LẠI REPOSITORY
    private final CourseSectionRepository sectionRepository; 
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;
    
    // --- HÀM MỚI CHO COURSES ---
    /**
     * TẠO MỘT KHÓA HỌC MỚI (Dữ liệu tĩnh)
     * Vì 'Course' là dữ liệu tĩnh (không có KTable), chúng ta chỉ cần lưu vào CSDL.
     */
    @PostMapping("/courses")
    public ResponseEntity<Course> createCourse(@RequestBody Course newCourse) {
        Course savedCourse = courseRepository.save(newCourse);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCourse);
    }
    

    // ========================================================================
    // HÀM MỚI: COMMANDS (CREATE, UPDATE, DELETE)
    // GHI VÀO CSDL (ĐỒNG BỘ) - CDC SẼ TỰ ĐỘNG GỬI KAFKA
    // ========================================================================

    @PostMapping("/course-sections")
    public ResponseEntity<CourseSection> createSection(@RequestBody CourseSection newSection) {
        // 1. GHI VÀO CSDL
        CourseSection savedSection = sectionRepository.save(newSection);
        
        // 2. Gửi sự kiện đến Kafka (Nguồn Chân Lý)
        kafkaTemplate.send(
            KafkaTopicConfig.COURSE_SECTIONS_STATE_TOPIC, 
            newSection.getSectionId(), 
            newSection
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSection);
    }

 // Trong file CourseController.java

    @PutMapping("/course-sections/{id}")
    public ResponseEntity<CourseSection> updateSection(@PathVariable String id, @RequestBody CourseSection updatedSectionData) {
        ReadOnlyKeyValueStore<String, CourseSection> sectionStore = getStore();
        if (sectionStore == null) {
             return ResponseEntity.status(503).build(); // Streams chưa sẵn sàng
        }

        CourseSection currentState = sectionStore.get(id);
        if (currentState == null) {
            return ResponseEntity.notFound().build();
        }

        // --- Merge ---
        currentState.setCourse(updatedSectionData.getCourse());
        currentState.setSchedule(updatedSectionData.getSchedule());
        currentState.setTotalSlots(updatedSectionData.getTotalSlots());

        // ⚠️ Cho phép admin hoặc API reset số lượng đăng ký (VD: 0)
        if (updatedSectionData.getRegisteredSlots() >= 0) {
            currentState.setRegisteredSlots(updatedSectionData.getRegisteredSlots());
        }

        // --- Đặt timestamp mới lớn hơn timestamp hiện tại ---
        long newTs = System.currentTimeMillis();
        if (currentState.getLastUpdatedAt() != null && newTs <= currentState.getLastUpdatedAt()) {
            newTs = currentState.getLastUpdatedAt() + 1;
        }
        currentState.setLastUpdatedAt(newTs);

        // --- Lưu DB (local) ---
        sectionRepository.save(currentState);

        // --- Gửi lại lên Kafka ---
        kafkaTemplate.send(
            KafkaTopicConfig.COURSE_SECTIONS_STATE_TOPIC,
            currentState.getSectionId(),
            currentState
        );

        return ResponseEntity.ok(currentState);
    }


    // Hàm deleteSection cũng nên kiểm tra lại
    @DeleteMapping("/course-sections/{id}")
    public ResponseEntity<Void> deleteSection(@PathVariable String id) {
        // 1. KIỂM TRA TỪ KTABLE (hoặc DB đều được)
        ReadOnlyKeyValueStore<String, CourseSection> sectionStore = getStore();
        if (sectionStore == null) { return ResponseEntity.status(503).build(); }
        
        CourseSection currentState = sectionStore.get(id);
        if (currentState == null) {
            return ResponseEntity.notFound().build();
        }

        // 2. Kiểm tra nghiệp vụ (VD: không cho xóa nếu đã có sinh viên đăng ký)
        if (currentState.getRegisteredSlots() > 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(null); // Hoặc trả về thông báo lỗi
        }

        // 3. XÓA KHỎI CSDL
        sectionRepository.deleteById(id);
        
        // 4. Gửi "tombstone" (bia mộ) đến Kafka
        kafkaTemplate.send(
            KafkaTopicConfig.COURSE_SECTIONS_STATE_TOPIC, 
            id, 
            null 
        );
        
        return ResponseEntity.noContent().build();
    }


    // ========================================================================
    // QUERIES (GET) - VẪN ĐỌC TỪ KTABLE
    // (Giữ nguyên không thay đổi)
    // ========================================================================

    @GetMapping("/courses")
    public ResponseEntity<List<Course>> getAllCourses() {
        List<Course> courses = courseRepository.findAll();
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/course-sections")
    public ResponseEntity<List<CourseSection>> getAllCourseSections() {
        ReadOnlyKeyValueStore<String, CourseSection> sectionStore = getStore();
        if (sectionStore == null) {
            return ResponseEntity.status(503).body(List.of()); 
        }
        List<CourseSection> sections = new ArrayList<>();
        sectionStore.all().forEachRemaining(kv -> sections.add(kv.value));
        return ResponseEntity.ok(sections);
    }
    
    @GetMapping("/course-sections/{id}")
    public ResponseEntity<CourseSection> getCourseSectionById(@PathVariable String id) {
        ReadOnlyKeyValueStore<String, CourseSection> sectionStore = getStore();
        if (sectionStore == null) {
             return ResponseEntity.status(503).build();
        }
        CourseSection section = sectionStore.get(id);
        if (section == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(section);
    }

    private ReadOnlyKeyValueStore<String, CourseSection> getStore() {
        KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
        if (kafkaStreams == null) {
            return null;
        }
        return kafkaStreams.store(
            StoreQueryParameters.fromNameAndType(
                RegistrationProcessor.SECTIONS_STORE_NAME,
                QueryableStoreTypes.keyValueStore()
            )
        );
    }
}