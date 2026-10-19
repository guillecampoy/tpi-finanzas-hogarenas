# TPI — Gestor de Finanzas Hogareñas

> Trabajo Práctico Integrador de Bases de Datos II — MongoDB / NoSQL
>
> **Estado:** prototipo inicial · **Integrantes:** LUIS MARIANO RIVERA ORTIZ, GUILLERMO CAMPOY.  
>  **Dominio:** finanzas domésticas

## 1. Objetivo

Diseñar e implementar una API pequeña que permita registrar y consultar **ingresos y egresos de un hogar**, organizados por cuentas y categorías. Los datos se almacenan en **MongoDB Atlas (M0)**. El trabajo debe demostrar modelado NoSQL, decisiones justificadas de *embedding* y *linking*, CRUD desde código, **baja lógica** y respaldo automatizado con `mongodump`.

**Nota importante:** el modelo de datos es el protagonista, no la arquitectura del backend. No se construirá un sistema complejo ni una interfaz gráfica.

## 2. Alcance

**Incluido, alcance evaluable como propuesta de la materia:**
- Cuatro colecciones relacionadas: `usuarios`, `cuentas`, `categorias`, `movimientos`.
- CRUD HTTP de `movimientos`, con baja lógica (`activo=false`, `deletedAt`).
- Listado de movimientos activos (excluyendo los eliminados lógicamente).
- Datos de ejemplo para mostrar las relaciones.
- Atlas M0, usuario de DB con permisos mínimos y acceso de red restringido.
- Swagger UI / OpenAPI para explorar y probar la API desde el navegador.
- Script de backup con `mongodump`, directorio relativo `resguardos_tpi/AAAA-MM-DD/`.
- Informe PDF con evidencias y coloquio: ambos integrantes dominan **todo** el proyecto.

**Opcional, IMPORTANTE, sólo si queda tiempo:** aggregation por categoría, índices fundamentados, validación JSON Schema en MongoDB, tests automáticos puntuales. No se implementarán inversiones, tarjetas, conciliación, presupuestos avanzados ni autenticación de usuarios.

## 3. Modelo propuesto (a validar @Luis)

| Colección | Responsabilidad | Vinculación |
| --- | --- | --- |
| `usuarios` | Quién registra un movimiento | Referencia desde `movimientos.creadoPorId` |
| `cuentas` | Origen/destino de fondos (efectivo, banco, billetera) | Referencia desde `movimientos.cuentaId` |
| `categorias` | Catálogo de clasificación de movimientos | Referencia lógica y snapshot embebida en `movimientos.categoria` |
| `movimientos` | Ingresos/egresos | Referencias y snapshot indicada |

### Ejemplo de movimiento

```json
{
  "descripcion": "Compra en supermercado",
  "monto": "48500.00",
  "tipo": "EGRESO",
  "fecha": "2026-10-18",
  "cuentaId": "507f1f77bcf86cd799439011",
  "creadoPorId": "507f191e810c19729de860ea",
  "categoria": {
    "id": "507f1f77bcf86cd799439012",
    "nombre": "Alimentación"
  },
  "activo": true
}
```

**Nota de IMPORTANTE sobre soporte para dinero:** en persistencia MongoDB se propone `Decimal128` para `monto` y Java `BigDecimal`; el ejemplo JSON representa el importe como *string* decimal para evitar problemas de precisión. El contrato definitivo debe quedar asentado en `docs/api/` y reflejar lo implementado.

### Decisión de diseño para la defensa

- **Linking:** `cuentaId` y `creadoPorId`, porque cuentas y usuarios tienen ciclo de vida propio y pueden relacionarse con muchos movimientos.
- **Embedding:** `categoria` contiene la ID y el nombre capturado al crear el movimiento. Simplifica la lectura y conserva la etiqueta histórica si el catálogo cambia. La colección `categorias` continúa siendo la fuente del catálogo.
- **Trade-off:** hay duplicación controlada y una decisión explícita sobre si las categorías renombradas se actualizan retroactivamente. **Propuesta:** no actualizar el nombre histórico.
- **Consistencia:** antes de crear o modificar, comprobar que las referencias sean válidas y activas. Para la entrega, no guardar saldo acumulado en `cuentas`: es un dato derivado que puede agregar complejidad de consistencia.
- **Baja lógica:** `activo` por defecto `true`, y `deletedAt` solo si se desactiva. Las consultas públicas excluyen inactivos. Un UPDATE sobre un registro inactivo debe rechazarse o responder no encontrado (decisión sugerida: 404).

