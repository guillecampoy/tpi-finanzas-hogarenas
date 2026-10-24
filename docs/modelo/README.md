# Modelo de datos — Gestor de Finanzas Hogareñas

Base de datos: `finanzas_hogar` (MongoDB Atlas, cluster M0).
Estado: borrador a validar por el grupo.

## 1. Visión general

Cuatro colecciones relacionadas:

| Colección | Responsabilidad | Relación con `movimientos` |
|---|---|---|
| `usuarios` | Quién registra un movimiento | Link: `movimientos.creadoPorId` |
| `cuentas` | Origen o destino de los fondos (efectivo, banco, billetera) | Link: `movimientos.cuentaId` |
| `categorias` | Catálogo de clasificación | Link lógico + copia embebida: `movimientos.categoria` |
| `movimientos` | Ingresos y egresos | Colección central |

Las relaciones son lógicas: MongoDB no impone claves foráneas, así que la aplicación valida que las referencias existan y estén activas antes de crear o modificar un movimiento.

```
usuarios (1) ──────< movimientos >────── (1) cuentas
                         │
                         └── categoria { id, nombre }  ← copia de categorias
```

**Cardinalidades:**

- Un usuario registra muchos movimientos (1:N).
- Una cuenta tiene muchos movimientos (1:N).
- Una categoría clasifica muchos movimientos (1:N).
- Cada movimiento pertenece a exactamente un usuario, una cuenta y una categoría (N:1).

## 2. Decisiones de diseño: embedding vs linking

**Linking (referencia por `_id`)** en `cuentaId` y `creadoPorId`:
usuarios y cuentas tienen ciclo de vida propio y se reutilizan en muchos movimientos. Si cambia el nombre de una cuenta, se modifica en un solo documento y todos los movimientos quedan coherentes.

**Embedding (copia dentro del documento)** en `categoria`:
el movimiento guarda el `id` de la categoría (link) y además una copia de su `nombre` tomada al momento de crearlo. Eso conserva la etiqueta histórica aunque el catálogo cambie después, y simplifica la lectura porque no hace falta buscar la categoría aparte. La colección `categorias` sigue siendo la fuente del catálogo.

**Trade-off:** hay duplicación controlada del nombre. Decisión: si una categoría se renombra, **no** se actualizan retroactivamente los movimientos ya registrados.

**Saldo de las cuentas:** no se guarda. Es un dato derivado (se calcula sumando movimientos) y guardarlo agregaría un problema de consistencia.

## 3. Baja lógica

Ningún documento se elimina físicamente.

- Todas las colecciones tienen el campo `activo` (`true` por defecto).
- Al dar de baja: `activo` pasa a `false` y se agrega `deletedAt` con la fecha. El campo `deletedAt` solo existe en documentos dados de baja.
- Las lecturas filtran siempre con `{ activo: true }`.
- Modificar un documento inactivo no está permitido (la API responde 404).
- Una baja es reversible: se vuelve a poner `activo: true` y se quita `deletedAt` con `$unset`.

Baja y lectura en el shell:

```js
db.movimientos.updateOne(
  { _id: ObjectId("...") },
  { $set: { activo: false, deletedAt: new Date() } }
)

db.movimientos.find({ activo: true })
```

## 4. Colecciones y documentos de ejemplo

Los ejemplos usan los tipos reales de MongoDB.

### 4.1 usuarios

```js
{
  _id: ObjectId("..."),
  nombre: "Luis",
  email: "luis@ejemplo.com",
  activo: true,
  createdAt: ISODate("2026-10-22T00:00:00.000Z")
}
```

No hay autenticación en este proyecto, por eso no se guarda contraseña.

### 4.2 cuentas

```js
{
  _id: ObjectId("..."),
  nombre: "Efectivo",
  tipo: "EFECTIVO",          // EFECTIVO | BANCO | BILLETERA
  activo: true,
  createdAt: ISODate("2026-10-22T00:00:00.000Z")
}
```

### 4.3 categorias

```js
{
  _id: ObjectId("..."),
  nombre: "Alimentación",
  tipo: "EGRESO",            // INGRESO | EGRESO
  activo: true,
  createdAt: ISODate("2026-10-22T00:00:00.000Z")
}
```

### 4.4 movimientos

```js
{
  _id: ObjectId("..."),
  descripcion: "Compra en supermercado",
  monto: Decimal128("48500.00"),
  tipo: "EGRESO",            // INGRESO | EGRESO
  fecha: ISODate("2026-10-18T00:00:00.000Z"),
  cuentaId: ObjectId("..."),       // link a cuentas
  creadoPorId: ObjectId("..."),    // link a usuarios
  categoria: {                     // embedding: id + nombre histórico
    id: ObjectId("..."),
    nombre: "Alimentación"
  },
  activo: true
  // deletedAt: ISODate("...")     solo si se da de baja
}
```

## 5. Tipos: API vs base de datos

En la API HTTP los valores viajan como texto (`"monto": "48500.00"`, ids y fecha como string). En MongoDB se guardan con su tipo propio:

- `monto` → `Decimal128`, para evitar errores de redondeo con dinero (nunca `double`).
- `fecha` → `Date` (se muestra como `ISODate`).
- ids → `ObjectId`.

## 6. Reglas de negocio y consistencia

Reglas de negocio de un movimiento:

- La descripción no puede estar vacía.
- El monto debe ser mayor a cero.
- El tipo es `INGRESO` o `EGRESO`.
- La fecha debe ser válida.

Reglas de consistencia:

- Antes de crear o modificar un movimiento se valida que `cuentaId`, `creadoPorId` y `categoria.id` existan y estén activos.
- No se eliminan físicamente usuarios, cuentas ni categorías; se dan de baja lógicamente.
- El nombre histórico de la categoría dentro del movimiento no se actualiza si el catálogo cambia.
