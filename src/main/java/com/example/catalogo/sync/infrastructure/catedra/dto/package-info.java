/**
 * Entidades del catálogo tal como las publica la cátedra en JSON. Son las mismas en el snapshot
 * REST (REF §7) y en los hashes de Redis (REF §14.4), por eso las comparten los dos adaptadores.
 * Ignoran los campos desconocidos. Nunca salen del adaptador: el mapper las convierte en modelos de
 * dominio.
 */
package com.example.catalogo.sync.infrastructure.catedra.dto;