Las relaciones son *lógicas*, no claves foráneas impuestas automáticamente por MongoDB. Para la defensa, documentar un caso de cada relación con documentos reales en `docs/modelo/`.

## 4. API HTTP propuesta

| Método | Ruta | Resultado |
| --- | --- | --- |
| `POST` | `/api/movimientos` | Crear movimiento (`201`) |
| `GET` | `/api/movimientos` | Listar **solo activos** (`200`) |
| `GET` | `/api/movimientos/{id}` | Consultar uno activo (`200` / `404`) |
| `PATCH` | `/api/movimientos/{id}` | Actualizar campos específicos (`200` / `404`) |
| `DELETE` | `/api/movimientos/{id}` | **Baja lógica** (`204` / `404`) |
| `GET` | `/api/health` | Confirmar que la API responde (`200`, no prueba Atlas) |

**Contrato mínimo:** validar descripción no vacía, monto mayor a cero, tipo `INGRESO`/`EGRESO`, fechas válidas, IDs válidas y categorías/cuentas existentes. Definir respuesta consistente ante errores (`400`, `404`, `500`). El prototipo contiene únicamente el endpoint de salud; el resto es trabajo pendiente a implementar.

## 5. Stack y herramientas

- **Java 21**, **Spring Boot 3.5.x**, Maven.
- **Spring Web**, **Spring Data MongoDB** (simple, sin capas innecesarias).
- **springdoc OpenAPI + Swagger UI**: documentación y prueba manual en navegador, sin Postman.
- **MongoDB Atlas M0** y MongoDB Database Tools (`mongodump`).
- `bash` para el respaldo (Linux/macOS).

Estructura intencionalmente corta: `api/`, `model/`, `repository/`. Una capa `service/` solo si hace falta para validaciones o reglas.

## 6. Estructura del repositorio

```text
.
├── README.md
├── pom.xml
├── .env.example
├── .gitignore
├── src/
│   ├── main/
│   │   ├── java/ar/edu/tpi/finanzas/
│   │   │   ├── FinanzasApplication.java
│   │   │   ├── api/HealthController.java
│   │   │   ├── model/                # TODO: documentos / DTOs
│   │   │   └── repository/           # TODO: interfaces de acceso
│   │   └── resources/application.yml
│   └── test/java/ar/edu/tpi/finanzas/
├── docs/
│   ├── modelo/README.md           # colecciones, ejemplos y decisiones
│   ├── api/README.md              # contrato y casos de prueba
│   └── evidencias/README.md       # capturas para el informe/coloquio
├── examples/README.md             # datos de prueba / comandos de carga
└── scripts/backup.sh
```

## 7. Guía paso a paso: levantar el proyecto desde cero

### Paso 0 — Preparar las herramientas (única vez)

Instalar:

