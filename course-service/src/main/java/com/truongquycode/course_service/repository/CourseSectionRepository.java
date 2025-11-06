package com.truongquycode.course_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.truongquycode.course_service.model.CourseSection;

public interface CourseSectionRepository extends JpaRepository<CourseSection, String> {
	
    // KHÔNG CẦN LOCK NỮA!
    // @Lock(LockModeType.PESSIMISTIC_WRITE)
    // Optional<CourseSection> findAndLockBySectionId(String sectionId);

    // Chỉ cần phương thức tìm kiếm bình thường
    Optional<CourseSection> findBySectionId(String sectionId);
    
    @Modifying
    @Query("UPDATE CourseSection c SET c.registeredSlots = :slots, c.lastUpdatedAt = :ts WHERE c.sectionId = :id AND (c.lastUpdatedAt IS NULL OR c.lastUpdatedAt <= :ts)")
    int updateIfNewer(@Param("id") String id, @Param("slots") int slots, @Param("ts") Long ts);
    
    @Modifying
    @Query(value = "UPDATE CourseSection " +
                   "SET registeredSlots = LEAST(totalslots, registeredslots + :inc), lastUpdatedAt = :ts " +
                   "WHERE sectionId = :id", nativeQuery = true)
    int incrementRegisteredSlots(@Param("id") String id, @Param("inc") int inc, @Param("ts") Long ts);

}