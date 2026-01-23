package hskl.cn.serverless.registry.config;

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
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Serverless Function Runner - Registry API")
                        .version("1.0.0")
                        .description("""
                                REST API für die Verwaltung von Serverless Functions.
                                
                                ## Features
                                - Function Registration und Verwaltung
                                - JAR-Upload zu MinIO Object Storage
                                - Lifecycle Management (PENDING → READY)
                                
                                ## 12-Factor App Compliance
                                Diese API folgt den 12-Factor App Prinzipien für Cloud-Native Anwendungen.
                                """)
                        .contact(new Contact()
                                .name("Hochschule Kaiserslautern")
                                .url("https://www.hs-kl.de"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("http://localhost:8082")
                                .description("Via API Gateway")
                ));
    }
}
