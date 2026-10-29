package com.example.catalogo.sync.infrastructure.rest.adapter;

import com.example.catalogo.shared.infrastructure.rest.CatedraReadExecutor;
import com.example.catalogo.shared.infrastructure.rest.CatedraRequestException;
import com.example.catalogo.sync.application.exception.CatalogSnapshotException;
import com.example.catalogo.sync.domain.model.CatalogSnapshot;
import com.example.catalogo.sync.domain.ports.out.CatalogSnapshotPort;
import com.example.catalogo.sync.infrastructure.rest.dto.CatalogSnapshotResponse;
import com.example.catalogo.sync.infrastructure.rest.mapper.CatalogSnapshotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class CatedraSnapshotAdapter implements CatalogSnapshotPort {

	static final String SNAPSHOT_PATH = "/api/synchronization/snapshot";

	private final RestClient catedraRestClient;
	private final CatedraReadExecutor catedraReadExecutor;
	private final CatalogSnapshotMapper catalogSnapshotMapper;

	// El reintento y los mensajes de falla son los de toda lectura a la cátedra (ADR-0063). Acá
	// solo se pasan a la excepción de la feature, que es la que conoce el caso de uso.
	@Override
	public CatalogSnapshot getSnapshot() {
		CatalogSnapshotResponse response;
		try {
			response = catedraReadExecutor.read(() -> catedraRestClient.get()
					.uri(SNAPSHOT_PATH)
					.retrieve()
					.body(CatalogSnapshotResponse.class));
		} catch (CatedraRequestException e) {
			throw new CatalogSnapshotException(e.getMessage());
		}
		// Sin la versión o sin alguna de las tres colecciones no hay una copia completa que guardar
		// (REF §7). El contenido de cada entidad no se valida acá (RNF-09).
		if (response.getSnapshotVersion() == null || response.getProfessionalCategories() == null
				|| response.getProfessionals() == null || response.getWeeklySchedules() == null) {
			throw new CatalogSnapshotException("Unexpected response from the catedra: incomplete snapshot");
		}
		return catalogSnapshotMapper.toDomainModel(response);
	}

}
