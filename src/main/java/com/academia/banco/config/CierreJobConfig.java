package com.academia.banco.config;

import com.academia.banco.batch.MovimientoProcessor;
import com.academia.banco.model.Movimiento;
import com.academia.banco.model.SaldoCuenta;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.data.MongoItemWriter;
import org.springframework.batch.infrastructure.item.data.builder.MongoItemWriterBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.JdbcCursorItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class CierreJobConfig {

    // Tasklet: revisa que exista el archivo de movimientos de la fecha que recibió el Job.
    @Bean
    public Step verificarArchivoStep(JobRepository jobRepository) {
        return new StepBuilder("verificarArchivoStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                Object fecha = chunkContext.getStepContext().getJobParameters().get("fecha");
                Path archivo = Path.of("datos/movimientos-" + fecha + ".csv");
                if (!Files.exists(archivo)) {
                    throw new IllegalStateException("No existe el archivo del día: " + archivo);
                }
                long movimientos = Files.readAllLines(archivo).size() - 1;
                System.out.println(">>> Archivo del día: " + archivo + " (" + movimientos + " movimientos)");
                return RepeatStatus.FINISHED;
            })
            .build();
    }

    // El Lector: lee el archivo de la fecha del Job, un renglón a la vez, y lo convierte en un Movimiento.
    @Bean
    @StepScope
    public FlatFileItemReader<Movimiento> movimientoReader(@Value("#{jobParameters['fecha']}") String fecha) {
        return new FlatFileItemReaderBuilder<Movimiento>()
            .name("movimientoReader")
            .resource(new FileSystemResource("datos/movimientos-" + fecha + ".csv"))
            .linesToSkip(1)
            .delimited()
            .names("cuenta", "tipo", "monto")
            .targetType(Movimiento.class)
            .build();
    }

    // El Escritor: guarda en MySQL los movimientos que le llegan, todos juntos.
    @Bean
    public JdbcBatchItemWriter<Movimiento> movimientoWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Movimiento>()
            .dataSource(dataSource)
            .sql("INSERT INTO movimiento (cuenta, tipo, monto) VALUES (:cuenta, :tipo, :monto)")
            .beanMapped()
            .build();
    }

    // Step de tipo Chunk: lee, procesa y escribe de 10 en 10, omitiendo renglones corruptos.
    @Bean
    public Step cargarMovimientosStep(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager,
                                      FlatFileItemReader<Movimiento> movimientoReader,
                                      JdbcBatchItemWriter<Movimiento> movimientoWriter) {
        return new StepBuilder("cargarMovimientosStep", jobRepository)
            .<Movimiento, Movimiento>chunk(10)
            .transactionManager(transactionManager)
            .reader(movimientoReader)
            .processor(new MovimientoProcessor())
            .writer(movimientoWriter)
            .faultTolerant()
            .skip(FlatFileParseException.class)
            .skipLimit(3)
            .build();
    }

    // Step 3: de MySQL a MongoDB
    // Lector: consulta a MySQL que agrupa y calcula el saldo neto de cada cuenta.
    @Bean
    public JdbcCursorItemReader<SaldoCuenta> saldoReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<SaldoCuenta>()
            .name("saldoReader")
            .dataSource(dataSource)
            .sql("""
                SELECT cuenta,
                       SUM(CASE WHEN tipo = 'DEPOSITO' THEN monto ELSE -monto END) AS saldo,
                       COUNT(*) AS movimientos
                FROM movimiento
                GROUP BY cuenta
                ORDER BY cuenta
                """)
            .dataRowMapper(SaldoCuenta.class)
            .build();
    }

    // Escritor: guarda cada SaldoCuenta en MongoDB (colección saldos).
    @Bean
    public MongoItemWriter<SaldoCuenta> saldoWriter(MongoTemplate mongoTemplate) {
        return new MongoItemWriterBuilder<SaldoCuenta>()
            .template(mongoTemplate)
            .collection("saldos")
            .build();
    }

    // Chunk sin procesador: lee de MySQL y escribe en MongoDB de 3 en 3.
    @Bean
    public Step publicarSaldosStep(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager,
                                   JdbcCursorItemReader<SaldoCuenta> saldoReader,
                                   MongoItemWriter<SaldoCuenta> saldoWriter) {
        return new StepBuilder("publicarSaldosStep", jobRepository)
            .<SaldoCuenta, SaldoCuenta>chunk(3)
            .transactionManager(transactionManager)
            .reader(saldoReader)
            .writer(saldoWriter)
            .build();
    }

    // El Job: encadena los tres steps en orden secuencial.
    @Bean
    public Job cierreDelDiaJob(JobRepository jobRepository,
                               Step verificarArchivoStep,
                               Step cargarMovimientosStep,
                               Step publicarSaldosStep) {
        return new JobBuilder("cierreDelDiaJob", jobRepository)
            .start(verificarArchivoStep)
            .next(cargarMovimientosStep)
            .next(publicarSaldosStep)
            .build();
    }
}
