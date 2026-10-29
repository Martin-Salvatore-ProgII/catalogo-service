package com.example.catalogo.shared.infrastructure.config;

import java.util.List;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Reglas de acceso del servicio. Todo pedido exige un JWT de usuario válido, salvo lo declarado
 * público acá (RNF-04). El catálogo no tiene registro, inicio de sesión ni rutas administrativas.
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				// CSRF protege formularios de sitios que identifican al usuario con una cookie de
				// sesión. Acá no hay cookies ni sesión: cada pedido trae su token en un encabezado.
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				// Sin sesión en el servidor: el token es la única prueba de identidad.
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						// Lo único público: saber si el servicio está en pie.
						.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
						// El despacho interno de errores no es un pedido nuevo del cliente: si se
						// protegiera, un error ya resuelto se convertiría en un 401.
						.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
						// Protegido por defecto: cualquier ruta nueva nace exigiendo token.
						.anyRequest().authenticated())
				// Valida el JWT del encabezado Authorization con lo definido en JwtConfig.
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
		return http.build();
	}

	// CORS cerrado: la lista de orígenes permitidos es configurable y está vacía (ADR-0036).
	// La app Android y el servicio de turnos no son navegadores y no se ven afectados.
	@Bean
	public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(corsProperties.allowedOrigins() == null ? List.of() : corsProperties.allowedOrigins());
		configuration.setAllowedMethods(List.of("GET"));
		configuration.setAllowedHeaders(List.of("Authorization"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

}
