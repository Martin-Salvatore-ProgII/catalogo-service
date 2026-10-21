package com.example.catalogo.sync.infrastructure.web.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import com.example.catalogo.sync.application.exception.CatalogNotReadyException;
import com.example.catalogo.sync.application.service.SyncService;
import com.example.catalogo.sync.domain.model.SyncState;
import com.example.catalogo.sync.domain.model.SyncType;
import com.example.catalogo.sync.infrastructure.web.mapper.SyncStatusDtoMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

// Sin los filtros de seguridad: acá se prueba el controller. Las reglas de acceso se prueban aparte.
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(SyncController.class)
@Import(SyncStatusDtoMapper.class)
class SyncControllerTest {

	private static final String SYNC_STATUS = "/api/catalog/sync-status";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SyncService syncService;

	@Test
	void returnsEveryFieldOfTheSyncStatus() throws Exception {
		when(syncService.getSyncStatus()).thenReturn(SyncState.builder()
				.localVersion(7L)
				.syncInProgress(false)
				.lastSuccessAt(Instant.parse("2026-09-24T13:45:00Z"))
				.lastSuccessType(SyncType.INCREMENTAL)
				.lastErrorAt(Instant.parse("2026-09-24T13:40:00Z"))
				.lastError("Catedra service unavailable (HttpTimeoutException)")
				.build());

		mockMvc.perform(get(SYNC_STATUS))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.localVersion").value(7))
				.andExpect(jsonPath("$.syncInProgress").value(false))
				.andExpect(jsonPath("$.lastSuccessAt").value("2026-09-24T13:45:00Z"))
				.andExpect(jsonPath("$.lastSuccessType").value("INCREMENTAL"))
				.andExpect(jsonPath("$.lastErrorAt").value("2026-09-24T13:40:00Z"))
				.andExpect(jsonPath("$.lastError").value("Catedra service unavailable (HttpTimeoutException)"));
	}

	@Test
	void returnsTheEmptyFieldsAsNullBeforeTheFirstSync() throws Exception {
		when(syncService.getSyncStatus()).thenReturn(SyncState.builder().syncInProgress(true).build());

		// Presentes y en null, como en el contrato: no se omiten. La comparación del JSON falla si
		// alguna clave falta.
		mockMvc.perform(get(SYNC_STATUS))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{ "localVersion": null, "syncInProgress": true, "lastSuccessAt": null,
						  "lastSuccessType": null, "lastErrorAt": null, "lastError": null }
						""", JsonCompareMode.STRICT));
	}

	@Test
	void answersEvenWhenTheCatalogIsNotReady() throws Exception {
		when(syncService.getSyncStatus()).thenReturn(SyncState.builder().build());
		// Si el endpoint llevara @RequiresCatalogReady, este mock haría responder 503.
		doThrow(new CatalogNotReadyException()).when(syncService).requireCatalogReady();

		mockMvc.perform(get(SYNC_STATUS)).andExpect(status().isOk());
	}

}
