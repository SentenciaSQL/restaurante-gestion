package com.restosuite.config;

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
    OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fogón API")
                        .version("0.1.0")
                        .description("API de gestión de restaurantes. La facturación electrónica y los cobros con pasarelas externas no están implementados: los comprobantes son internos y los pagos se registran de forma manual."))
                .addSecurityItem(new SecurityRequirement().addList("bearer"))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().name("bearer").type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
