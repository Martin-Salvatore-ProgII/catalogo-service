/**
 * Puertos de salida: lo que la feature necesita del exterior (base de datos, Redis, REST de la
 * cátedra), expresado en tipos de dominio.
 *
 * <p>Nombre {@code <Feature>Repository} para persistencia y {@code <Cosa>Port} para el resto. Sin
 * tipos de JPA, Jackson, Redis ni HTTP en las firmas. Los implementa un adaptador en
 * {@code infrastructure}.
 */
package com.example.catalogo.category.domain.ports.out;
