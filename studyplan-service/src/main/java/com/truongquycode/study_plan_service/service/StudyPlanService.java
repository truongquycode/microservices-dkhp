package com.truongquycode.study_plan_service.service;

import com.truongquycode.study_plan_service.dto.AddCoursePlanDto;
import com.truongquycode.study_plan_service.dto.Course;
import com.truongquycode.study_plan_service.model.PlanEntry;
import com.truongquycode.study_plan_service.repository.PlanEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.Month;
@Service
@RequiredArgsConstructor
public class StudyPlanService {

    private final PlanEntryRepository planEntryRepository;
    private final RestTemplate restTemplate;

    @Value("${plan.max-credits-per-semester:20}")
    private int MAX_CREDITS;
    @Value("${services.course.url}")
    private String COURSE_SERVICE_URL;


    public List<PlanEntry> getStudentPlan(String studentId) {
        return planEntryRepository.findByStudentId(studentId);
    }

    public PlanEntry addCourseToPlan(String studentId, AddCoursePlanDto dto) {
        
        // Sử dụng lại hàm kiểm tra 2 tham số
        if (planEntryRepository.existsByStudentIdAndCourseId(studentId, dto.getCourseId())) {
            throw new RuntimeException("Đã xếp môn này vào kế hoạch học tập.");
        }
        
        String url = COURSE_SERVICE_URL + "/api/courses/" + dto.getCourseId() + "/details";
        Course course = restTemplate.getForObject(url, Course.class); 
        if (course == null) {
            throw new RuntimeException("Không tìm thấy thông tin môn học.");
        }
        int newCredits = course.getCredits();

        Integer currentCreditsObj = planEntryRepository.sumCreditsBySemester(
            studentId, dto.getAcademicYear(), dto.getSemester()
        );
        int currentCredits = (currentCreditsObj == null) ? 0 : currentCreditsObj;
        if (currentCredits + newCredits > MAX_CREDITS) {
            throw new RuntimeException("Vượt quá 20 tín chỉ cho học kỳ này.");
        }

        PlanEntry newEntry = new PlanEntry(
            null, studentId, dto.getCourseId(), newCredits,
            dto.getAcademicYear(), dto.getSemester()
        );
        return planEntryRepository.save(newEntry);
    }

    public void removeCourseFromPlan(String studentId, Long planEntryId) {
        PlanEntry entry = planEntryRepository.findById(planEntryId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy mục kế hoạch."));
        
        if (!entry.getStudentId().equals(studentId)) {
            throw new RuntimeException("Không có quyền xóa.");
        }
        planEntryRepository.delete(entry);
    }

    public List<Course> getRegistrableCourses(String studentId) {

        // Lấy năm học và học kỳ hiện tại TỰ ĐỘNG
        RegistrationPeriod currentPeriod = getCurrentRegistrationPeriod();
        int regYear = currentPeriod.year;
        int regSemester = currentPeriod.semester;

        // Lấy các môn đã lên kế hoạch cho kỳ đăng ký hiện tại (dùng biến động)
        List<PlanEntry> planForCurrentSemester = planEntryRepository.findByStudentIdAndAcademicYearAndSemester(
            studentId, regYear, regSemester
        );

        // Lấy chi tiết từng môn học (dùng vòng lặp for thay vì .stream())
        List<Course> coursesToRegister = new ArrayList<>();

        for (PlanEntry entry : planForCurrentSemester) {
            String url = COURSE_SERVICE_URL + "/api/courses/" + entry.getCourseId() + "/details";
            Course course = restTemplate.getForObject(url, Course.class);
            if (course != null) {
                coursesToRegister.add(course);
            }
        }

        return coursesToRegister;
    }
    
    private static class RegistrationPeriod {
        final int year;
        final int semester;

        RegistrationPeriod(int year, int semester) {
            this.year = year;
            this.semester = semester;
        }
    }
    
    /**
     * Tính toán năm học & học kỳ hiện tại dựa trên ngày thật.
     * BẠN CẦN THAY ĐỔI LOGIC NÀY CHO PHÙ HỢP VỚI QUY ĐỊNH CỦA TRƯỜNG.
     */
    private RegistrationPeriod getCurrentRegistrationPeriod() {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        Month currentMonth = today.getMonth();

        int academicYear;
        int semester;

        // ==================================================
        // === VÍ DỤ LOGIC NGHIỆP VỤ ===
        // (Giả sử năm học X bắt đầu từ 1/8/X)
        // * HK1 (Năm X): 1/8/X -> 31/12/X
        // * HK2 (Năm X): 1/1/(X+1) -> 31/5/(X+1)
        // * HK3 (Năm X): 1/6/(X+1) -> 31/7/(X+1)
        // ==================================================

        if (currentMonth.getValue() >= Month.AUGUST.getValue()) {
            // Từ tháng 8 -> 12: Là HK1 của năm học HIỆN TẠI
            // Ví dụ: 10/2025 -> Năm học 2025, HK1
            academicYear = currentYear;
            semester = 1;
        } else if (currentMonth.getValue() >= Month.JUNE.getValue()) {
            // Từ tháng 6 -> 7: Là HK3 (Hè) của năm học TRƯỚC
            // Ví dụ: 6/2025 -> Năm học 2024, HK3
            academicYear = currentYear - 1;
            semester = 3;
        } else {
            // Từ tháng 1 -> 5: Là HK2 của năm học TRƯỚC
            // Ví dụ: 2/2025 -> Năm học 2024, HK2
            academicYear = currentYear - 1;
            semester = 2;
        }

        return new RegistrationPeriod(academicYear, semester);
    }
}



