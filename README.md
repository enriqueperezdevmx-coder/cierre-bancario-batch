
**Autor:** Enrique Pérez Sánchez

## Cómo correrlo

docker compose up -d --wait
./correr.sh 2026-09-30 prueba
./ver-batch.sh
Día 1 · Mi primer Job

Boleto de salida
¿Qué diferencia hay entre un proceso batch y la API REST de la Semana 3? Da dos.

Un proceso batch corre por lotes de forma programada y desatendida: inicia, procesa un volumen grande de datos fuera de línea y al terminar se apaga automáticamente. Una API REST debe mantenerse viva y a la escucha 24/7 en un puerto esperando peticiones HTTP síncronas de usuarios o clientes externos.

El proceso batch está diseñado para maximizar el rendimiento procesando miles o millones de registros masivos en bloque, mientras que la API REST prioriza tiempos de respuesta en milisegundos para operaciones puntuales individuales.

¿Qué es un Job, qué es un Step y qué es un Tasklet?

Job: Es la definición global del proceso batch completo de negocio.

Step: Es una fase o etapa secuencial e independiente en la que se descompone un Job.

Tasklet: Es una tarea atómica dentro de un Step orientada a ejecutar una sola acción de inicio a fin (como crear una carpeta, verificar un archivo o limpiar una tabla).

Con tus tablas: ¿qué diferencia hay entre una JobInstance y una JobExecution?

JobInstance representa la identidad lógica del trabajo combinada con sus parámetros identificadores únicos.

JobExecution representa cada intento físico de ejecución de esa instancia (un intento puede fallar y generar una segunda ejecución sobre la misma instancia).

¿Por qué Spring Batch no deja correr dos veces el cierre del 28?

Porque la JobInstance correspondiente al día 28 ya alcanzó el estado COMPLETED. Por diseño, Spring Batch lanza una excepción (JobInstanceAlreadyCompleteException) para garantizar la idempotencia y evitar duplicar cobros, intereses o movimientos contables.

(MP-4, paso 6) Si mañana llega el archivo del 25 y corres otra vez el cierre del 25, ¿será otra instancia u otra ejecución de la misma? ¿Por qué lo crees?

Si la ejecución anterior del día 25 terminó en FAILED, será una nueva ejecución de la misma instancia, ya que Spring Batch busca reanudarla. Si ya había terminado en COMPLETED, Spring Batch no permitirá correrla a menos que se cambien los parámetros de identificación, creando una nueva instancia.

Día 2 · El primer chunk
Boleto de salida
¿Qué diferencia hay entre un step de tipo Tasklet y uno de tipo chunk?

Un Step de tipo Tasklet ejecuta una tarea puntual directa de principio a fin. Un Step de tipo Chunk implementa el patrón ETL para procesar grandes volúmenes de datos mediante ciclos continuos de lectura elemento por elemento (ItemReader), transformación opcional (ItemProcessor) y escritura en bloques transaccionales (ItemWriter).

¿Qué hace cada una de las tres piezas de un chunk? ¿Cuál es opcional?

ItemReader: Lee los datos registro por registro desde el origen (archivo plano CSV, base de datos, cola de mensajería).

ItemProcessor: Aplica reglas de negocio, validaciones y transformaciones de tipo sobre cada registro individual. Es la única pieza opcional.

ItemWriter: Recibe la lista completa acumulada en el tamaño del chunk y la persiste en lote en el destino (por ejemplo, inserciones masivas en MySQL).

Con 45 movimientos y chunks de 10, ¿cuántos commits habría? ¿Y con chunks de 50?

Con chunks de 10: Habría 5 commits (4 chunks de 10 registros y 1 chunk final con los últimos 5 registros restantes).

Con chunks de 50: Habría 1 commit (un único bloque que abarca los 45 registros completos).

¿Por qué el Escritor recibe el chunk completo y no un movimiento a la vez?

Para reducir drásticamente el costo de operaciones de entrada/salida (I/O) y conexiones de red contra la base de datos, permitiendo ejecutar sentencias batch dentro de una sola transacción atómica.

Mi predicción de la MP-3, paso 1: ¿qué habría pasado sin el Procesador?

Los datos brutos habrían entrado directamente al destino sin filtrado de registros no deseados, sin normalización de tipos ni aplicación de reglas contables de negocio.

Día 3 · Parámetros, fallas y reinicio
Boleto de salida
¿Qué diferencia hay entre una JobInstance y una JobExecution? Usa como ejemplo el cierre del 25.

