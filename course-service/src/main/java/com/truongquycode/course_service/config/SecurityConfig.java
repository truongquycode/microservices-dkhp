package com.truongquycode.course_service.config; // Đặt package phù hợp

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // CHO PHÉP SỬ DỤNG @PreAuthorize
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // SINH VIÊN chỉ được phép xem (GET)
                .requestMatchers(HttpMethod.GET, "/api/courses/**", "/api/course-sections/**", "/api/db/course-sections/**").permitAll() 
                // Mọi request khác (POST, PUT, DELETE) yêu cầu phải được xác thực
                .anyRequest().authenticated()
            )
            // SỬA DÒNG NÀY:
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                .jwtAuthenticationConverter(jwtAuthenticationConverter()) // Sử dụng converter tùy chỉnh của chúng ta
            ));

        return http.build();
    }
    /**
     * Bean này "dạy" Spring Security cách đọc Client Roles (từ resource_access) 
     * của Keycloak thay vì tìm ở 'scope' (mặc định).
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        // Đây là converter sẽ trích xuất các role
        Converter<Jwt, Collection<GrantedAuthority>> grantedAuthoritiesConverter = jwt -> {
            // Lấy toàn bộ claim "resource_access"
            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            if (resourceAccess == null) {
                return Collections.emptyList();
            }

            // Lấy các role của client "student-app"
            Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get("student-app");
            if (clientAccess == null) {
                return Collections.emptyList();
            }

            // Lấy mảng "roles"
            Collection<String> roles = (Collection<String>) clientAccess.get("roles");
            if (roles == null) {
                return Collections.emptyList();
            }

            // Map mỗi role (ví dụ: "ADMIN") sang "ROLE_ADMIN"
            // (Spring Security tự động thêm tiền tố "ROLE_")
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        };

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtConverter;
    }
}