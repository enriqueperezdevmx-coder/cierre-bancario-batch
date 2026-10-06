package com.academia.banco.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CierreJobConfig {

    // Primer Tasklet: saludo
    @Bean
    public Step saludoStep(JobRepository jobRepository) {
        return new StepBuilder("saludoStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                System.out.println(">>> Hola desde el cierre del día");
                return RepeatStatus.FINISHED;
            })
            .build();
    }

    // Segundo Tasklet: verifica que exista el archivo de movimientos de la fecha del Job
    @Bean
    public Step verificarArchivoStep(JobRepository jobRepository) {
        return new StepBuilder("verificarArchivoStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                Object fecha = chunkContext.getStepContext().getJobParameters().get("fecha");
                Path archivo = Path.of("datos/movimientos-" + fecha + ".csv");
                if (!Files.exists(archivo)) {
                    throw new IllegalStateException("No existe el archivo del dia: " + archivo);
                }
                long movimientos = Files.readAllLines(archivo).size() - 1; // menos el encabezado
                System.out.println(">>> Archivo del dia: " + archivo + " (" + movimientos + " movimientos)");
                return RepeatStatus.FINISHED;
            })
            .build();
    }

    // El Job con dos steps en secuencia: primero saludo, luego verificar archivo
    @Bean
    public Job cierreDelDiaJob(JobRepository jobRepository, Step saludoStep, Step verificarArchivoStep) {
        return new JobBuilder("cierreDelDiaJob", jobRepository)
            .start(saludoStep)
            .next(verificarArchivoStep)
            .build();
    }
}