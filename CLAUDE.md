# CLAUDE.md

Guía para agentes de IA que trabajan en este repositorio. Es corta a propósito: lo que vale para todo el sistema está en el `CLAUDE.md` de la carpeta que contiene a los repos y en el repositorio `docs` (constitución, ADR, contratos, `guia-git.md`). Acá va solo lo propio de este repo y lo que no se deduce leyendo el código.

## Antes de escribir código

1. Leer `docs/constitucion.md` y los ADR que toque la issue.
2. Cargar la skill `/hexagonal` y seguirla. No reconstruir el patrón de memoria.
3. Plantearle al alumno las decisiones abiertas, con una recomendación y su porqué, y esperar su respuesta. El alumno defiende cada decisión en una evaluación oral.

## Comandos

| Para | Comando |
| --- | --- |
| Compilar y correr todas las pruebas | `./gradlew build` |
| Correr una clase de prueba | `./gradlew test --tests '*NombreDeLaClase'` |
| Levantar el servicio con una base descartable | `./gradlew bootTestRun` |
| Levantar el servicio con su base en Docker | `docker compose up --build` |
| Levantar el sistema completo | Desde `../turnos-service`: `docker compose up --build` |

Las pruebas necesitan Docker en ejecución (PostgreSQL real con Testcontainers). El README explica la configuración (`.env`, clave pública del JWT).

## Qué hay construido

- **Feature `category`:** `GET /api/professional-categories`, solo lectura. Es la feature de referencia para las siguientes.
- **`shared/infrastructure/config`:** `JwtConfig` (valida el JWT con la clave pública), `SecurityConfig` (reglas de acceso y CORS).
- **`shared/infrastructure/web/advice`:** `GlobalExceptionHandler`.
- **Migración `V1`:** tablas `professional_category`, `professional` y `weekly_schedule`.

Todavía no hay: sincronización con la cátedra (snapshot, Redis, Kafka), las tablas `SYNC_STATE` y `PROCESSED_EVENT`, la respuesta `503 CATALOG_NOT_READY`, la búsqueda de profesionales ni la agenda. Tampoco HTTPS (ADR-0035).

## Lo propio de este servicio

- **Los datos no son propios.** Las tablas copian lo que publica la cátedra. El `id` de cada fila es el de la cátedra: las entities no llevan `@GeneratedValue` ni auditoría automática, y las fechas son las de la cátedra.
- **La base no valida el contenido de esos datos** (RNF-09): textos sin largo máximo, sin reglas sobre horas ni duraciones. Un valor inesperado se guarda y no frena la sincronización.
- **Las horas de atención se guardan sin zona,** tal como vienen, y se tratan como hora local (REF §4). No convertirlas.
- **Las reglas de lectura se declaran en el puerto** (`findAllEnabledOrderedByName`) y las ejecuta la base. No esconder un filtro dentro de la consulta de un `findAll`.
- **Este servicio no emite tokens.** Solo tiene la clave pública. En las pruebas, `TestJwtKeysConfiguration` hace el papel de turnos: genera el par, le pasa la pública al servicio y firma tokens con `signToken(...)`.
- **Las búsquedas se responden con la copia local,** nunca consultando a la cátedra (Constitución P-06).
- **El `.env` de este repo no usa prefijo** (`DB_NAME`, `DB_USER`); el de turnos sí. No se pueden repetir nombres entre los dos (ver `docs/arq/despliegue.md`).

## Probar con el sistema completo

Los tokens los emite turnos, así que para llamar a este servicio a mano hay que levantar los dos desde `../turnos-service`, registrarse e iniciar sesión ahí (`POST :8080/api/register`, `POST :8080/api/authenticate`) y usar el `id_token` como `Authorization: Bearer` contra el puerto 8081.

## Flujo de trabajo con Git

El detalle está en `docs/guia-git.md`. En resumen:

- **El agente** crea la rama de la issue, vinculada a ella: `gh issue develop <N> --base develop --name <tipo>/<N>-<descripcion> --checkout`. Escribe los archivos y corre `./gradlew build`.
- **El alumno** revisa, commitea, pushea, abre el pull request a `develop` y mergea. El agente no commitea, no pushea commits ni mergea, y no crea ni cierra issues.
- Al terminar, el agente entrega un bloque de comandos listo para pegar (`git add` por commit, `git commit -m`, `git push`, `gh pr create --base develop`, `gh pr merge --merge --delete-branch`, `gh issue close`). Mensajes en español, con Conventional Commits, sin atribuciones a herramientas.
- Antes de crear la rama de la issue siguiente, verificar con `git fetch` que el pull request anterior ya está en `origin/develop`.
- Al cerrar la última issue de una milestone se crea la rama `milestone/<n>-<nombre>` y el pull request a `main` sale de ahí, con revisión del profesor. Nunca de `develop`.
- Una decisión que afecta al otro servicio se propone para `docs` (ADR), con permiso del alumno antes de escribirla.

## Convenciones del código

