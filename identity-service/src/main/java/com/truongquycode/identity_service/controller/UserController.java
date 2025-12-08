package com.truongquycode.identity_service.controller;

import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.truongquycode.identity_service.service.KeycloakAdminService;
@RestController
@RequestMapping("/api/users")
public class UserController {
	@Autowired
    private KeycloakAdminService keycloakAdminService;

    @GetMapping("/me")
    public ResponseEntity<UserRepresentation> getMyInfo() {
        // Lấy ID của user đang đăng nhập từ Token
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        // Ở đây `getName()` thường trả về ID (UUID) trong JWT resource server
        // Nếu config trả về username, bạn cần tìm user theo username
        
        // Cách an toàn nhất với JWT Authentication Token:
        JwtAuthenticationToken token = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        String uuid = token.getToken().getSubject(); // Lấy 'sub' claim (UUID của user)

        return ResponseEntity.ok(keycloakAdminService.getUserById(uuid));
    }
}
