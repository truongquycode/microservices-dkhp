package com.truongquycode.registration_service.config;

public class TopicConstants {
    
    public static final String REGISTRATION_REQUESTS = "registration_requests";
    public static final String STUDENT_VALIDATED = "student_validated";
    public static final String REGISTRATION_RESULTS = "registration_results";

    // --- MỚI: Thêm 2 topic này ---
    public static final String REGISTRATION_CANCELS = "registration_cancels";       // Topic nhận yêu cầu hủy
    public static final String REGISTRATION_CANCELLED = "registration_cancelled";   // Topic thông báo đã hủy thành công (để CourseService trừ sỉ số)
}