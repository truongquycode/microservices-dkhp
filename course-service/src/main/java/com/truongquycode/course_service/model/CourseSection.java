package com.truongquycode.course_service.model;

import java.util.List;
import java.util.ArrayList;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "course_sections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseSection {
    @Id
    private String sectionId;

    @ManyToOne
    @JoinColumn(name = "course_id")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Course course;

    private String groupName;
    private String instructorName;
    private int totalSlots;
    private int registeredSlots;
    private Long lastUpdatedAt; 
    @Column(nullable = false)
    private int academicYear;

    @Column(nullable = false)
    private int semester;
    @OneToMany(
        mappedBy = "courseSection", 
        cascade = CascadeType.ALL, 
        orphanRemoval = true,
        fetch = FetchType.EAGER // LỖI 500
    )
    private List<ScheduleEntry> scheduleEntries = new ArrayList<>(); // <-- Khởi tạo
    /**
     * Phương thức này chỉ trả về 'courseId' cho JSON,
     * thay vì toàn bộ đối tượng 'course' (gây ra vòng lặp).
     * Nó cho phép frontend (AdminPage.js) biết section này thuộc course nào.
     */
    @JsonProperty("courseId") // <-- Báo cho Jackson gọi hàm này và đặt tên key là "courseId"
    public String getCourseIdForJson() {
        return (this.course != null) ? this.course.getCourseId() : null;
    }
}