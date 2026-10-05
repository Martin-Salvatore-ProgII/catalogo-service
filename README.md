# catalogo-service

Servicio de catálogo y sincronización del proyecto integrador 2026. Guarda la copia local del catálogo de profesionales que publica la cátedra y responde las búsquedas con esos datos.

Es uno de los tres repositorios de la entrega, junto con `turnos-service` y `app-kmp`. Los requisitos, la arquitectura, los contratos y las decisiones (ADR) están en el repositorio [`docs`](https://github.com/Martin-Salvatore-ProgII/docs).

## Estado

Esqueleto del servicio: arranca, se conecta a su base PostgreSQL, ejecuta las migraciones de Flyway y responde los errores en `application/problem+json`. Todavía no tiene endpoints de negocio ni sincronización.

## Requisitos

- **Docker** con Docker Compose. Alcanza para levantar el servicio y para correr las pruebas.
- **Java 25**, solo para compilar o correr las pruebas fuera de Docker. Gradle no hace falta instalarlo: se usa el wrapper (`./gradlew`).

## Configuración

La configuración sale de variables de entorno. Para Docker Compose se leen de un archivo `.env`, que no se commitea:

```
cp .env.example .env
```

| Variable | Para qué | Valor de ejemplo |
| --- | --- | --- |
| `DB_NAME` | Nombre de la base del catálogo | `catalogo` |
| `DB_USER` | Usuario de la base | `catalogo` |
| `DB_PASSWORD` | Clave de la base. Cambiarla | `cambiar-esta-clave` |
| `CATALOGO_PORT` | Puerto del servicio en la máquina | `8081` |
| `CATALOGO_DB_PORT` | Puerto de PostgreSQL en la máquina | `5433` |

El servicio recibe la conexión en `DB_URL`, `DB_USER` y `DB_PASSWORD`. Con Docker Compose, `DB_URL` se arma sola a partir de `DB_NAME`. No hay valores por defecto: si falta alguna, el servicio no arranca.

## Arranque

```
docker compose up --build
```

Construye la imagen del servicio y levanta dos contenedores: `catalogo-db` (PostgreSQL 18, con su propio volumen) y `catalogo-service`, que espera a que la base esté lista.

Para verificar que está en pie:

```
curl http://localhost:8081/actuator/health
```

Tiene que responder `{"status":"UP"}` (con el detalle de los grupos de salud).

Para detenerlo:

```
docker compose down
```

Agregando `-v` se borra también el volumen con los datos de la base.

### Sin Docker Compose

Para probar el servicio a mano sin configurar nada, con una base descartable:

```
./gradlew bootTestRun
```

Necesita Docker en ejecución; los datos se pierden al detenerlo.

## Pruebas

```
./gradlew test
```

Las pruebas usan PostgreSQL real con Testcontainers, así que **Docker tiene que estar corriendo**. No hay que levantar nada a mano ni definir variables. El detalle de qué prueba cada cosa está en [`src/test/README.md`](src/test/README.md).

Las mismas pruebas corren en GitHub Actions en cada pull request hacia `develop` o `main` (`.github/workflows/ci.yml`).

## Estructura

El código sigue la arquitectura hexagonal de la cátedra: un paquete por feature con las capas `domain`, `application` e `infrastructure`, y un paquete `shared` para lo transversal. Cada paquete tiene un `package-info.java` que explica qué va ahí. La regla de dependencias entre capas la verifica `ArchitectureTest`.

```
com.example.catalogo
├── category/      feature de categorías de profesionales
└── shared/        configuración, seguridad y manejo de errores comunes
```
