package com.truongquycode.study_plan_service.controller;

import com.truongquycode.study_plan_service.dto.AddCoursePlanDto;
import com.truongquycode.study_plan_service.dto.Course;
import com.truongquycode.study_plan_service.dto.CourseSection; // Import thêm CourseSection
import com.truongquycode.study_plan_service.model.PlanEntry;
import com.truongquycode.study_plan_service.service.StudyPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional; // Import Transactional
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    // Lấy toàn bộ kế hoạch (cho trang StudyPlanPage.js)
    @GetMapping("/me")
    public ResponseEntity<List<PlanEntry>> getMyPlan(@AuthenticationPrincipal Jwt jwt) {
        String studentId = jwt.getSubject();
        return ResponseEntity.ok(studyPlanService.getStudentPlan(studentId));
    }

    // Thêm một môn vào kế hoạch
    @PostMapping("/me/courses")
    public ResponseEntity<?> addCourseToMyPlan(
            @AuthenticationPrincipal Jwt jwt, 
            @RequestBody AddCoursePlanDto dto) {
        String studentId = jwt.getSubject();
        // Xử lý lỗi nếu service ném ra exception
        try {
            PlanEntry savedEntry = studyPlanService.addCourseToPlan(studentId, dto);
            return ResponseEntity.ok(savedEntry);
            
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            return ResponseEntity.badRequest().body(Map.of("message", errorMessage));
        }
    }

    // Xóa một môn khỏi kế hoạch
    @DeleteMapping("/me/courses/{planEntryId}")
    public ResponseEntity<Void> removeCourseFromMyPlan(
            @AuthenticationPrincipal Jwt jwt, 
            @PathVariable Long planEntryId) {
        String studentId = jwt.getSubject();
        studyPlanService.removeCourseFromPlan(studentId, planEntryId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/registrable-courses")
    @Transactional(readOnly = true) // Giữ kết nối Database mở
    public ResponseEntity<List<Course>> getMyRegistrableCourses(@AuthenticationPrincipal Jwt jwt) {
        String studentId = jwt.getSubject();
        
        List<Course> courses = studyPlanService.getRegistrableCourses(studentId);

        // Duyệt qua danh sách để ép Hibernate tải dữ liệu chi tiết (Lazy Loading Fix)
        if (courses != null) {
            for (Course course : courses) {
                // Tải danh sách lớp (Sections)
                if (course.getCourseSections() != null) {
                    course.getCourseSections().size(); 

                    for (CourseSection section : course.getCourseSections()) {
                        // Tải danh sách lịch học (ScheduleEntries) - QUAN TRỌNG NHẤT
                        if (section.getScheduleEntries() != null) {
                            section.getScheduleEntries().size(); 
                        }
                    }
                }
            }
        }

        return ResponseEntity.ok(courses);
    }
}