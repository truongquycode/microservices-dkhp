package com.truongquycode.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // Import
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity; // Import
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration; // Import
import org.springframework.web.cors.reactive.CorsConfigurationSource; // Import
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays; // Import

@Configuration
@EnableWebFluxSecurity // (Thêm annotation này)
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            // 1. Kích hoạt CORS bằng cấu hình Bean bên dưới
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 2. Tắt CSRF
            .csrf(csrf -> csrf.disable())

            // 3. Cấu hình bảo vệ đường dẫn
            .authorizeExchange(exchanges -> exchanges
                // (QUAN TRỌNG) Cho phép OPTIONS đi qua
                .pathMatchers(HttpMethod.OPTIONS).permitAll() 
                .pathMatchers("/eureka/**").permitAll() 
                .pathMatchers("/api/auth/**").permitAll() // Cho phép API đăng nhập
                .anyExchange().authenticated() // Mọi thứ khác cần xác thực
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        
        return http.build();
    }

    /**
     * Bean cấu hình CORS, thay thế cho file application.properties
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Cho phép React app
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        
        // Cho phép các phương thức này
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Cho phép mọi header (bao gồm Authorization và Content-Type)
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Cho phép gửi thông tin (cookie, token)
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Áp dụng cấu hình này cho mọi đường dẫn ("/**")
        source.registerCorsConfiguration("/**", configuration); 
        
        return source;
    }
}