1. **Git** para descargar el repositorio: [git-scm.com/downloads](https://git-scm.com/downloads).
2. **JDK 21** (no solo JRE): por ejemplo [Eclipse Temurin 21](https://adoptium.net/temurin/releases/?version=21).
3. **Apache Maven 3.9 o superior**: [maven.apache.org/install.html](https://maven.apache.org/install.html).
4. Editor de código; **VS Code** con *Extension Pack for Java* es suficiente. También sirve IntelliJ IDEA.
5. Para la etapa de backups (no es necesario para levantar la API): **MongoDB Database Tools**, que incluye `mongodump`: [documentación de instalación](https://www.mongodb.com/docs/database-tools/installation/installation/).

Abrir una terminal y ejecutar **uno por uno**:

```bash
git --version
java -version
mvn -version
```

**Resultado esperado:** se muestran las versiones sin errores; Java debe indicar **21** y Maven **3.9 o superior**. Si algún comando dice `command not found` / `no se reconoce`, falta instalarlo o agregarlo al `PATH`. En Windows, se puede usar PowerShell para estos tres comandos, pero los comandos `.sh` del proyecto requieren Bash (Git Bash o WSL). **Para simplificar, los pasos siguientes usan Linux/macOS o Git Bash.**

### Paso 1 — Obtener el código

```bash
git clone https://github.com/guillecampoy/tpi-finanzas-hogarenas.git
cd tpi-finanzas-hogarenas
```

### Paso 2 — Crear la base de datos en MongoDB Atlas (una sola vez por grupo)

1. Entrar a [MongoDB Atlas](https://cloud.mongodb.com/) y crear una cuenta (o iniciar sesión).
2. Crear un proyecto y un clúster con **opción gratuita M0**. Verificar que sea el plan gratuito antes de finalizar.
3. En **Database Access**, crear un **usuario de base de datos** con nombre y contraseña. No confundirlo con la cuenta que usás para entrar al sitio de Atlas. Para la práctica, asignar permisos de lectura/escritura sobre `finanzas_hogar` si esa opción está disponible.
4. En **Network Access**, agregar **tu IP actual** a la lista de IP permitidas. Cada integrante puede necesitar agregar su propia IP. Evitar `0.0.0.0/0` como configuración permanente.
5. Ir al clúster, seleccionar **Connect → Drivers** y copiar la cadena de conexión para Java. Se parece a:

```text
mongodb+srv://USUARIO:PASSWORD@CLUSTER.mongodb.net/?retryWrites=true&w=majority
```

Sustituir usuario, contraseña y host del clúster por los valores reales. Si la contraseña contiene caracteres especiales (por ejemplo `@`, `:`, `/`, `#`), deben codificarse para una URI o cambiarse por una contraseña compatible. Agregar `/finanzas_hogar` antes de `?` para indicar la base de datos:

```text
mongodb+srv://USUARIO:PASSWORD@CLUSTER.mongodb.net/finanzas_hogar?retryWrites=true&w=majority
```

**Importante:** Atlas puede no mostrar `finanzas_hogar` hasta que se inserte el primer documento. Eso es normal. Las credenciales se pasan por un archivo local; **nunca se publican en Git, capturas ni chats grupales**.

### Paso 3 — Configurar variables locales

Desde la raíz del repositorio:

```bash
cp .env.example .env
```

Abrir `.env` con el editor y reemplazar la URI de ejemplo por la propia de Atlas:

```dotenv
MONGODB_URI='mongodb+srv://USUARIO:CLAVE@CLUSTER.mongodb.net/finanzas_hogar?retryWrites=true&w=majority'
MONGODB_DATABASE=finanzas_hogar
```

El archivo `.env` está ignorado por Git. **No agregar espacios antes o después del `=`.** El archivo de ejemplo `.env.example` sí puede compartirse, porque no contiene datos reales.

**Cada integrante debe crear su propio `.env`**. Pueden usar el mismo clúster para el TPI, pero es preferible entregar credenciales a cada integrante de manera privada (idealmente con usuarios de base de datos distintos).

### Paso 4 — Ejecutar la aplicación

Ubicado donde está `pom.xml`, cargar las variables del archivo en la terminal:

```bash
set -a
source .env
set +a
```

Ahora ejecutar:

```bash
mvn spring-boot:run
```

En la **primera ejecución** Maven debe descargar las dependencias de Internet. Una vez iniciado, debería aparecer una línea semejante a `Started FinanzasApplication` y el servidor escuchará en el puerto `8080`.


> Si usás **PowerShell** en Windows (sin Git Bash), `source .env` no funciona. Podés configurar las variables solo para esa sesión con `$env:MONGODB_URI="..."` y `$env:MONGODB_DATABASE="finanzas_hogar"` (sin copiar los puntos suspensivos), y luego ejecutar `mvn spring-boot:run`. No guardes contraseñas en scripts versionados.

### Paso 5 — Comprobar que funciona

Abrir estas direcciones en el navegador, con la aplicación en ejecución:

| Dirección | Qué deberías ver |
| --- | --- |
| [http://localhost:8080/api/health](http://localhost:8080/api/health) | Respuesta del endpoint de salud. |
| [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | Interfaz Swagger UI con las rutas disponibles. |
| [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) | Definición OpenAPI en formato JSON. |

**Cómo probar Swagger:** abrir Swagger UI, desplegar una operación, pulsar **Try it out** y después **Execute**. El navegador mostrará el código HTTP y la respuesta.

**Ojo con esta distinción:** hoy el andamiaje solo implementa `/api/health`. Poder abrir Swagger o recibir `200` desde `/api/health` **no demuestra que se haya conectado a Atlas**. Cuando se implemente el repositorio y el CRUD de `movimientos`, deberán insertar y consultar un documento real para verificar la conexión. Hasta entonces, que no aparezcan las rutas de movimientos **no es un error**.

### Paso 6 — Hacer un primer cambio (ejercicio de práctica)

1. Abrir `src/main/java/ar/edu/tpi/finanzas/api/HealthController.java`.
2. Leer qué ruta expone y qué devuelve.
3. Modificar el mensaje de respuesta (sin cambiar la ruta).
4. Reiniciar la aplicación (`Ctrl+C`, luego `mvn spring-boot:run`).
5. Volver a abrir `/api/health` y comprobar el cambio.

La idea es familiarizarse con el ciclo **editar → ejecutar → verificar**, antes de implementar MongoDB.

### Paso 7 — Ejecutar el backup (cuando ya existan datos)

Instalar **MongoDB Database Tools** y verificar que `mongodump` esté disponible:

```bash
mongodump --version
```

Desde la raíz del repositorio, cargar el `.env` **si abriste una terminal nueva** y ejecutar:

```bash
set -a
source .env
set +a
bash scripts/backup.sh
```

El script crea `resguardos_tpi/AAAA-MM-DD/finanzas_hogar/` y guarda allí los archivos BSON/metadatos extraídos desde Atlas. Para verificar los archivos:

```bash
ls -la resguardos_tpi/
```

Un directorio vacío o sin colecciones no demuestra un backup completo: probarlo **después de insertar datos reales de ejemplo**. No subir `resguardos_tpi/` a Git. Más adelante, como práctica adicional, se puede restaurar con `mongorestore` sobre **otra base de pruebas**, nunca sobre la base compartida sin acordarlo con el equipo.

### Problemas frecuentes (y cómo diagnosticarlos)

| Problema | Qué revisar primero |
| --- | --- |
| `java: command not found` o versión incorrecta | JDK 21 instalado y `JAVA_HOME`/`PATH`. |
| `mvn: command not found` | Maven instalado y su carpeta `bin` agregada al `PATH`. |
| `mvn` no encuentra `pom.xml` | Abriste la terminal en otra carpeta; volvé a la raíz del proyecto. |
| `Address already in use: 8080` | Otro proceso usa el puerto 8080. Cerralo o cambiá `server.port` localmente. |
| Swagger no abre | Revisá que la aplicación siga ejecutándose y que estés usando `http://localhost:8080/swagger-ui.html`. |
| Error `Authentication failed` en MongoDB | Usuario/contraseña de **Database Access**, URI y codificación de caracteres especiales. |
| Error de conexión/timeout con Atlas | La IP está autorizada en **Network Access**, Internet funciona y el host de la URI es correcto. |
| `mongodump: command not found` | Falta instalar **MongoDB Database Tools** o configurarlo en el `PATH`. |
| El backup no contiene documentos | Todavía no se insertaron datos, se apuntó a otra base o faltan permisos. |

**Regla simple para pedir ayuda:** compartir el comando usado, el texto del error y qué paso de esta guía falló. **Antes de compartir logs, ocultar la URI, usuarios, contraseñas y cualquier dato sensible.**

### Checklist personal de arranque

- [ ] Instalé Git, JDK 21 y Maven, y sus comandos muestran versiones.
- [ ] Tengo el repositorio en mi computadora y encuentro `pom.xml`.
- [ ] Tengo acceso al proyecto/clúster Atlas del grupo.
- [ ] Mi IP está habilitada y tengo credenciales válidas de base de datos.
- [ ] Creé mi `.env` local sin subirlo a Git.
- [ ] Arranqué Spring Boot sin errores.
- [ ] Abrí `/api/health` y Swagger UI en el navegador.
- [ ] Entiendo que la prueba de conexión con Atlas queda pendiente hasta implementar el CRUD.
- [ ] Cuando haya datos: ejecuté `mongodump` y confirmé los archivos de respaldo.

## 8. Checklist de la entrega

### Funcionalidad y modelado (obligatorio)
- [ ] Acordar dominio, reglas de negocio y alcance con el grupo.
- [ ] Definir **al menos 3 colecciones relacionadas** (propuesta: 4).
- [ ] Documentar ejemplos reales de documentos y cardinalidades.
- [ ] Explicar con ejemplos **embedding** y **linking**, incluyendo los trade-offs.
- [ ] Crear Atlas M0; configurar Database Access y Network Access.
- [ ] Probar conexión real desde el backend a Atlas.
- [ ] Implementar CREATE, READ, UPDATE y DELETE **lógico** desde código.
- [ ] Verificar que las lecturas no muestran documentos inactivos.
- [ ] Ejecutar el script con `mongodump` y conservar evidencia del backup.

### Calidad mínima (recomendada)
- [ ] README reproducible desde clon limpio.
- [ ] `.env.example` sin credenciales; `.gitignore` efectivo.
- [ ] Swagger UI con ejemplos claros de request/response.
- [ ] Casos felices y errores básicos (IDs inválidas, no existe, monto inválido).
- [ ] Dataset chico pero representativo para la demostración.
- [ ] Revisar consistencia de capturas, nombres de colecciones y documentación.

### Presentación y coloquio (obligatorio)
- [ ] Entregar **informe PDF** con dominio, modelo, justificación, Atlas, CRUD y respaldo.
- [ ] Entregar **código fuente** y pasos de ejecución.
- [ ] Incluir evidencias: Atlas, Swagger CRUD, baja lógica y backup realizado.
- [ ] Ambos integrantes pueden explicar el modelo entero y ejecutar CRUD/backup.
- [ ] Ensayar una defensa corta con preguntas cruzadas.

## 9. Backlog y reparto del trabajo

No asignar trabajo solo por especialidad: **cada tarea debe ser revisada y explicada por el otro integrante**. Añadir responsable/estado una vez conversado con el grupo.

| ID | Tarea | Prioridad | Responsable | Estado |
| --- | --- | --- | --- | --- |
| T01 | Cerrar alcance, reglas y diagrama de relaciones | Alta | Sin asignar | Pendiente |
| T02 | Definir documentos, embedding/linking y baja lógica | Alta | Sin asignar | Pendiente |
| T03 | Crear Atlas M0 y acceso restringido | Alta | Sin asignar | Pendiente |
| T04 | Conectar Java/Spring con Atlas | Alta | Sin asignar | Pendiente |
| T05 | Implementar POST + validaciones | Alta | Sin asignar | Pendiente |
| T06 | Implementar GET (filtrado por activo) | Alta | Sin asignar | Pendiente |
| T07 | Implementar PATCH por campos y DELETE lógico | Alta | Sin asignar | Pendiente |
| T08 | Completar Swagger/OpenAPI y ejemplos | Alta | Sin asignar | Pendiente |
| T09 | Completar y ejecutar `mongodump` | Alta | Sin asignar | Pendiente |
| T10 | Datos demo y prueba extremo a extremo | Alta | Sin asignar | Pendiente |
| T11 | Evidencias + informe PDF | Alta | Sin asignar | Pendiente |
| T12 | Ensayar defensa, intercambiando quién ejecuta y quién explica | Alta | Ambos | Pendiente |
| T13 | Aggregation / validación JSON Schema / índices | Opcional | Sin asignar | Pendiente |

**Organización sugerida:** primero acordar juntos el modelo; después dividir T03–T09 según disponibilidad; cerrar juntos T10–T12. Usar PRs o revisión de pares sencilla (sin un proceso de Git complejo).

## 10. Plan de prototipo por hitos

1. **Hito A — Conexión:** levantar Spring, abrir Swagger, acceder a Atlas.
2. **Hito B — Modelo:** insertar datos demo, comprobar referencias y snapshot embebida.
3. **Hito C — CRUD:** ejecutar operaciones completas y comprobar baja lógica.
4. **Hito D — Resguardo:** ejecutar `mongodump`, inspeccionar el resultado.
5. **Hito E — Defensa:** generar PDF, capturas y ensayo cruzado.

## 11. Decisiones pendientes / riesgos

- **R1 — Semántica del dinero:** acordar formato del monto en HTTP y persistencia `Decimal128`; evitar `double`.
- **R2 — Baja lógica de catálogos:** no eliminar físicamente usuarios/cuentas/categorías; definir cómo impedir referenciar elementos inactivos y qué mostrar en históricos.
- **R3 — Categorías históricas:** snapshot preserva nombre original; documentar expresamente.
- **R4 — API sin autenticación:** usar **datos ficticios**, API solo local para la demo y credenciales fuera de Git.
- **R5 — Backup:** `mongodump` es un backup lógico (BSON), aunque la consigna lo llame «respaldo físico»; documentar la herramienta y qué produce sin afirmar que es snapshot físico de almacenamiento.
- **R6 — Reproducibilidad:** el backup depende de la IP habilitada, permisos, URI correcta y herramientas instaladas.
- **R7 — Demostración:** disponer de un dataset pequeño, restaurable y con movimientos activos e inactivos.

## 12. Documentación de referencia

- [MongoDB Atlas](https://www.mongodb.com/docs/atlas/)
- [mongodump](https://www.mongodb.com/docs/database-tools/mongodump/)
- [Spring Data MongoDB](https://docs.spring.io/spring-data/mongodb/reference/)
- [springdoc OpenAPI](https://springdoc.org/)

---

**Definición de terminado:** con un clon limpio, configuración propia de Atlas y los prerrequisitos instalados, cualquiera de los dos integrantes puede levantar la API, demostrar CRUD con baja lógica en Swagger, ejecutar el backup y explicar decisiones de modelado.
