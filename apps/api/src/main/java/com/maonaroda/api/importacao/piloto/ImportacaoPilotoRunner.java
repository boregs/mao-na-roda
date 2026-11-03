package com.maonaroda.api.importacao.piloto;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import java.nio.file.*;

@Component
@ConditionalOnProperty(name = "piloto.arquivo")
public class ImportacaoPilotoRunner implements ApplicationRunner {
    private final PublicadorPiloto publicador;
    private final ConfigurableApplicationContext context;
    private final String arquivo;


    public ImportacaoPilotoRunner(PublicadorPiloto publicador, ConfigurableApplicationContext context,
            @Value("${piloto.arquivo}") String arquivo) {
        this.publicador = publicador; this.context = context; this.arquivo = arquivo;
    }


    @Override public void run(ApplicationArguments args) throws Exception {
        if (!"none".equalsIgnoreCase(context.getEnvironment().getProperty("spring.main.web-application-type", "")))
            throw new IllegalArgumentException("Importação administrativa exige --spring.main.web-application-type=none.");

        var path = Path.of(arquivo);

        if (Files.size(path) > 4_000_000) throw new IllegalArgumentException("Arquivo piloto excede 4 MB.");

        var dataset = JsonMapper.builder().build().readValue(Files.readString(path), PublicadorPiloto.Dataset.class);

        publicador.publicar(dataset);
        // Não imprimir coordenadas, evidências privadas ou credenciais.
        System.out.println("Dataset piloto publicado atomicamente.");
        context.close();
    }
}
