package pe.edu.utp.aglome_tienda.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Activa el llenado automático de fechaCreacion y fechaActualizacion. */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
