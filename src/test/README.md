# Pruebas del servicio de catálogo

## Requisito: Docker en ejecución

Las pruebas usan PostgreSQL real, no una base en memoria (ADR-0045). Al correrlas, Testcontainers levanta un contenedor `postgres:18` descartable, le aplica las migraciones de Flyway y lo elimina al terminar. La cátedra también se simula en un contenedor, con WireMock (`wiremock/wiremock`): ninguna prueba habla con la cátedra real (ADR-0044).

Por eso **Docker tiene que estar corriendo** en la máquina. No hace falta levantar nada a mano ni definir variables de entorno: el contenedor y la conexión los maneja el propio test. Si Docker no está disponible, las pruebas que levantan el contexto de Spring fallan al iniciar.

La primera ejecución descarga las imágenes de PostgreSQL y WireMock, y tarda más.

## Cómo ejecutarlas

```
./gradlew test
```

El reporte queda en `build/reports/tests/test/index.html`.

Para correr una sola clase:

```
./gradlew test --tests '*ArchitectureTest'
```

## Qué hay

| Archivo | Para qué | Necesita Docker |
| --- | --- | --- |
| `CatalogoApplicationTests` | Verifica que la aplicación arranca completa contra PostgreSQL, con Flyway | Sí |
| `ArchitectureTest` | Verifica la regla de dependencias de la arquitectura hexagonal (ADR-0053) y que una feature solo use el `domain` de otra (ADR-0062) | No |
| `TestcontainersConfiguration` | Declara el contenedor de PostgreSQL que usan las pruebas. Se importa con `@Import` en cada prueba que necesite base | — |
| `TestJwtKeysConfiguration` | Hace el papel de turnos: genera en memoria un par de claves RSA, le da la pública al servicio y permite firmar tokens de prueba. No hay ninguna clave en el repositorio | — |
| `TestCatedraConfiguration` | Le da a la aplicación una cátedra falsa que responde como la real: la integración en estado `PROVISIONED` y el snapshot real. Toda prueba con `@SpringBootTest` la importa, y su contexto arranca con el catálogo real cargado | — |
| `TestClockConfiguration` | Fija el reloj de la aplicación completa en el martes 2026-10-20 a las 12 de Argentina, para las pruebas que dependen de "hoy" | — |
| `CatalogTables` | Vacía las tablas del catálogo (y vuelve el estado de sincronización al inicial) para las pruebas con la aplicación completa que necesitan sus propios datos | — |
| `FakeCatedra` | Arma el contenedor de WireMock y le programa respuestas por su API de administración (incluidas demoras, cortes de conexión y escenarios). La respuesta de la integración está en `resources/catedra/integration.json`; la del snapshot, en `resources/catedra/snapshot.json`, una copia de un snapshot real de la cátedra | — |
| `TestCatalogoApplication` | No es una prueba: levanta la aplicación con una base descartable para probarla a mano | Sí |

## Pruebas por tema

Las pruebas siguen la estructura de paquetes del código: cada clase se prueba en su capa.

