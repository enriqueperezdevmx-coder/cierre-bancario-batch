# Cierre bancario con Spring Batch
**Autor:** Enrique Pérez Sánchez

## Cómo correrlo
docker compose up -d --wait
./correr.sh 2026-09-30 prueba
./ver-batch.sh

## Dia 2 El primer chunk

### Boleto de salida
1. **¿Qué diferencia hay entre un step de tipo Tasklet y uno de tipo chunk?**
   - Un Tasklet ejecuta una sola tarea atómica (por ejemplo, validar si un archivo existe o borrar una tabla) y termina[cite: 8, 20, 50, 70]. Un step de tipo chunk procesa grandes volúmenes de datos iterativamente en bloques mediante tres fases: lectura (`ItemReader`), procesamiento (`ItemProcessor`) y escritura transaccional (`ItemWriter`).

2. **¿Qué hace cada una de las tres piezas de un chunk? ¿Cuál es opcional?**
   - **ItemReader:** Lee los datos elemento por elemento desde una fuente externa (como un archivo CSV o base de datos).
   - **ItemProcessor:** Aplica lógica de negocio, validaciones o transformaciones a cada elemento; es la única pieza **opcional**.
   - **ItemWriter:** Recibe el conjunto completo de elementos acumulados en el chunk y los escribe en lote dentro de una única transacción (por ejemplo, con sentencias SQL por lotes).

3. **Con 45 movimientos y chunks de 10, ¿cuántos commits habría? ¿Y con chunks de 50?**
   - Con chunk de 10: **5 commits** ($10 + 10 + 10 + 10 + 5$).
   - Con chunk de 50: **1 commit** (los 45 entran en una sola transacción).

4. **¿Por qué el Escritor recibe el chunk completo y no un movimiento a la vez?**
   - Para optimizar el rendimiento y reducir el costo de I/O y de red; ejecutar inserciones por lotes dentro de una transacción confirmada por bloque es mucho más eficiente que abrir y cerrar transacciones individuales por cada fila

5. **Mi predicción de la MP-3, paso 1: ¿qué habría pasado sin el Procesador?**
   - Los registros habrían entrado con inconsistencias tipográficas como `'deposito'`, `'Retiro'` y cadenas con espacios al inicio como `' RETIRO'`, lo que provocaría que agrupaciones o cálculos posteriores fallen o dividan los importes en categorías incorrectas