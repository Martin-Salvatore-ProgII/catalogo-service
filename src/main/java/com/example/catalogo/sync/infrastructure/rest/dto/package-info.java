/**
 * Respuestas de la API de la cátedra tal como llegan por JSON (REF §7). Ignoran los campos
 * desconocidos, que la cátedra puede agregar sin cambiar de versión. Nunca salen del adaptador:
 * el mapper las convierte en modelos de dominio.
 */
package com.example.catalogo.sync.infrastructure.rest.dto;
