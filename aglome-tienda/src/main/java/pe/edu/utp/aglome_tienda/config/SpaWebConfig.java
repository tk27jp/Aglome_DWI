package pe.edu.utp.aglome_tienda.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Sirve el front de Angular desde este mismo servidor (carpeta src/main/resources/static).
 *
 * Angular maneja sus propias rutas (/productos, /productos/123...). Si el usuario recarga la página
 * en una de ellas, el servidor no tiene ese archivo: por eso, cuando la ruta no es un archivo real,
 * se devuelve index.html y Angular se encarga de mostrar la pantalla correcta.
 * Las rutas de la API (/api/**) y de la documentación nunca caen aquí.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String rutaRecurso, Resource ubicacion) throws IOException {
                        Resource pedido = ubicacion.createRelative(rutaRecurso);
                        if (pedido.exists() && pedido.isReadable()) {
                            return pedido;
                        }
                        if (esRutaDeApi(rutaRecurso) || rutaRecurso.contains(".")) {
                            return null; // un archivo que no existe (o la API) debe dar 404, no la página principal
                        }
                        Resource indice = new ClassPathResource("static/index.html");
                        return indice.exists() ? indice : null; // sin front compilado: 404 normal
                    }
                });
    }

    private boolean esRutaDeApi(String ruta) {
        return ruta.startsWith("api/") || ruta.equals("api") || ruta.startsWith("v3/") || ruta.startsWith("swagger-ui");
    }
}
