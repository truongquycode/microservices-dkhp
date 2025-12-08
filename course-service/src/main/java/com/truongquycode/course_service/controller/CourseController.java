package com.truongquycode.course_service.controller;

import com.truongquycode.course_service.model.Course;
import com.truongquycode.course_service.model.CourseSection;
import com.truongquycode.course_service.model.ScheduleEntry;
import com.truongquycode.course_service.repository.CourseRepository;
import com.truongquycode.course_service.repository.CourseSectionRepository;
import com.truongquycode.course_service.config.KafkaTopicConfig;
import com.truongquycode.course_service.processor.RegistrationProcessor; 

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional; 

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional; 

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CourseController {

    private final CourseRepository courseRepository;
    private final CourseSectionRepository sectionRepository; 
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    // ========================================================================
    // I. API QUẢN LÝ KHÓA HỌC (COURSE)
    // ========================================================================

    @PostMapping("/courses")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional
    public ResponseEntity<Course> createCourse(@RequestBody Course newCourse) {
        Course savedCourse = courseRepository.save(newCourse);
        
        // TRÁNH LỖI LAZY KHI TRẢ VỀ JSON
        if(savedCourse.getCourseSections() != null) {
            savedCourse.getCourseSections().size(); 
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(savedCourse);
    }
    
    @PutMapping("/courses/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional
    public ResponseEntity<Course> updateCourse(@PathVariable String id, @RequestBody Course courseDetails) {
        Optional<Course> courseOptional = courseRepository.findById(id);
        if (courseOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Course courseToUpdate = courseOptional.get();
        courseToUpdate.setName(courseDetails.getName());
        courseToUpdate.setCredits(courseDetails.getCredits());
        
        Course updatedCourse = courseRepository.save(courseToUpdate);
        
        // Load danh sách sections lên trước khi đóng transaction
        if(updatedCourse.getCourseSections() != null) {
            updatedCourse.getCourseSections().size(); // Ép Hibernate tải dữ liệu
            
            // JSON trả về đầy đủ chi tiết sâu hơn như trong getAllCourses:
            for (CourseSection section : updatedCourse.getCourseSections()) {
                 if (section.getScheduleEntries() != null) {
                     section.getScheduleEntries().size();
                 }
            }
        }
        
        return ResponseEntity.ok(updatedCourse);
    }
    
    @DeleteMapping("/courses/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteCourse(@PathVariable String id) {
        if (!courseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        courseRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * LẤY TẤT CẢ KHÓA HỌC (ĐÃ SỬA LỖI LAZY)
     */
    @GetMapping("/courses")
    @Transactional(readOnly = true) 
    public ResponseEntity<List<Course>> getAllCourses() {
        List<Course> courses = courseRepository.findAll();
        for (Course course : courses) {
            // Tải CourseSections
            course.getCourseSections().size(); 
            
            // Tải ScheduleEntries cho mỗi section
            for (CourseSection section : course.getCourseSections()) {
                section.getScheduleEntries().size();
                // Tải luôn Course bên trong section
                // để đảm bảo không bị lỗi vòng lặp
                if (section.getCourse() != null) {
                    section.getCourse().getCourseId();
                }
            }
        }
        return ResponseEntity.ok(courses);
    }
    

    // ========================================================================
    // II. API QUẢN LÝ LỚP HỌC PHẦN (COURSE SECTION)
    // ========================================================================
    
    @PostMapping("/course-sections")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional
    public ResponseEntity<CourseSection> createSection(@RequestBody CourseSection newSection) {
        if (newSection.getCourse() == null || newSection.getCourse().getCourseId() == null) {
            return ResponseEntity.badRequest().build(); 
        }
        String courseId = newSection.getCourse().getCourseId();
        Course managedCourse = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy Course ID: " + courseId));
        newSection.setCourse(managedCourse);
        if (newSection.getScheduleEntries() != null) {
            for (ScheduleEntry entry : newSection.getScheduleEntries()) {
                entry.setCourseSection(newSection);
            }
        }
        CourseSection savedSection = sectionRepository.save(newSection);
        kafkaTemplate.send(
            KafkaTopicConfig.COURSE_SECTIONS_STATE_TOPIC, 
            savedSection.getSectionId(), 
            savedSection
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSection);
    }

    @PutMapping("/course-sections/{id}")
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<CourseSection> updateSection(@PathVariable String id, @RequestBody CourseSection updatedSectionData) {
        Optional<CourseSection> dbEntryOpt = sectionRepository.findById(id);
        if (dbEntryOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        CourseSection entityToUpdate = dbEntryOpt.get();
        if (updatedSectionData.getCourse() == null || updatedSectionData.getCourse().getCourseId() == null) {
            return ResponseEntity.badRequest().build(); 
        }
        String courseId = updatedSectionData.getCourse().getCourseId();
        Course managedCourse = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy Course ID: " + courseId));
        entityToUpdate.setCourse(managedCourse);
        entityToUpdate.setGroupName(updatedSectionData.getGroupName());
        entityToUpdate.setInstructorName(updatedSectionData.getInstructorName());
        entityToUpdate.setTotalSlots(updatedSectionData.getTotalSlots());
        if (updatedSectionData.getRegisteredSlots() >= 0) {
            entityToUpdate.setRegisteredSlots(updatedSectionData.getRegisteredSlots());
        }
        entityToUpdate.setAcademicYear(updatedSectionData.getAcademicYear());
        entityToUpdate.setSemester(updatedSectionData.getSemester());
        if (entityToUpdate.getScheduleEntries() == null) {
            entityToUpdate.setScheduleEntries(new ArrayList<>());
        }
        entityToUpdate.getScheduleEntries().clear(); 
        if (updatedSectionData.getScheduleEntries() != null) {
            for (ScheduleEntry newEntry : updatedSectionData.getScheduleEntries()) {
                newEntry.setCourseSection(entityToUpdate); 
                entityToUpdate.getScheduleEntries().add(newEntry);
            }
        }
        long newTs = System.currentTimeMillis();
        if (entityToUpdate.getLastUpdatedAt() != null && newTs <= entityToUpdate.getLastUpdatedAt()) {
            newTs = entityToUpdate.getLastUpdatedAt() + 1;
        }
        entityToUpdate.setLastUpdatedAt(newTs);
        CourseSection savedSection = sectionRepository.save(entityToUpdate);
        kafkaTemplate.send(
            KafkaTopicConfig.COURSE_SECTIONS_STATE_TOPIC,
            savedSection.getSectionId(),
            savedSection
        );
        return ResponseEntity.ok(savedSection);
    }

    @DeleteMapping("/course-sections/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteSection(@PathVariable String id) {
        Optional<CourseSection> dbEntryOpt = sectionRepository.findById(id);
        if (dbEntryOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        CourseSection sectionToDelete = dbEntryOpt.get();
        if (sectionToDelete.getRegisteredSlots() > 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); 
        }
        sectionRepository.deleteById(id);
        kafkaTemplate.send(
            KafkaTopicConfig.COURSE_SECTIONS_STATE_TOPIC, 
            id, 
            null 
        );
        return ResponseEntity.noContent().build();
    }
    // --- QUERIES (ĐỌC DỮ LIỆU) ---
    
    @GetMapping("/course-sections")
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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
    
    @GetMapping("/courses/{courseId}/sections")
    @Transactional(readOnly = true)
    public ResponseEntity<List<CourseSection>> getSectionsByCourse(@PathVariable String courseId) {
        if (!courseRepository.existsById(courseId)) {
             return ResponseEntity.notFound().build();
        }
        List<CourseSection> sections = sectionRepository.findByCourse_CourseId(courseId);
        return ResponseEntity.ok(sections);
    }

    /**
     * LẤY TẤT CẢ LỚP HỌC PHẦN (ĐỌC TỪ CSDL) - DÙNG CHO ADMIN (ĐÃ SỬA LỖI LAZY)
     */
    @GetMapping("/db/course-sections")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')") 
    @Transactional(readOnly = true) 
    public ResponseEntity<List<CourseSection>> getAllCourseSectionsFromDb() {
        List<CourseSection> sections = sectionRepository.findAll();
        for (CourseSection section : sections) {
            // Tải Course (để lấy courseId)
            if (section.getCourse() != null) {
                section.getCourse().getCourseId(); 
                // Tải luôn CourseSections của Course đó
                section.getCourse().getCourseSections().size();
            }
            // Tải ScheduleEntries
            section.getScheduleEntries().size();
        }
        
        return ResponseEntity.ok(sections);
    }

    /**
     * LẤY MỘT LỚP HỌC PHẦN THEO ID (ĐỌC TỪ CSDL) (ĐÃ SỬA LỖI LAZY)
     */
    @GetMapping("/db/course-sections/{id}")
    @Transactional(readOnly = true) 
    public ResponseEntity<CourseSection> getCourseSectionFromDbById(@PathVariable String id) {
        Optional<CourseSection> sectionOpt = sectionRepository.findById(id);

        if (sectionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        CourseSection section = sectionOpt.get();
        // Sửa lỗi Lazy:
        if (section.getCourse() != null) {
            section.getCourse().getCourseId();
        }
        section.getScheduleEntries().size();
        
        return ResponseEntity.ok(section);
    }

    // ========================================================================
    // III. PHƯƠNG THỨC NỘI BỘ (PRIVATE HELPER)
    // ========================================================================

    // Hàm getStore()
    private ReadOnlyKeyValueStore<String, CourseSection> getStore() {
        KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
        if (kafkaStreams == null) {
            return null;
        }
        try {
            return kafkaStreams.store(
                StoreQueryParameters.fromNameAndType(
                    RegistrationProcessor.SECTIONS_STORE_NAME,
                    QueryableStoreTypes.keyValueStore()
                )
            );
        } catch (Exception e) {
            return null;
        }
    }
    
    // Hàm getCourseWithSections()
    @GetMapping("/courses/{id}/details")
    @Transactional(readOnly = true)
    public ResponseEntity<Course> getCourseWithSections(@PathVariable String id) {
        Course course = courseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"));
        
        course.getCourseSections().size(); // Khởi tạo collection
        
        return ResponseEntity.ok(course);
    }
}