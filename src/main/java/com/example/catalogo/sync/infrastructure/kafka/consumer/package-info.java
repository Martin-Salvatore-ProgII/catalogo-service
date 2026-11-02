/**
 * Adaptador de entrada que consume los avisos de la cátedra por Kafka. No decide nada: lee el
 * aviso, se lo pasa a la fachada y confirma el offset.
 */
package com.example.catalogo.sync.infrastructure.kafka.consumer;
