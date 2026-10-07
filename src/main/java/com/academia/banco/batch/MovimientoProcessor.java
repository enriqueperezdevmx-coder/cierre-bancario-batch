package com.academia.banco.batch;

import com.academia.banco.model.Movimiento;
import java.math.BigDecimal;
import org.springframework.batch.infrastructure.item.ItemProcessor;

public class MovimientoProcessor implements ItemProcessor<Movimiento, Movimiento> {

    private static final BigDecimal LIMITE_AUDITORIA = new BigDecimal("10000.00");

    @Override
    public Movimiento process(Movimiento movimiento) {
        if (movimiento.tipo() == null) {
            return null;
        }

        String tipo = movimiento.tipo().trim().toUpperCase();
        if (!tipo.equals("DEPOSITO") && !tipo.equals("RETIRO")) {
            return null; // Filtrado por tipo no soportado
        }

        // Reto: filtrar montos mayores a 10,000 para revisión manual
        if (movimiento.monto() != null && movimiento.monto().compareTo(LIMITE_AUDITORIA) > 0) {
            return null; // Filtrado por monto excesivo
        }

        return new Movimiento(movimiento.cuenta().trim(), tipo, movimiento.monto());
    }
}
