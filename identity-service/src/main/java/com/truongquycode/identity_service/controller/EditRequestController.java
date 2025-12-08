package com.truongquycode.identity_service.controller;

import com.truongquycode.identity_service.dto.EditRequestDTO;
import com.truongquycode.identity_service.entity.EditRequest;
import com.truongquycode.identity_service.repository.EditRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/edit-requests")
public class EditRequestController {

    @Autowired
    private EditRequestRepository editRequestRepository;

    // API cho Sinh viên gửi yêu cầu
    @PostMapping
    public ResponseEntity<?> createRequest(@RequestBody EditRequestDTO dto) {
        // Lấy ID user từ Token hiện tại
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        
        EditRequest req = new EditRequest();
        req.setStudentId(username);
        req.setRequestDetails(dto.getRequestDetails());
        editRequestRepository.save(req);
        
        return ResponseEntity.ok("Request sent successfully");
    }

    // API cho Admin xem danh sách yêu cầu
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EditRequest>> getAllRequests() {
        return ResponseEntity.ok(editRequestRepository.findAllByOrderByCreatedAtDesc());
    }
    
 // Trong file EditRequestController.java

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteRequest(@PathVariable Long id) {
        editRequestRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa yêu cầu."));
    }
}