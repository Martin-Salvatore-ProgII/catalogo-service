package com.example.catalogo.sync.domain.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Los ids de las entidades que cambiaron en una versión (REF §14.5). No trae las entidades: su
// estado actual se lee aparte.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionChanges {
	private long version;
	private List<Long> categoryIds;
	private List<Long> professionalIds;
	private List<Long> weeklyScheduleIds;
}
