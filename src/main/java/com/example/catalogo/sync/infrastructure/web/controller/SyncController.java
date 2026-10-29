package com.example.catalogo.sync.infrastructure.web.controller;

import com.example.catalogo.sync.application.service.SyncService;
import com.example.catalogo.sync.infrastructure.web.dto.SyncStatusResponse;
import com.example.catalogo.sync.infrastructure.web.mapper.SyncStatusDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/sync-status")
@RequiredArgsConstructor
public class SyncController {

	private final SyncService syncService;
	private final SyncStatusDtoMapper syncStatusDtoMapper;

	// Sin @RequiresCatalogReady: es justamente donde se ve que el catálogo todavía no está listo.
	@GetMapping
	public ResponseEntity<SyncStatusResponse> getSyncStatus() {
		return ResponseEntity.ok(syncStatusDtoMapper.toResponse(syncService.getSyncStatus()));
	}

}
