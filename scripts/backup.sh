#!/usr/bin/env bash
set -euo pipefail

# Ejecutar desde la raíz del repo: bash scripts/backup.sh
: "${MONGODB_URI:?Exportar MONGODB_URI antes de ejecutar el backup}"
DB="${MONGODB_DATABASE:-finanzas_hogar}"
DATE="$(date +%F)"
DEST="./resguardos_tpi/${DATE}"

command -v mongodump >/dev/null 2>&1 || { echo "Falta mongodump (MongoDB Database Tools)" >&2; exit 1; }
mkdir -p "$DEST"
# La URI se pasa como argumento: puede resultar visible a procesos locales durante la ejecución.
# Evitar imprimirla y no guardar el output en Git.
mongodump --uri="$MONGODB_URI" --db="$DB" --out="$DEST"
echo "Resguardo generado en: $DEST/$DB"
