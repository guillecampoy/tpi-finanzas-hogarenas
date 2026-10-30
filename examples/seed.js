// Dataset mínimo para las referencias del ejemplo Swagger.
// Ejecutar contra finanzas_hogar con: mongosh "$MONGODB_URI" --file examples/seed.js
const demoDb = db.getSiblingDB("finanzas_hogar");
const createdAt = ISODate("2026-10-27T00:00:00.000Z");

const usuarioId = ObjectId("507f191e810c19729de860ea");
const cuentaId = ObjectId("507f1f77bcf86cd799439011");
const categoriaId = ObjectId("507f1f77bcf86cd799439012");

demoDb.usuarios.updateOne(
  { _id: usuarioId },
  {
    $set: {
      nombre: "Usuario Demo",
      email: "usuario.demo@ejemplo.com",
      activo: true,
      updatedAt: createdAt
    },
    $setOnInsert: { createdAt }
  },
  { upsert: true }
);

demoDb.cuentas.updateOne(
  { _id: cuentaId },
  {
    $set: {
      usuarioId,
      nombre: "Efectivo Demo",
      tipo: "EFECTIVO",
      moneda: "ARS",
      activo: true,
      updatedAt: createdAt
    },
    $setOnInsert: { createdAt }
  },
  { upsert: true }
);

demoDb.categorias.updateOne(
  { _id: categoriaId },
  {
    $set: {
      nombre: "Reparaciones",
      tipo: "EGRESO",
      descripcion: "Arreglos y mantenimiento del hogar",
      activo: true,
      updatedAt: createdAt
    },
    $setOnInsert: { createdAt }
  },
  { upsert: true }
);

print("Referencias demo listas:");
print(`cuentaId=${cuentaId}`);
print(`categoriaId=${categoriaId}`);
print(`creadoPorId=${usuarioId}`);
