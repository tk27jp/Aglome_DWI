package pe.edu.utp.aglome_tienda.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;
import java.util.Arrays;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) -> ErrorJsonWriter.escribir(
                        request, response, HttpStatus.UNAUTHORIZED,
                        "Debes iniciar sesión para acceder a este recurso"))
                .accessDeniedHandler((request, response, e) -> ErrorJsonWriter.escribir(
                        request, response, HttpStatus.FORBIDDEN,
                        "No tienes permiso para realizar esta acción")))
        .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()    
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/v3/api-docs", "/v3/api-docs/**",
                        "/swagger-ui.html", "/swagger-ui/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/api/productos", "/api/productos/**",
                        "/api/categorias", "/api/categorias/**",
                        "/api/marcas", "/api/marcas/**").permitAll()
                // Operación diaria: la pueden hacer el administrador y el vendedor
                .requestMatchers("/api/admin/pedidos", "/api/admin/pedidos/**",
                        "/api/admin/stock", "/api/admin/stock/**").hasAnyRole("ADMIN", "VENDEDOR")
                // Todo lo demás del panel (catálogo, reportes, dashboard, usuarios): solo administrador
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // El front de Angular se sirve desde este mismo servidor: páginas y archivos son públicos.
                // (Todo lo que empieza con /api sigue protegido por las reglas de arriba y de abajo.)
                .requestMatchers(RegexRequestMatcher.regexMatcher(HttpMethod.GET, "^(?!/api/|/v3/|/swagger-ui).*"))
                        .permitAll()
                .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
    return provider;
}

    @Value("${cors.origenes:http://localhost:4200}")
private String origenesPermitidos;

@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(Arrays.stream(origenesPermitidos.split(","))
            .map(String::trim).toList());
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
}