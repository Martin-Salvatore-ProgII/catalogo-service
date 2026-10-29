package com.example.catalogo.sync.application.usecases;

import com.example.catalogo.sync.domain.ports.in.IsCatalogReadyUseCase;
import com.example.catalogo.sync.domain.ports.out.SyncStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// El catálogo está listo cuando hay una versión aplicada por completo. Mientras sincroniza, la
// copia anterior sigue siendo válida (ADR-0010): una sincronización en curso no lo hace "no listo".
@Component
@RequiredArgsConstructor
public class IsCatalogReadyUseCaseImpl implements IsCatalogReadyUseCase {

	private final SyncStateRepository syncStateRepository;

	@Override
	public boolean isCatalogReady() {
		return syncStateRepository.getCurrent().getLocalVersion() != null;
	}

}
