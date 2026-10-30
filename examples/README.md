# Datos de demostración para probar el CRUD

La API valida que las referencias de un movimiento apunten a documentos existentes y activos. En una base nueva, preparar primero las entidades base con el script idempotente `seed.js`; luego se pueden crear movimientos desde Swagger.

## Requisitos

- `mongosh` instalado.
- `MONGODB_URI` configurada para la base `finanzas_hogar` y con permisos de lectura/escritura.
- La IP del equipo autorizada si se usa Atlas.

Desde la raíz del repositorio, cargar el entorno (si todavía no está cargado) y ejecutar:

```bash
set -a
source .env
set +a
mongosh "$MONGODB_URI" --file examples/seed.js
```

El script crea o actualiza un usuario, una cuenta y una categoría ficticios, usando IDs fijos. Es seguro volverlo a ejecutar: actualiza esos tres documentos demo sin duplicarlos. Los IDs son:

| Entidad | ID de ejemplo |
| --- | --- |
| Cuenta | `507f1f77bcf86cd799439011` |
| Categoría | `507f1f77bcf86cd799439012` |
| Usuario (`creadoPorId`) | `507f191e810c19729de860ea` |

Estos mismos valores aparecen en los ejemplos de Swagger. Son datos de demostración, no deben usarse para información real. La precarga no crea movimientos: después de ejecutarla, `POST /api/movimientos` puede utilizar esas referencias.
