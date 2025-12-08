package com.truongquycode.identity_service.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakConfig {

    @Value("${keycloak.auth-server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    @Bean
    public Keycloak keycloak() {
        // Cấu hình JSON để BỎ QUA lỗi "Unrecognized field"
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        ResteasyJackson2Provider provider = new ResteasyJackson2Provider();
        provider.setMapper(mapper);

        // Tạo ResteasyClientBuilder bằng cách ép kiểu từ ClientBuilder chuẩn
        // Đây là bước quan trọng để dùng được hàm connectionPoolSize
        ResteasyClientBuilder resteasyClientBuilder = (ResteasyClientBuilder) ClientBuilder.newBuilder();
        
        //Cấu hình Client
        resteasyClientBuilder.connectionPoolSize(10);
        resteasyClientBuilder.register(provider); // Đăng ký bộ xử lý JSON đã fix lỗi
        
        Client client = resteasyClientBuilder.build();

        // Tạo Keycloak Admin Client
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .resteasyClient(client)
                .build();
    }
}