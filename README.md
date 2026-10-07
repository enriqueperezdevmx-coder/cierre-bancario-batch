## Dia 3 Parámetros, fallas y reinicio

### Boleto de salida
1. **¿Qué diferencia hay entre una JobInstance y una JobExecution? Usa como ejemplo el cierre del 25.**
   - La `JobInstance` es el trabajo lógico definido por sus parámetros (`cierreDelDiaJob` del `2026-12-25`). La `JobExecution` es cada intento físico de correrlo[cite: 56, 80].
   - En el cierre del 25, existió una sola instancia con dos ejecuciones: el intento 1 falló por falta de archivo y el intento 2 lo completó cuando el archivo estuvo listo.

2. **¿En qué caso Spring Batch se niega a correr un cierre, y en qué caso lo reinicia?**
   - **Se niega (`JobInstanceAlreadyCompleteException`):** Si la instancia ya terminó en `COMPLETED`, para no duplicar operaciones contables ni alterar saldos.
   - **Lo reinicia:** Si la instancia quedó en `FAILED`, permitiendo crear una nueva ejecución sobre la misma instancia para subsanar el error.

3. **En el reinicio del día 5, ¿por qué el step de carga leyó 10 movimientos y no 20?**
   - Porque el primer chunk (registros 1 al 10) ya se había confirmado en MySQL antes del fallo[cite: 61, 63]. Gracias al contexto de ejecución guardado, Spring Batch retomó la lectura directo desde el registro 11, evitando reprocesar y duplicar datos ya guardados.

4. **¿Qué diferencia hay entre un movimiento filtrado y uno omitido?**
   - **Filtrado (`FILTER_COUNT`):** Regla de negocio en el `ItemProcessor` devolviendo `null` para descartar datos que no aplican al cierre (como `TRANSFERENCIA` o `PAGO`).
   - **Omitido (`SKIP_COUNT`):** Tolerancia técnica a fallas de formato (`FlatFileParseException` por caracteres inválidos) para no frenar la operación completa del banco si un renglón viene roto.

5. **¿Por qué importa el código de salida, si el estado ya queda en las tablas?**
   - Porque los cierres corren de forma desatendida mediante planificadores automáticos (como Control-M)[cite: 65, 80]. El planificador no consulta MySQL: evalúa únicamente el código de salida del sistema operativo (`0` para éxito, distinto de cero para error) para alertar al equipo de guardia y condicionar los trabajos siguientes.