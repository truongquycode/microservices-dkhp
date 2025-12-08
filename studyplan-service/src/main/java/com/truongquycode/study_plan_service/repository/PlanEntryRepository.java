package com.truongquycode.study_plan_service.repository;

import com.truongquycode.study_plan_service.model.PlanEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanEntryRepository extends JpaRepository<PlanEntry, Long> {

    // Lấy toàn bộ kế hoạch của một sinh viên
    List<PlanEntry> findByStudentId(String studentId);

    // Lấy các môn trong một học kỳ cụ thể của sinh viên
    List<PlanEntry> findByStudentIdAndAcademicYearAndSemester(String studentId, int academicYear, int semester);

    // Tính tổng số tín chỉ đã đăng ký cho một học kỳ
    @Query("SELECT COALESCE(SUM(p.credits), 0) FROM PlanEntry p " +
           "WHERE p.studentId = :studentId " +
           "AND p.academicYear = :year " +
           "AND p.semester = :semester")
    Integer sumCreditsBySemester(
        @Param("studentId") String studentId, 
        @Param("year") int academicYear, 
        @Param("semester") int semester
    );

    // Kiểm tra xem môn học đã tồn tại trong kế hoạch chưa
    boolean existsByStudentIdAndCourseId(String studentId, String courseId);
    boolean existsByStudentIdAndCourseIdAndAcademicYearAndSemester(
            String studentId, String courseId, int academicYear, int semester
        );
}