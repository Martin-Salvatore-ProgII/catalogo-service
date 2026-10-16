/**
 * Adaptadores REST hacia la cátedra: implementan un puerto de salida con el cliente
 * {@code catedraRestClient} y las reglas comunes de lectura de {@code shared} (ADR-0063).
 *
 * <p>Convierten cualquier falla en una excepción de aplicación de la feature, con un mensaje
 * seguro. Sin lógica de negocio.
 */
package com.example.catalogo.sync.infrastructure.rest.adapter;
