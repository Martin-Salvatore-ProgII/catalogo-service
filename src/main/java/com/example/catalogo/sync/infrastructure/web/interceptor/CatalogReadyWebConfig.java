package com.example.catalogo.sync.infrastructure.web.interceptor;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Registra el interceptor para todos los endpoints: el propio interceptor deja pasar los que no
// llevan @RequiresCatalogReady, así que un endpoint nuevo solo tiene que declararlo.
@Configuration
@RequiredArgsConstructor
public class CatalogReadyWebConfig implements WebMvcConfigurer {

	private final CatalogReadyInterceptor catalogReadyInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(catalogReadyInterceptor);
	}

}
