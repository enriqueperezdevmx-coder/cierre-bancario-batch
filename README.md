# Cierre bancario con Spring Batch

**Autor:** Enrique Perez Sanchez 

## Cómo correrlo

    docker compose up -d --wait
    ./correr.sh 2026-09-30 prueba
    ./ver-batch.sh

## Día 1 · Mi primer Job

### Boleto de salida

1. ¿Qué diferencia hay entre un proceso batch y la API REST de la Semana 3? Da dos.
un proceso batch solo neesita que arranquemos , por ejemplo, el cierre bancario que trabajara toda la noche y cuando termine el solo se apaga.
Una API REST necesita ser ejecutado para trabajar mientras sigue encendido para hacerle las peticiones que se rquieren (get, set, delete, buscar). Como por ejemplo un set de empledos de x empresa para hacer una operación con ellos. 
2. ¿Qué es un Job, qué es un Step y qué es un Tasklet?
un job es el proceso que se necesita para obtner información, un Step es una parte de job, el job se divide en steps, el Tasklet es la operación que realiza un step.
3. Con tus tablas: ¿qué diferencia hay entre una **JobInstance** y una **JobExecution**?
La primera guarda las instancias de los jobs, la segunda las ejecuciones de las instancias.
4. ¿Por qué Spring Batch no deja correr dos veces el cierre del 28?
Porque ya existe y si queremos ejecutarlo otra vez: se borra o se cambian los parámetros, en terminos bancarios, es cobraar doble vez intereses al cliente.
5. (MP-4, paso 6) Si mañana llega el archivo del 25 y corres otra vez el cierre del 25, ¿será otra instancia u
   otra ejecución de la misma? ¿Por qué lo crees?
   Yo creo que seria otra instancia del mismo job pero con parametros diferentes porque si no pasaría lo de la pregunta anterior

   ## Día 2 · El primer chunk

### Boleto de salida

1. ¿Qué diferencia hay entre un step de tipo Tasklet y uno de tipo chunk?
Un tasklet se define por realizar una tarea, es decir, un job es tipo tasklet porque ejecuta una tarea; un chunk es igual una tarea pero se diferecía en que con el trabajamos con datos extraidos de un .csv.
2. ¿Qué hace cada una de las tres piezas de un chunk? ¿Cuál es opcional?
Lector lee los datos del .csv.
Escritor: guarda en mysql los movimientos que se realizaron.
Procesador: limpia los mocimientos
3. Con 45 movimientos y chunks de 10, ¿cuántos commits habría? ¿Y con chunks de 50? 5 en ambos. 45 = (10*4) + (5*1)
50 = (5*10)
4. ¿Por qué el Escritor recibe el chunk completo y no un movimiento a la vez?
Para optimizar el rendimiento del DD.
5. Mi predicción de la MP-3, paso 1: ¿qué habría pasado sin el Procesador?
My SQL no llegaria limpio y traeria datos innecesarios del dia anterior.

## Día 3 · Parámetros, fallas y reinicio

### Boleto de salida

1. ¿Qué diferencia hay entre una JobInstance y una JobExecution? Usa como ejemplo el cierre del 25.
Recordando que la intancia cuando es una tarea que ejecuta el job, mientras que una ejecucion se encarga de ejecutar la instancia una o varias veces. 
2. ¿En qué caso Spring Batch se niega a correr un cierre, y en qué caso lo reinicia?
se reinicia cuando falla un paso pero se niega a el cierre cuando es solo una linea la que produjo el error , mejor solo la escribe para verificar despues 
3. En el reinicio del día 5, ¿por qué el step de carga leyó 10 movimientos y no 20?
porque eran 10 los chunk's, ya no repite los que ya leyo
4. ¿Qué diferencia hay entre un movimiento **filtrado** y uno **omitido**?
filtrar se ecarga de seleccionar los movimentos que pertene cen a un cierto tipo,  y omotido se encrga de ignorar el renglón ilegible para que nosea el responsable de parar toda el cierre
5. ¿Por qué importa el código de salida, si el estado ya queda en las tablas?
Se reduce a una distinción clave: el estado operativo vs. el flujo de control de negocio.

## Día 4 · De MySQL a MongoDB

### Boleto de salida

1. ¿Qué hace cada uno de los tres steps de tu Job, y de qué tipo es cada uno?
-> Mongo cuadra con MySQL
-> Step de tipo chunk que lee en MySQL y escribe en MongoDB
-> Step de tipo chunk que vuelve a escribir todos los datos
2. ¿Por qué el cierre del 9 no duplicó los saldos, y el del 10 (sin `@Id`) sí?
Porque al saber el id puede reconocer en que punto se quedo u que movimientos faltaron por ejcutar, sin saber el id revisa todos desde el inicio porque son una instanci nueva
3. Al reiniciar el cierre del 11, ¿por qué no se cargó otra vez el archivo?
La misma razon de la pregunta anterior, ya sqabe que analizó los 15 movimientos, por lo tanto se queda con la misma información anterior
4. ¿Qué diferencia hay entre `spring-boot-starter-data-mongodb` y «Spring Batch MongoDB» (`batch-data-mongodb`)?
1.- Operaciones CRUD, consultas interactivas y aplicaciones OLTP/REST en tiempo real.
2.- Procesamiento por lotes (Batch/ETL), migraciones masivas y trabajos pesados fuera de línea.

## Lo que aprendí esta semana

(Con tus palabras, en 5 a 10 renglones: qué es un proceso batch, qué piezas tiene un Job y qué hace SpringBatch cuando algo falla.)
batch se dedica al procesamiento por lotes con la indicación para iniciar el análisis, realiza todo su proceso hasta que se apaga solo sin intervención humana; las piezas de un job son: instancia, las partes de un job son: lector, escritor y procesador; loo que hace cuando falla es escribir en las tablas porque huo una fallida ejecución indicando el error, borra lo generado al momento de fallar yse renicia exactamente donde hubo fallo, puede saltar ese renglon para no parar la ejecución completa o puede simplemente ignorar el renglon u otras acciones programadas si el fallo sucede.
