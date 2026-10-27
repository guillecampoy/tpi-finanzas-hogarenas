# Modelo de datos — Gestión de Finanzas Hogareñas

El sistema utiliza cuatro colecciones MongoDB. Las relaciones se implementan mediante referencias (`ObjectId`); la categoría de un movimiento incorpora además una copia histórica de su nombre (*snapshot*).

```mermaid
erDiagram
    %% Relaciones mediante Linking
    USUARIOS ||--o{ CUENTAS : "1:N Linking"
    USUARIOS ||--o{ MOVIMIENTOS : "1:N Linking"
    CUENTAS ||--o{ MOVIMIENTOS : "1:N Linking"

    %% Linking + Snapshot embebido
    CATEGORIAS ||--o{ MOVIMIENTOS : "1:N Linking + Snapshot"

    USUARIOS {
        ObjectId _id PK
        string nombre
        string email UK
        boolean activo
        date createdAt
        date updatedAt
        date deletedAt
    }

    CUENTAS {
        ObjectId _id PK
        ObjectId usuarioId FK
        string nombre
        string tipo
        string moneda
        boolean activo
        date createdAt
        date updatedAt
        date deletedAt
    }

    CATEGORIAS {
        ObjectId _id PK
        string nombre
        string tipo
        string descripcion
        boolean activo
        date createdAt
        date updatedAt
        date deletedAt
    }

    MOVIMIENTOS {
        ObjectId _id PK
        ObjectId usuarioId FK
        ObjectId cuentaId FK
        object categoria "Embedding: id + nombre"
        string descripcion
        string tipo
        Decimal128 monto
        date fecha
        boolean activo
        date createdAt
        date updatedAt
        date deletedAt
    }
```

## Decisiones de modelado

- **Linking:** `usuarioId` y `cuentaId` referencian documentos independientes. `categoria.id` referencia una categoría dentro del subdocumento embebido.
- **Embedding:** `movimientos.categoria` contiene `{ id, nombre }` para preservar el nombre histórico usado al registrar el movimiento.
- **Baja lógica:** todas las colecciones utilizan `activo` y `deletedAt`; no se eliminan documentos físicamente.
- **Importes:** `Decimal128` evita los problemas de precisión binaria de `double` para dinero.
- **Identificadores:** MongoDB genera automáticamente `_id` de tipo `ObjectId`.

> **Nota:** `PK`, `FK` y `UK` son anotaciones conceptuales del diagrama. MongoDB no impone claves foráneas; la aplicación valida las referencias. La unicidad de `email` requiere un índice único.
