package com.truongquycode.identity_service.service;

import com.truongquycode.identity_service.dto.PasswordResetRequest;
import com.truongquycode.identity_service.dto.UserUpdateRequest;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KeycloakAdminService {

    @Autowired
    private Keycloak keycloak;
    @Autowired
    private EmailService emailService;

    @Value("${keycloak.realm}")
    private String realm;

    // Lấy danh sách tất cả sinh viên (ĐÃ FIX LỖI NULL ATTRIBUTES)
    public List<UserRepresentation> getAllUsers() {
        UsersResource usersResource = getUsersResource();
        // Lấy danh sách user cơ bản
        List<UserRepresentation> basicUsers = usersResource.list();
        
        List<UserRepresentation> fullUsers = new ArrayList<>();

        for (UserRepresentation basicUser : basicUsers) {
            try {
                // QUAN TRỌNG: Lấy UserResource theo ID, sau đó lấy UserRepresentation
                // Cách này ép Keycloak trả về toàn bộ thông tin chi tiết, bao gồm attributes
                UserRepresentation fullUser = usersResource.get(basicUser.getId()).toRepresentation();
                fullUsers.add(fullUser);
            } catch (Exception e) {
                // Nếu lỗi khi lấy chi tiết, dùng tạm thông tin cơ bản
                fullUsers.add(basicUser);
                System.err.println("Error fetching details for user " + basicUser.getUsername() + ": " + e.getMessage());
            }
        }
        return fullUsers;
    }

    // Lấy chi tiết 1 sinh viên
    public UserRepresentation getUserById(String id) {
        return getUsersResource().get(id).toRepresentation();
    }

    // Cập nhật thông tin sinh viên
    public void updateUser(String id, UserUpdateRequest request) {
        UserResource userResource = getUsersResource().get(id);
        UserRepresentation user = userResource.toRepresentation();
        if(request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if(request.getLastName() != null) user.setLastName(request.getLastName());
        if(request.getEmail() != null) user.setEmail(request.getEmail());

        // Xử lý Attributes
        Map<String, List<String>> currentAttrs = user.getAttributes();
        if (currentAttrs == null) {
            currentAttrs = new HashMap<>();
        } else {
            currentAttrs = new HashMap<>(currentAttrs);
        }

        // Merge các attributes từ request.getAttributes() (lớp, ngành, khoa, ngày sinh...)
        if (request.getAttributes() != null) {
            currentAttrs.putAll(request.getAttributes());
        }

        // Xử lý riêng cho GENDER (vì frontend gửi riêng)
        if (request.getGender() != null) {
            // Keycloak lưu attribute dưới dạng List
            currentAttrs.put("gender", List.of(request.getGender()));
        }
        
        user.setAttributes(currentAttrs);
        userResource.update(user);
    }

 // Đổi mật khẩu và GỬI MAIL
    public void resetPassword(String id, PasswordResetRequest request) {
        // Thực hiện đổi pass trên Keycloak
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.getValue());
        credential.setTemporary(request.isTemporary());

        UserResource userResource = getUsersResource().get(id);
        userResource.resetPassword(credential);
        
        // Lấy thông tin user để biết email và username
        UserRepresentation user = userResource.toRepresentation();
        String email = user.getEmail();
        String username = user.getUsername();

        // Gửi email thông báo (nếu user có email)
        if (email != null && !email.isEmpty()) {
            emailService.sendPasswordResetNotification(email, username, request.getValue());
        } else {
            System.out.println("User " + username + " does not have an email address. Skipped sending email.");
        }
    }

    private UsersResource getUsersResource() {
        return keycloak.realm(realm).users();
    }
}