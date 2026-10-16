package com.example.catalogo.shared.infrastructure.web.catalog;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * El endpoint responde con la copia local del catálogo: mientras no haya versión local, responde
 * 503 {@code CATALOG_NOT_READY} sin llegar al controller (docs/arq/contratos.md).
 *
 * <p>Está en {@code shared} para que cualquier feature la use sin depender de {@code sync}, que es
 * la que sabe si el catálogo está listo y la hace cumplir.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresCatalogReady {
}