| Qué se quiere comprobar | Dónde | Necesita Docker |
| --- | --- | --- |
| Seguridad del endpoint de categorías: sin token, token vencido, firma inválida, y el caso válido de punta a punta | `category/CategoryAccessTest` | Sí |
| Reglas de acceso: rutas públicas, sin token, con token, CORS | `shared/infrastructure/config/SecurityConfigTest` | Sí |
| Validación del JWT: firma, vencimiento, login y roles | `shared/infrastructure/config/JwtConfigTest` | No |
| Toda lectura a la cátedra: JWT rechazado, respuesta inesperada o ilegible, cátedra lenta, caída o con error, exactamente un reintento, sin la dirección en los mensajes | `shared/infrastructure/rest/CatedraReadExecutorTest` | Sí |
| Integración con la cátedra al arrancar: campos, JWT técnico, `PROVISIONED` u otro estado, sin secretos al imprimirla | `shared/infrastructure/config/CatedraIntegrationConfigTest` | Sí |
| Cliente del snapshot: el snapshot real de la cátedra, todos los campos, horas `HH:mm`, campos desconocidos, día inválido, snapshot incompleto o con colecciones vacías | `sync/infrastructure/rest/adapter/CatedraSnapshotAdapterTest` | Sí |
| Listado de categorías: regla de negocio | `category/application/usecases/*Test` | No |
| Listado de categorías: forma de la respuesta HTTP | `category/infrastructure/web/controller/CategoryControllerTest` | No |
| Lectura y escritura de categorías en PostgreSQL: por id, todas, o solo habilitadas por nombre; guardar por id, reemplazar, borrar todo y volver a guardar | `category/infrastructure/persistence/adapter/JpaCategoryRepositoryAdapterTest` | Sí |
| Profesionales y horarios en PostgreSQL: todos los campos, horas y días, guardar por id, reemplazar, borrar todo y volver a guardar; buscar por id; horarios habilitados de un profesional ordenados por día y hora | `professional/infrastructure/persistence/adapter/*Test` | Sí |
| Búsqueda de profesionales en PostgreSQL: por categoría, nombre parcial sin mayúsculas ni tildes (en los dos sentidos), `%` y `_` literales, habilitado efectivo y deshabilitados, disponibilidad por día y franja (bordes que solo se tocan, horario deshabilitado, sin duplicados), filtros combinados, página y total, dirección del orden y desempate por id | `professional/infrastructure/persistence/adapter/JpaProfessionalRepositoryAdapterTest` | Sí |
| Habilitado efectivo (ADR-0014): las combinaciones de profesional y categoría, y sin categoría | `professional/domain/model/ProfessionalTest` | No |
| Búsqueda de profesionales: regla de negocio (orden por defecto, espacios del nombre, solo habilitados por defecto, categorías habilitadas, estado de cada resultado, fecha entre hoy y el horizonte en hora de Argentina) | `professional/application/usecases/*Test` | No |
| Búsqueda de profesionales: parámetros (fechas y horas `HH:mm` y `HH:mm:ss`), orden, respuesta, cabeceras de paginación, cada filtro inválido, las reglas de la franja y `DATE_OUT_OF_RANGE`; agenda con todos los campos, horas `HH:mm:ss`, 404 y 503 | `professional/infrastructure/web/controller/ProfessionalControllerTest` | No |
| Agenda vigente: regla de negocio (no existe, datos, versión, habilitado efectivo) y fachada (404) | `professional/application/usecases/GetProfessionalAgendaUseCaseImplTest`, `professional/application/service/ProfessionalServiceTest` | No |
| Los cuatro filtros de la búsqueda, cada uno solo, de a dos y los cuatro juntos, sobre el snapshot real recargado con casos borde (categoría deshabilitada, tilde, dos horarios el mismo día, sábado, horario apagado). Cada fila de la tabla es un caso con sus ids esperados | `professional/ProfessionalSearchFiltersTest` | Sí |
| Errores `problem+json`: validación con `fieldErrors`, todo 400 de Spring como `VALIDATION_ERROR` (JSON mal formado, tipo inválido, parámetro que falta), 404, 405, 500 genérico, `CATALOG_NOT_READY` | `shared/infrastructure/web/advice/GlobalExceptionHandlerTest` | No |
| Búsqueda de profesionales con la aplicación completa: 401 sin token, nombre sin tildes, filtros combinados, por defecto solo habilitados, deshabilitados marcados, disponibilidad con franja, fecha pasada, página y total, filtro inválido; agenda con 401, horarios habilitados en orden, versión local, categoría deshabilitada y 404 | `professional/ProfessionalSearchAccessTest` | Sí |
| Convención de paginación: enlaces `Link` en cada posición de página, total, máximo configurable | `shared/infrastructure/web/pagination/*Test` | No |
| Sincronización desde el snapshot de punta a punta: carga al arrancar con la base vacía, no repite con versión local, y una referencia inconsistente deja la copia anterior intacta y registra el error | `sync/CatalogSnapshotSyncTest` | Sí |
| Reglas del ciclo de sincronización: cuándo se pide el snapshot, el orden de borrado y guardado, el pedido fuera de la transacción, otro ciclo que ganó, errores de la cátedra y propios | `sync/application/usecases/*Test` | No |
| Estado de sincronización y catálogo no listo con la aplicación completa: el snapshot del arranque, 401 sin token, 503 `CATALOG_NOT_READY` en categorías sin versión local y `sync-status` respondiendo igual | `sync/CatalogReadinessAccessTest` | Sí |
| Respuesta HTTP del estado de sincronización: todos los campos, los vacíos en `null`, responde aunque el catálogo no esté listo | `sync/infrastructure/web/controller/SyncControllerTest` | No |
| Disparador del arranque: borra la marca de una caída antes de sincronizar | `sync/infrastructure/startup/CatalogSyncStartupListenerTest` | No |
| Estado de sincronización en PostgreSQL: fila inicial, avance solo desde la versión esperada (ADR-0007), fallas que no tocan la versión, marca de sincronización en curso | `sync/infrastructure/persistence/adapter/JpaSyncStateRepositoryAdapterTest` | Sí |
| Esquema y restricciones de la base, incluida la fila única de `sync_state` | `DatabaseMigrationTest` | Sí |

## Levantar la aplicación con una base descartable

```
./gradlew bootTestRun
```

Arranca el servicio contra un PostgreSQL de Testcontainers y una cátedra simulada, sin configurar `DB_URL`, `DB_USER`, `DB_PASSWORD` ni las variables de la cátedra. Los datos se pierden al detenerlo.
