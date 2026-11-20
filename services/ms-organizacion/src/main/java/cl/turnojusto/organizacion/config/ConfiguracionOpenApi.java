package cl.turnojusto.organizacion.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Contrato OpenAPI 3 publicado en /v3/api-docs y Swagger UI en /swagger-ui.html. */
@Configuration
public class ConfiguracionOpenApi {

    @Bean
    OpenAPI contrato() {
        return new OpenAPI()
                .info(new Info().title("TurnoJusto · ms-organizacion").version("v1")
                        .description("Empresa, sucursales, equipos y coordinadores."))
                .components(new Components().addSecuritySchemes("entra", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                        .description("Token de acceso emitido por Microsoft Entra External ID")));
    }
}