- Indentación con tabs.
- Identificadores y mensajes de error en inglés; comentarios y documentación en español.
- **Comentarios de decisión:** donde se toma una decisión que alguien podría cuestionar, dos o tres líneas con el porqué, en ese lugar. No se comenta lo obvio.
- Cada paquete nuevo lleva su `package-info.java` (ADR-0055).
- Cada dependencia entra al build en la issue que la usa por primera vez.
- Las pruebas de cada pieza entran en la misma issue que la pieza.

## Particularidades técnicas

Spring Boot 4, Spring Security 7 y Jackson 3 cambiaron cosas respecto de versiones anteriores. Lo que ya costó descubrir:

**Dependencias.** Los starters tienen nombres nuevos (`spring-boot-starter-webmvc`, `spring-boot-starter-security-oauth2-resource-server`) y cada uno tiene su starter de test (`...-webmvc-test`, `...-data-jpa-test`). Ante la duda, pedirle la lista a Spring Initializr en lugar de adivinar.

**Paquetes de las anotaciones de prueba.**

| Anotación | Paquete |
| --- | --- |
| `@WebMvcTest`, `@AutoConfigureMockMvc` | `org.springframework.boot.webmvc.test.autoconfigure` |
| `@DataJpaTest` | `org.springframework.boot.data.jpa.test.autoconfigure` |
| `TestEntityManager` | `org.springframework.boot.jpa.test.autoconfigure` |
| `@AutoConfigureTestDatabase` | `org.springframework.boot.jdbc.test.autoconfigure` |
| `@MockitoBean` | `org.springframework.test.context.bean.override.mockito` |

**Cómo se arma cada tipo de prueba.**

- **Controller:** `@WebMvcTest` con `@AutoConfigureMockMvc(addFilters = false)`. Sin eso, la seguridad responde 401 antes de llegar al controller. Las reglas de acceso se prueban aparte.
- **Seguridad y recorridos completos:** `@SpringBootTest` + `@AutoConfigureMockMvc`, importando `TestcontainersConfiguration` y `TestJwtKeysConfiguration`. Sin la segunda, el contexto no arranca porque falta la clave del JWT.
- **Adaptador de persistencia:** `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + `@Import` de `TestcontainersConfiguration`, el adaptador y su mapper. Para leer de verdad de PostgreSQL hay que hacer `flush()` y `clear()` antes: sin eso JPA responde desde memoria, y los errores de restricciones de una colección no aparecen.
- **Casos de uso y fachada:** JUnit y Mockito, sin Spring.

**Jackson 3.** Un campo primitivo (`boolean`, `int`) que no viene en el JSON hace fallar el pedido con 400. Un campo opcional de un DTO de entrada va con su tipo envoltorio (`Boolean`, `Integer`).

**Spring Security 7.**

- Agrega por su cuenta la autoridad `FACTOR_BEARER` a todo pedido autenticado con token. No viene del JWT; al verificar roles en una prueba, usar `contains` y no una comparación exacta.
- Los 401 y 403 que genera la seguridad no tienen cuerpo: traen el encabezado `WWW-Authenticate`. Es lo acordado en el contrato.
- Una ruta inexistente sin token da 401, no 404, porque todo está protegido por defecto.

**Errores.** `GlobalExceptionHandler` tiene un manejador genérico de `Exception` que responde 500. La excepción de cada feature necesita su propio manejador ahí, o termina como 500. `ProblemDetail` agrega solo el campo `instance`; el `code`, el `path` y los `fieldErrors` del contrato se agregan con `setProperty`.

**Base de datos.** El esquema lo define Flyway; Hibernate solo valida (`ddl-auto: validate`). Una migración ya mergeada no se edita: el cambio va en la siguiente (`V2__...`). Los nombres de columnas se derivan solos de los campos de la entity (`createdAt` es `created_at`).

**Docker.** Si se cambia código y se prueba con Compose, hay que pasar `--build`, o corre la imagen vieja.

**Terminal.** La shell es zsh: una variable con espacios no se separa en palabras como en bash. En un script que encadena pasos, cortar ante el primer error (`set -e` o `&&`): un paso que falla no tiene que dejar seguir a uno que cierra o borra algo.

## Secretos

- `.env` y `secrets/` están fuera de Git. No se commitean ni se muestran.
- En la carpeta que contiene a los repos hay dos archivos de la cuenta técnica de la cátedra, `cuenta-tecnica.json` y `cuenta-tecnica-respuesta.json`. **No abrirlos, no mencionarlos con `@` y no copiar su contenido:** tienen la contraseña, el JWT técnico y la contraseña de Redis. Si hace falta pasar un valor a un `.env`, se hace con un script que no lo imprima, avisándole antes al alumno.
- `snapshot.json`, en esa misma carpeta, sí se puede leer: es una copia del catálogo de la cátedra, con datos ficticios.
- La dirección y los puertos del servidor de la cátedra no se escriben en el código, en la documentación ni en los mensajes de commit.
