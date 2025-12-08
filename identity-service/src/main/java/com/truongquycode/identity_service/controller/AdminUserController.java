package com.truongquycode.identity_service.controller;

import com.truongquycode.identity_service.dto.PasswordResetRequest;
import com.truongquycode.identity_service.dto.UserUpdateRequest;
import com.truongquycode.identity_service.service.KeycloakAdminService;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    @Autowired
    private KeycloakAdminService keycloakAdminService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserRepresentation>> getAllUsers() {
        return ResponseEntity.ok(keycloakAdminService.getAllUsers());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable String id, @RequestBody UserUpdateRequest request) {
        keycloakAdminService.updateUser(id, request);
        return ResponseEntity.ok(Map.of("message", "Cập nhật thông tin thành công!"));
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resetPassword(@PathVariable String id, @RequestBody PasswordResetRequest request) {
        keycloakAdminService.resetPassword(id, request);
        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công!"));
    }
}