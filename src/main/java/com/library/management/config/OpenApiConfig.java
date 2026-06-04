package com.library.management.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI libraryOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Library Management System API")
                        .description("""
                                REST API for managing library books, members, loans, fines and reservations.
                                
                                **Key features:**
                                - Book catalog with search and pagination
                                - Member registration and status management
                                - Loan issuance, return and extension
                                - Automatic fine calculation for overdue loans
                                - Reservation queue for unavailable books
                                - Reports and statistics
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Library Team")
                                .email("library@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development server")
                ));
    }
}
