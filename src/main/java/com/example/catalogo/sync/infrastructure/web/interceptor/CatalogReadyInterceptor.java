package com.example.catalogo.sync.infrastructure.web.interceptor;

import com.example.catalogo.shared.infrastructure.web.catalog.RequiresCatalogReady;
import com.example.catalogo.sync.application.service.SyncService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

// Hace cumplir @RequiresCatalogReady antes de entrar al controller. Si el catálogo no está listo,
// la excepción la traduce a 503 el GlobalExceptionHandler, como cualquier otra.
@Component
@RequiredArgsConstructor
public class CatalogReadyInterceptor implements HandlerInterceptor {

	private final SyncService syncService;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (handler instanceof HandlerMethod method && method.hasMethodAnnotation(RequiresCatalogReady.class)) {
			syncService.requireCatalogReady();
		}
		return true;
	}

}
