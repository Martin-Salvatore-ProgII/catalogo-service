# Pruebas del servicio de catálogo

## Requisito: Docker en ejecución

Las pruebas usan PostgreSQL real, no una base en memoria (ADR-0045). Al correrlas, Testcontainers levanta un contenedor `postgres:18` descartable, le aplica las migraciones de Flyway y lo elimina al terminar.

Por eso **Docker tiene que estar corriendo** en la máquina. No hace falta levantar nada a mano ni definir variables de entorno: el contenedor y la conexión los maneja el propio test. Si Docker no está disponible, las pruebas que levantan el contexto de Spring fallan al iniciar.

La primera ejecución descarga la imagen de PostgreSQL y tarda más.

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
| `ArchitectureTest` | Verifica la regla de dependencias de la arquitectura hexagonal (ADR-0053) | No |
| `TestcontainersConfiguration` | Declara el contenedor de PostgreSQL que usan las pruebas. Se importa con `@Import` en cada prueba que necesite base | — |
| `TestJwtKeysConfiguration` | Hace el papel de turnos: genera en memoria un par de claves RSA, le da la pública al servicio y permite firmar tokens de prueba. No hay ninguna clave en el repositorio | — |
| `TestCatalogoApplication` | No es una prueba: levanta la aplicación con una base descartable para probarla a mano | Sí |

## Pruebas por tema

Las pruebas siguen la estructura de paquetes del código: cada clase se prueba en su capa.

| Qué se quiere comprobar | Dónde | Necesita Docker |
| --- | --- | --- |
| Seguridad del endpoint de categorías: sin token, token vencido, firma inválida, y el caso válido de punta a punta | `category/CategoryAccessTest` | Sí |
| Reglas de acceso: rutas públicas, sin token, con token, CORS | `shared/infrastructure/config/SecurityConfigTest` | Sí |
| Validación del JWT: firma, vencimiento, login y roles | `shared/infrastructure/config/JwtConfigTest` | No |
| Listado de categorías: regla de negocio | `category/application/usecases/*Test` | No |
| Listado de categorías: forma de la respuesta HTTP | `category/infrastructure/web/controller/CategoryControllerTest` | No |
| Lectura de categorías en PostgreSQL: solo habilitadas, por nombre | `category/infrastructure/persistence/adapter/JpaCategoryRepositoryAdapterTest` | Sí |
| Esquema y restricciones de la base | `DatabaseMigrationTest` | Sí |

## Levantar la aplicación con una base descartable

```
./gradlew bootTestRun
```

Arranca el servicio contra un PostgreSQL de Testcontainers, sin configurar `DB_URL`, `DB_USER` ni `DB_PASSWORD`. Los datos se pierden al detenerlo.
