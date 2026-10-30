# Finanzas Hogareñas

API REST para registrar y consultar movimientos financieros de un hogar. El proyecto implementa el alta, consulta, modificación parcial y baja lógica de movimientos en MongoDB.

[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.5.16](https://img.shields.io/badge/Spring%20Boot-3.5.16-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MongoDB](https://img.shields.io/badge/MongoDB-Database-47A248?logo=mongodb&logoColor=white)](https://www.mongodb.com/)
[![Maven](https://img.shields.io/badge/Maven-Build-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-Swagger%20UI-85EA2D?logo=swagger&logoColor=black)](https://springdoc.org/)

## Estado

La aplicación Spring Boot persiste movimientos en la colección `movimientos` y valida sus referencias contra las colecciones `usuarios`, `cuentas` y `categorias`. Incluye validación de datos, documentación OpenAPI/Swagger, un script idempotente para cargar entidades de demostración y un script de respaldo con `mongodump`.

La configuración admite MongoDB local por defecto o una instancia indicada mediante variables de entorno. La API no implementa autenticación ni operaciones CRUD para usuarios, cuentas o categorías; esas entidades se preparan con el script de demostración.

## Tecnologías

- Java 21 y Spring Boot 3.5.16.
- Spring Web, Spring Data MongoDB y Bean Validation.
- MongoDB y MongoDB Database Tools (`mongodump`).
- Maven.
- springdoc OpenAPI 3 y Swagger UI.

## Requisitos

- JDK 21.
- Maven 3.9 o posterior.
- MongoDB local o una URI de conexión a MongoDB. Para los scripts de datos y respaldo también se necesitan `mongosh` y/o MongoDB Database Tools, respectivamente.

## Ejecutar

Desde la raíz del repositorio, configura la conexión si no vas a usar el valor local predeterminado:

```bash
export MONGODB_URI='mongodb://localhost:27017/finanzas_hogar'
export MONGODB_DATABASE='finanzas_hogar'
mvn spring-boot:run
```

La aplicación escucha en el puerto `8080`. Si `MONGODB_URI` y `MONGODB_DATABASE` no están definidas, `application.yml` usa `mongodb://localhost:27017/finanzas_hogar` y la base `finanzas_hogar`.

### Entidades de demostración

La API requiere que las referencias indicadas al crear un movimiento existan y estén activas. Para cargar usuario, cuenta y categoría de prueba, configura `MONGODB_URI` y ejecuta desde la raíz:

```bash
mongosh "$MONGODB_URI" --file examples/seed.js
```

El script crea o actualiza los documentos de demostración con identificadores conocidos. Instrucciones y valores están en [examples/README.md](examples/README.md).

## API

| Método | Ruta | Comportamiento |
| --- | --- | --- |
| `GET` | `/api/health` | Comprueba que la aplicación responde. |
| `POST` | `/api/movimientos` | Crea un movimiento (`201`). |
| `GET` | `/api/movimientos` | Lista movimientos activos. |
| `GET` | `/api/movimientos/{id}` | Consulta un movimiento activo. |
| `PATCH` | `/api/movimientos/{id}` | Modifica parcialmente un movimiento activo. |
| `DELETE` | `/api/movimientos/{id}` | Da de baja lógicamente (`204`). |

Los identificadores inválidos producen `400`; los movimientos inexistentes o inactivos producen `404`. `POST` requiere `descripcion`, `monto`, `tipo`, `fecha`, `cuentaId`, `creadoPorId` y `categoriaId`. El monto se envía como decimal en texto, debe ser mayor que cero y se almacena como `Decimal128`. La fecha de entrada usa `yyyy-MM-dd`; los tipos admitidos son `INGRESO` y `EGRESO`.

Con la aplicación en ejecución:

- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- Especificación OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- Estado: [http://localhost:8080/api/health](http://localhost:8080/api/health)

## Modelo de datos

`movimientos` guarda `cuentaId` y `creadoPorId` como referencias `ObjectId`. La categoría se copia dentro del movimiento como `{ id, nombre }`, conservando el nombre utilizado al registrarlo. La baja lógica establece `activo=false` y `deletedAt`; las consultas de movimientos solo devuelven documentos activos.

El diagrama, los campos de las cuatro colecciones y las decisiones de embedding/linking están documentados en [docs/modelo/modelo-datos.md](docs/modelo/modelo-datos.md). La aplicación gestiona directamente los movimientos y consulta las colecciones de referencia para validar el alta y la modificación.

## Respaldo

Con `MONGODB_URI` configurada y MongoDB Database Tools instalado, ejecuta desde la raíz:

```bash
bash scripts/backup.sh
```

El script guarda la salida de `mongodump` en `resguardos_tpi/AAAA-MM-DD/`. Esa ruta está excluida de Git.

## Pruebas

El repositorio incluye pruebas unitarias de `MovimientoController`. Para ejecutarlas desde la raíz:

```bash
mvn test
```

## Documentación del repositorio

La documentación está organizada por tema y enlazada desde este README:

| Tema | Documento | Contenido |
| --- | --- | --- |
| Inicio y ejecución | [README.md](README.md) | Estado, requisitos, configuración, rutas y comandos del proyecto. |
| Modelo | [docs/modelo/modelo-datos.md](docs/modelo/modelo-datos.md) y [docs/modelo/README.md](docs/modelo/README.md) | Colecciones, relaciones, ejemplo y decisiones de modelado. |
| API | [docs/api/README.md](docs/api/README.md) | Acceso a Swagger/OpenAPI y escenarios de la API. |
| Datos de demostración | [examples/README.md](examples/README.md) | Preparación de entidades de referencia y datos de prueba. |
| Evidencias | [docs/evidencias/README.md](docs/evidencias/README.md) | Evidencias del modelo, Swagger, baja lógica y respaldo. |

## Integrantes

- Luis Mariano Rivera Ortiz
- Guillermo Campoy