La JobInstance es el concepto lógico del cierre con parámetro fecha=2026-09-25. Si durante la ejecución falla un paso por falta de un archivo, la primera JobExecution queda en estado FAILED. Cuando se arregla el archivo y se relanza el cierre con la misma fecha, Spring Batch asocia una segunda JobExecution a la misma JobInstance existente.

¿En qué caso Spring Batch se niega a correr un cierre, y en qué caso lo reinicia?

Se niega (JobInstanceAlreadyCompleteException): Si la instancia ya terminó exitosamente en estado COMPLETED.

Lo reinicia: Si la ejecución previa terminó en estado FAILED o STOPPED, aprovechando los metadatos persistidos para reanudar el trabajo pendiente.

En el reinicio del día 5, ¿por qué el step de carga leyó 10 movimientos y no 20?

Porque el primer chunk (registros 1 al 10) ya había hecho commit en MySQL antes de ocurrir la falla. Gracias al ExecutionContext persistido en la base de datos, Spring Batch retomó la lectura exactamente desde el registro 11 en adelante, evitando reprocesar y duplicar datos.

¿Qué diferencia hay entre un movimiento filtrado y uno omitido?

Filtrado (FILTER_COUNT): Ocurre intencionalmente por lógica de negocio en el ItemProcessor devolviendo null (por ejemplo, omitir transacciones que no aplican a la liquidación).

Omitido (SKIP_COUNT): Ocurre por tolerancia a fallos técnicos cuando un registro genera una excepción (como un error de formato en el CSV) y la política de Skip lo descarta para no abortar el Job completo.

¿Por qué importa el código de salida, si el estado ya queda en las tablas?

Porque los procesos batch son orquestados por planificadores externos del sistema operativo (cron, Control-M, Jenkins). El planificador no realiza consultas a las tablas internas de MySQL, sino que evalúa el código de salida del proceso (ExitStatus / código 0 para éxito, distinto de 0 para error) para alertar a los administradores y detonar tareas dependientes.

Día 4 · De MySQL a MongoDB
Boleto de salida
¿Qué hace cada uno de los tres steps de tu Job, y de qué tipo es cada uno?

Step 1: Prepara y valida el entorno previo y las tablas de trabajo (Tasklet).

Step 2: Lee los movimientos registrados en MySQL y los migra estructurados hacia MongoDB (Chunk).

Step 3: Realiza el cuadre general de saldos finales y consolidación del cierre (Chunk o Tasklet).

¿Por qué el cierre del 9 no duplicó los saldos, y el del 10 (sin @Id) sí?

Porque al tener el campo @Id mapeado explícitamente, MongoDB realiza una operación de upsert (reemplazo por clave primaria si ya existe). Sin @Id, MongoDB genera identificadores aleatorios únicos para cada inserción, duplicando los documentos en cada corrida.

Al reiniciar el cierre del 11, ¿por qué no se cargó otra vez el archivo?

Porque el step de carga inicial ya había concluido en estado COMPLETED en la ejecución previa; Spring Batch consulta las tablas de metadatos y avanza de inmediato al step que había quedado inconcluso.

¿Qué diferencia hay entre spring-boot-starter-data-mongodb y «Spring Batch MongoDB» (batch-data-mongodb)?

spring-boot-starter-data-mongodb: Está orientado a transacciones en línea, repositorios CRUD interactivos y consultas de baja latencia para aplicaciones web.

batch-data-mongodb: Provee componentes especializados de lectura y escritura (MongoItemReader paginado y MongoItemWriter) preparados para el streaming y procesamiento masivo por lotes.

Lo que aprendí esta semana
En esta semana aprendí que un proceso batch está diseñado para procesar grandes volúmenes de datos por lotes de forma autónoma, sin interacción directa con el usuario final y con un ciclo de vida definido que concluye al terminar la tarea. Comprendí la arquitectura modular de Spring Batch dividida en Jobs, Steps (Tasklet para tareas atómicas y Chunks para procesamiento ETL con Reader, Processor y Writer). Asimismo, entendí el rol de las tablas de metadatos para rastrear el ciclo de vida mediante JobInstance y JobExecution, permitiendo reiniciar procesos exactamente donde fallaron gracias al ExecutionContext, la gestión de tolerancia a fallos mediante mecanismos de Skip y Retry, y la integración de persistencia entre bases de datos relacionales (MySQL) y almacenes NoSQL (MongoDB).