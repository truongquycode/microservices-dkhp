package com.truongquycode.registration_service.repository;

import java.util.List; // SỬA: Dùng List
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock; // THÊM IMPORT
import org.springframework.data.jpa.repository.Query; // THÊM IMPORT
import org.springframework.stereotype.Repository;

import com.truongquycode.registration_service.model.Enrollment;

import jakarta.persistence.LockModeType; // THÊM IMPORT

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    /**
     * SỬA LẠI:
     * Thay vì dùng Optional (gây crash), chúng ta dùng List.
     * Thêm @Lock để khóa các hàng tìm thấy cho đến khi transaction kết thúc.
     * Bất kỳ thread nào khác cố gắng chạy query này cho CÙNG student/course sẽ phải CHỜ.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Enrollment e WHERE e.studentId = ?1 AND e.courseSectionId = ?2")
    List<Enrollment> findAndLockByStudentIdAndCourseSectionId(String studentId, String courseSectionId);

    // Phương thức này vẫn giữ nguyên cho frontend poll
    Optional<Enrollment> findByEventId(String eventId);
    
    // Phương thức cũ (không dùng nữa, có thể xóa)
    // Optional<Enrollment> findByStudentIdAndCourseSectionId(String studentId, String courseSectionId);
}