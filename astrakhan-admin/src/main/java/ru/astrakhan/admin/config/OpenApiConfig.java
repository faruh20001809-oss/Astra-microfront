package ru.astrakhan.admin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 / Swagger UI для всех {@code @RestController} приложения.
 * UI: {@code /swagger-ui.html} (в проде с context-path: {@code /java-api/swagger-ui.html}).
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Bean
    public OpenAPI astrakhanOpenAPI() {
        String base = contextPath == null || contextPath.isBlank() ? "" : contextPath;
        return new OpenAPI()
                .info(new Info()
                        .title("Астрахань. Живая История — REST API")
                        .version("1.0.0")
                        .description("""
                                Документация публичного API для module2 (`/api/v1/**`) \
                                и служебных JSON-эндпоинтов админки (`/admin/api/**`, оплата).
                                Ответы публичного API: `{ "status": "success", "data": ... }`.""")
                        .contact(new Contact().name("Astrakhan History Admin")))
                .addServersItem(new Server().url(base.isEmpty() ? "/" : base).description("Текущий хост"));
    }

    @Bean
    public GroupedOpenApi publicApiV1() {
        return GroupedOpenApi.builder()
                .group("01-public-v1")
                .displayName("Публичный API v1 (module2)")
                .pathsToMatch("/api/v1/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminJsonApi() {
        return GroupedOpenApi.builder()
                .group("02-admin-json")
                .displayName("Админка — JSON API")
                .pathsToMatch("/admin/api/**")
                .build();
    }

    @Bean
    public GroupedOpenApi allRestApi() {
        return GroupedOpenApi.builder()
                .group("00-all")
                .displayName("Все REST-эндпоинты")
                .pathsToMatch("/api/**", "/admin/api/**")
                .build();
    }
}
