package pe.edu.utp.aglome_tienda.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación Swagger de la API.
 * UI: http://localhost:8080/swagger-ui.html  |  JSON: http://localhost:8080/v3/api-docs
 * Para probar rutas protegidas: hacer login, copiar el token y pulsar "Authorize".
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI aglomeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Aglome Tienda API")
                        .version("0.0.1")
                        .description("API REST de la tienda Aglome: catálogo, carrito, pedidos y panel de administración."))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }
}
