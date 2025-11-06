package com.truongquycode.registration_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

/**
 * Lớp này sẽ "bắt" các Exception xảy ra trong tất cả Controller
 * và chuyển đổi chúng thành các phản hồi HTTP (ResponseEntity) rõ ràng.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Bắt các lỗi 'IllegalStateException' (ví dụ: "Đã đăng ký rồi", "Hết chỗ")
     * và chuyển thành một lỗi HTTP 400 Bad Request.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalStateException(IllegalStateException ex) {
        // Trả về một object JSON, ví dụ: { "error": "Bạn đã đăng ký lớp học này rồi." }
        // Kèm theo mã lỗi 400 (BAD_REQUEST)
        return new ResponseEntity<>(
            Map.of("error", ex.getMessage()), 
            HttpStatus.BAD_REQUEST
        );
    }

    /**
     * Bắt các lỗi 'RuntimeException' khác (ví dụ: "Không tìm thấy mã lớp học")
     * và chuyển thành một lỗi HTTP 404 Not Found.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        // Trả về một object JSON, ví dụ: { "error": "Không tìm thấy mã lớp học: SEC_999" }
        // Kèm theo mã lỗi 404 (NOT_FOUND)
        return new ResponseEntity<>(
            Map.of("error", ex.getMessage()), 
            HttpStatus.NOT_FOUND
        );
    }
}