package com.maonaroda.api.service.rota;

import java.time.Duration;
import java.util.function.LongSupplier;
import com.maonaroda.api.exception.rota.FalhaRota;
import org.springframework.http.HttpStatus;

/** Relógio monotônico: ajustes de data do servidor não estendem uma consulta. */
public final class OrcamentoConsulta {
    private final LongSupplier relogio;
    private final long inicio, limite;
    public OrcamentoConsulta(Duration limite) { this(limite, System::nanoTime); }
    public OrcamentoConsulta(Duration limite, LongSupplier relogio) {
        this.limite = limite.toNanos();
        if (this.limite <= 0) throw new IllegalArgumentException("Orçamento deve ser positivo.");
        this.relogio = relogio; this.inicio = relogio.getAsLong();
    }
    public Duration restante() {
        long restante = limite - (relogio.getAsLong() - inicio);
        if (restante <= 0) throw new FalhaRota(HttpStatus.GATEWAY_TIMEOUT, "O planejamento excedeu o tempo de espera.");
        return Duration.ofNanos(restante);
    }
}
