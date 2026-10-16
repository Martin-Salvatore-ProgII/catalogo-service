# catalogo-service

Servicio de catálogo y sincronización del proyecto integrador 2026. Guarda la copia local del catálogo de profesionales que publica la cátedra y responde las búsquedas con esos datos.

Es uno de los tres repositorios de la entrega, junto con `turnos-service` y `app-kmp`. Los requisitos, la arquitectura, los contratos y las decisiones (ADR) están en el repositorio [`docs`](https://github.com/Martin-Salvatore-ProgII/docs).

## Estado

Listado de categorías de profesionales (`GET /api/professional-categories`), con la seguridad activa: todo pedido exige un JWT de usuario válido emitido por `turnos-service`, salvo `/actuator/health`. Al arrancar obtiene de la cátedra la configuración de la integración. Todavía no sincroniza el catálogo, así que las tablas están vacías; faltan también la búsqueda de profesionales y la agenda.

## Requisitos

- **Docker** con Docker Compose. Alcanza para levantar el servicio y para correr las pruebas.
- **Acceso a la red de la cátedra**, para levantar el servicio: al arrancar le pide la configuración de la integración, y sin respuesta no arranca. Las pruebas no lo necesitan.
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
| `CATEDRA_API_URL` | URL base de la API de la cátedra | La que indica la cátedra |
| `CATEDRA_TECHNICAL_TOKEN` | JWT técnico de la cuenta del proyecto ante la cátedra. Es un secreto | El `id_token` del registro de la cuenta técnica |

El servicio recibe la conexión en `DB_URL`, `DB_USER` y `DB_PASSWORD`. Con Docker Compose, `DB_URL` se arma sola a partir de `DB_NAME`. No hay valores por defecto: si falta alguna, el servicio no arranca.

### Integración con la cátedra

En el `.env` van solo la URL base y el JWT técnico. Redis, Kafka y sus topics no se configuran: al arrancar, el servicio los pide con `GET /api/student/integration` y los guarda solo en memoria (ADR-0005). Si la cátedra no responde (con un reintento), rechaza el JWT o la integración no está `PROVISIONED`, el servicio no arranca y lo informa en el log, sin mostrar valores.

Los timeouts hacia la cátedra tienen un valor inicial de 2 segundos para conectar y 5 para la respuesta (ADR-0029). Se pueden cambiar con `CATALOGO_CATEDRA_CONNECT_TIMEOUT` y `CATALOGO_CATEDRA_READ_TIMEOUT` (por ejemplo, `3s`).

### Clave pública del JWT de usuario

Este servicio no emite tokens: valida los que emite `turnos-service`, con la clave pública de su par RSA. La lee de `secrets/jwt-public.pem`. La carpeta `secrets/` no se commitea.

La clave pública se exporta de la clave privada de turnos, que tiene que existir antes (su README explica cómo generarla):

```
mkdir -p secrets
openssl pkey -in ../turnos-service/secrets/jwt-private.pem -pubout -out secrets/jwt-public.pem
```

Docker Compose monta esa carpeta en el contenedor en solo lectura. Sin ese archivo, el servicio no arranca. Si se cambia la clave de turnos, hay que volver a exportar la pública.

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

Para probar el servicio a mano sin configurar nada, con una base descartable y una cátedra simulada (no hace falta la red de la cátedra):

```
./gradlew bootTestRun
```

Necesita Docker en ejecución; los datos se pierden al detenerlo.

## Pruebas

```
./gradlew test
```

Las pruebas usan PostgreSQL real y una cátedra simulada con WireMock, las dos en contenedores de Testcontainers, así que **Docker tiene que estar corriendo**. Ninguna prueba habla con la cátedra real. No hay que levantar nada a mano ni definir variables. El detalle de qué prueba cada cosa está en [`src/test/README.md`](src/test/README.md).

Las mismas pruebas corren en GitHub Actions en cada pull request hacia `develop` o `main` (`.github/workflows/ci.yml`).

## Estructura

El código sigue la arquitectura hexagonal de la cátedra: un paquete por feature con las capas `domain`, `application` e `infrastructure`, y un paquete `shared` para lo transversal. Cada paquete tiene un `package-info.java` que explica qué va ahí. La regla de dependencias entre capas la verifica `ArchitectureTest`.

```
com.example.catalogo
├── category/      feature de categorías de profesionales
└── shared/        configuración (cliente e integración de la cátedra, seguridad) y manejo de errores comunes
```
