#!/usr/bin/env bash
set -u
uso() { echo "Uso: ./correr.sh <fecha AAAA-MM-DD> <nombre-de-la-evidencia>"; exit 2; }
[ $# -eq 2 ] || uso
[[ $1 =~ ^[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$ ]] || uso
[[ $2 =~ ^[A-Za-z0-9_-]+$ ]] || { echo "El nombre de la evidencia solo puede llevar letras, números, - y _"; uso; }

mkdir -p evidencia
SALIDA="evidencia/$2.txt"
if [ -e "$SALIDA" ]; then
  echo "Ya existe $SALIDA y no la voy a sobrescribir."
  echo "Si el intento anterior falló y quieres repetirlo con el mismo nombre, bórrala primero: rm $SALIDA"
  exit 2
fi

echo "$ ./mvnw -q spring-boot:run -Dspring-boot.run.arguments=\"fecha=$1\"" > "$SALIDA"
./mvnw -q spring-boot:run -Dspring-boot.run.arguments="fecha=$1" >> "$SALIDA" 2>&1
codigo=$?
echo "Código de salida: $codigo" >> "$SALIDA"

patron='>>>|Application run failed|Executing step:|Step already complete|Job: \[|Encountered an error executing step|Caused by:|Exception:|Error:|\[ERROR\]'
grep -E "$patron" "$SALIDA" | sed -e 's/^[0-9]\{4\}-[0-9]\{2\}-[0-9]\{2\}T[^ ]* *[A-Z]* *[0-9]* *--- *\[[^]]*\] *[^:]*: *//'
exit $codigo
