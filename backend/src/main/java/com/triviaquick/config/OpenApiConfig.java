package com.triviaquick.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI triviaQuickOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TriviaQuick API")
                        .version("1.0.0")
                        .description("""
                                API de partidas en vivo, minijuegos y clasificación de TriviaQuick.
                                Las puntuaciones las calcula siempre el servidor. La autenticación usa una cookie
                                httpOnly (navegador) o la cabecera `Authorization: Bearer <jwt>` (clientes de API).
                                Las peticiones que modifican estado con cookie requieren `X-Requested-With: TriviaQuick`.
                                """))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
